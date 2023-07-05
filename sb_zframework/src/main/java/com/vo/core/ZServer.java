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
// FIXME 2023年7月4日 下午4:45:53 zhanghen: TODO NIO server 支持ssl
public class ZServer extends Thread {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final String Z_SERVER_QPS = "ZServer_QPS";

	public static final String DEFAULT_ZFRAMEWORK_NIO_HTTP_THREAD_NAME_PREFIX = "zframework-nio-http-thread-";

	public static final int DEFAULT_HTTP_PORT = 80;

	private static final ServerConfiguration SERVER_CONFIGURATION = ZSingleton
			.getSingletonByClass(ServerConfiguration.class);

	public final static ZE ZE = ZES.newZE(SERVER_CONFIGURATION.getThreadCount(),
			DEFAULT_ZFRAMEWORK_NIO_HTTP_THREAD_NAME_PREFIX);

	@Override
	public void run() {
		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		if (serverConfiguration.getSslEnable()) {
			LOG.info("SSL启用，启动SSLServer,port={}", serverConfiguration.getPort());
			ZServer.startSSLServer();
		} else {
			LOG.info("启动Server,port={}", serverConfiguration.getPort());
			final NioLongConnectionServer nioLongConnectionServer = new NioLongConnectionServer();
			nioLongConnectionServer.startNIOServer();
		}
	}

	private static void startSSLServer() {

		try {
			// 加载密钥库文件
			// 密钥库密码
			final KeyStore keyStore = KeyStore.getInstance(SERVER_CONFIGURATION.getSslType());
			final FileInputStream fis = new FileInputStream(SERVER_CONFIGURATION.getSslKeyStore());
			final char[] password = SERVER_CONFIGURATION.getSslPassword().toCharArray();
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

			ZServer.LOG.trace("zSSLServer开始启动,serverPort={}", SERVER_CONFIGURATION.getPort());

			// 创建ServerSocket并绑定SSL上下文
			final ServerSocket serverSocket = sslContext.getServerSocketFactory()
					.createServerSocket(SERVER_CONFIGURATION.getPort());

			ZServer.LOG.trace("zSSLServer启动成功，等待连接,serverPort={}", SERVER_CONFIGURATION.getPort());

			// 启动服务器
			while (true) {
				final SSLSocket socket = (SSLSocket) serverSocket.accept();

				final boolean allow = Counter.allow(ZServer.Z_SERVER_QPS, SERVER_CONFIGURATION.getConcurrentQuantity());
				if (!allow) {

					final ZResponse response = new ZResponse(socket.getOutputStream());

					response
							.httpStatus(HttpStatus.HTTP_403.getCode())
							.contentType(HeaderEnum.JSON.getType())
							.body(JSON.toJSONString(CR.error("zserver-超出QPS限制,qps = " + SERVER_CONFIGURATION.getConcurrentQuantity())))
							.write();

					socket.close();

				} else {
					ZServer.ZE.executeInQueue(() -> {
						// FIXME 2023年7月4日 上午10:26:22 zhanghen: 用TaskNIO
						final Task task = new Task(socket);
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
