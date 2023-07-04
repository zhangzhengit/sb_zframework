/*
 *
 */
package com.vo.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import com.alibaba.fastjson.JSON;
import com.vo.conf.ServerConfiguration;
import com.vo.conf.ZFrameworkDatasourcePropertiesLoader;
import com.vo.conf.ZFrameworkProperties;
import com.vo.http.HttpStatus;
import com.votool.common.CR;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
// FIXME 2023年7月3日 下午7:46:30 zhanghen: TODO： 支持ssl nioserver，现在ssl开启后会忽略nioEnable
// FIXME 2023年7月3日 下午8:08:48 zhanghen: TODO : nio实现长连接，bio弃用 OK
public class ZServer extends Thread {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final String Z_SERVER_QPS = "ZServer_QPS";

	public static final String DEFAULT_ZFRAMEWORK_NIO_HTTP_THREAD_NAME_PREFIX = "zframework-nio-http-thread-";
	public static final String DEFAULT_ZFRAMEWORK_HTTP_THREAD_NAME_PREFIX = "zframework-http-thread-";

	public static final int DEFAULT_HTTP_PORT = 80;

	private static final ZFrameworkProperties FRAMEWORK_PROPERTIES = ZFrameworkDatasourcePropertiesLoader
			.getFrameworkPropertiesInstance();
	private final static ZE ZE = ZES.newZE(ZServer.FRAMEWORK_PROPERTIES.getThreadCount(),
//			ZSingleton.getSingletonByClass(ServerConfiguration.class).getNioEnable()
//					?
							DEFAULT_ZFRAMEWORK_NIO_HTTP_THREAD_NAME_PREFIX
//					: DEFAULT_ZFRAMEWORK_HTTP_THREAD_NAME_PREFIX
					);

	@Override
	public void run() {
		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		if (serverConfiguration.getSslEnable()) {
			LOG.info("SSL启用，启动SSLServer,port={}", serverConfiguration.getPort());
			this.startSSLServer();
		} else {
			LOG.info("启动Server,port={}", serverConfiguration.getPort());
			this.startNIOServer();

//			if (serverConfiguration.getNioEnable()) {
//				this.startNIOServer();
//			} else {
//				this.startServer();
//			}
		}
	}

	private void startSSLServer() {

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);

		// 加载密钥库文件
		// 密钥库密码
		final char[] password = serverConfiguration.getSslPassword().toCharArray();
		KeyStore keyStore;
		try {

			keyStore = KeyStore.getInstance(serverConfiguration.getSslType());
			final FileInputStream fis = new FileInputStream(serverConfiguration.getSslKeyStore());
			keyStore.load(fis, password);

			// 初始化密钥管理器
			final KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, password);
			keyManagerFactory.init(keyStore, password);

			// 初始化信任管理器
			final TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);

			// 初始化SSL上下文
			final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			ZServer.LOG.trace("zSSLServer开始启动,serverPort={}",serverConfiguration.getPort());

			// 创建ServerSocket并绑定SSL上下文
			final ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket(serverConfiguration.getPort());

			ZServer.LOG.trace("zSSLServer启动成功，等待连接,serverPort={}",serverConfiguration.getPort());

			// 启动服务器
			while (true) {
				final SSLSocket socket = (SSLSocket) serverSocket.accept();

				final boolean allow = Counter.allow(ZServer.Z_SERVER_QPS, serverConfiguration.getConcurrentQuantity());
				if (!allow) {

					final ZResponse response = new ZResponse(socket.getOutputStream());

					response.contentType(HeaderEnum.JSON.getType())
							.httpStatus(HttpStatus.HTTP_403.getCode())
							.body(JSON.toJSONString(CR.error("zserver-超出QPS限制,qps = " + serverConfiguration.getConcurrentQuantity())))
							.writeAndFlushAndClose();

					socket.close();

				} else {
					ZServer.ZE.executeInQueue(() -> {

						// FIXME 2023年7月4日 上午10:26:22 zhanghen: 用TaskNIO
						final TaskNIO task = new TaskNIO(socket);
						final ZRequest request = task.readAndParse();
						task.invoke(request);
					});
				}

			}
		} catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException
				| UnrecoverableKeyException | KeyManagementException e) {
			e.printStackTrace();
		}
	}

	private static final int BUFFER_SIZE = 1024 * 50;

	private Selector selector;

	public void startNIOServer() {
		ZServer.LOG.trace("zNIOServer开始启动,serverPort={}",ZServer.FRAMEWORK_PROPERTIES.getServerPort());

		try {
			this.selector = Selector.open();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		ServerSocketChannel serverSocketChannel = null;
		try {
			serverSocketChannel = ServerSocketChannel.open();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocketChannel
					.bind(new InetSocketAddress(ZSingleton.getSingletonByClass(ServerConfiguration.class).getPort()));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocketChannel.configureBlocking(false);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
		} catch (final ClosedChannelException e) {
			e.printStackTrace();
		}

		ZServer.LOG.trace("zNIOServer启动成功，等待连接,serverPort={}",ZServer.FRAMEWORK_PROPERTIES.getServerPort());

		while (true) {
			try {
				this.selector.select();
			} catch (final IOException e) {
				e.printStackTrace();
			}

			final Iterator<SelectionKey> keyIterator = this.selector.selectedKeys().iterator();
			while (keyIterator.hasNext()) {
				final SelectionKey key = keyIterator.next();
				keyIterator.remove();
				final boolean valid = key.isValid();

				if (!valid) {
					System.out.println("key is valid");
					continue;
				}

				if (key.isAcceptable()) {
					try {
						this.handleAccept(key);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				} else if (key.isReadable()) {
					this.handleRead(key);
				}
			}
		}
	}

	private void handleAccept(final SelectionKey key) throws IOException {
		final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		final SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(this.selector, SelectionKey.OP_READ);
	}

	private  void handleRead(final SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		final boolean open = socketChannel.isOpen();
		if (!open) {
			return;
		}
		int bytesRead = 0;
		try {
			bytesRead = socketChannel.read(buffer);
		} catch (final IOException e) {
			return;
		}

		if (bytesRead == -1) {
			try {
				key.cancel();
				socketChannel.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return;
		}

		if (bytesRead > 0) {
			buffer.flip();
			final byte[] requestData = new byte[buffer.remaining()];
			buffer.get(requestData);

			final String request = new String(requestData, StandardCharsets.UTF_8);

			key.cancel();
			if (socketChannel.isOpen()) {

				ZE.executeInQueue(() -> {
					final TaskNIO taskNIO = new TaskNIO(socketChannel);
					final ZRequest requestX = TaskNIO.handleRead(request);
					final ZRequest request2 = TaskNIO.parseRequest(requestX);
					taskNIO.invoke(request2);
				});

			}
		}
	}

	private void handleRequest(final SocketChannel socketChannel, final String request, final SelectionKey key) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "NonBlockingHttpServer.handleRequest()");
		try {
			Thread.sleep(1);
		} catch (final InterruptedException e1) {
			e1.printStackTrace();
		}

		// 在这里处理请求并生成响应
		final String response =
					  "HTTP/1.1 200 OK\r\n"
					+ "Content-Type: text/plain\r\n"
					+ "Content-Length: 12\r\n"
					+ "\r\n"
					+ "Hello World!";

		try {
			final ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
			socketChannel.write(buffer);
			socketChannel.close();
		} catch (final IOException e) {
//	            e.printStackTrace();
			return;
		}
	}

//	private void startServer() {
//
//		ServerSocket serverSocket = null;
//		try {
//
//			ZServer.LOG.trace("zserver开始启动,serverPort={}",ZServer.FRAMEWORK_PROPERTIES.getServerPort());
//			serverSocket = new ServerSocket(ZServer.FRAMEWORK_PROPERTIES.getServerPort());
//			ZServer.LOG.trace("zserver启动成功，等待连接,serverPort={}",ZServer.FRAMEWORK_PROPERTIES.getServerPort());
//			while (true) {
//				final Socket socket = serverSocket.accept();
//				final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
//				final boolean allow = Counter.allow(ZServer.Z_SERVER_QPS, serverConfiguration.getConcurrentQuantity());
//				if (!allow) {
//
//					final ZResponse response = new ZResponse(socket.getOutputStream());
//
//					response.contentType(HeaderEnum.JSON.getType())
//							.httpStatus(HttpStatus.HTTP_403.getCode())
//							.body(CR.error("超出QPS限制,qps = " + serverConfiguration.getConcurrentQuantity()))
//							.writeAndFlushAndClose();
//
//
//					socket.close();
//
//				} else {
//					ZServer.ZE.executeInQueue(() -> {
//						final Task task = new Task(socket);
//						final ZRequest request = task.readAndParse();
//						task.invoke(request);
//					});
//				}
//			}
//		} catch (final IOException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				if (serverSocket != null) {
//					serverSocket.close();
//				}
//			} catch (final IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//	}

	/**
	 * 一秒内是否超过允许的次数
	 *
	 * @author zhangzhen
	 * @date 2023年7月1日
	 *
	 */
	public static class Counter {

		private static final Map<String, Long> map = new ConcurrentHashMap<>(4, 1F);

		public static boolean allow(final String keyword, final long qps) {

			if (qps <= 0) {
				return false;
			}

			final long seconds = System.currentTimeMillis() / 1000L;

			final String key = keyword + "@" + seconds;

			synchronized (keyword) {

				final Long v = Counter.map.get(key);
				if (v == null) {
					Counter.map.put(key, 1L);
					return true;
				}

				final long newCount = v + 1L;
				Counter.map.put(key, newCount);

				return newCount <= qps;
			}
		}

	}
}
