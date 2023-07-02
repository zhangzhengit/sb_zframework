/*
 *
 */
package com.vo.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

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
public class ZServer extends Thread {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final String Z_SERVER_QPS = "ZServer_QPS";

	public static final String DEFAULT_ZFRAMEWORK_HTTP_THREAD_NAME_PREFIX = "zframework-http-thread-";

	public static final int DEFAULT_HTTP_PORT = 80;

	private static final ZFrameworkProperties FRAMEWORK_PROPERTIES = ZFrameworkDatasourcePropertiesLoader
			.getFrameworkPropertiesInstance();
	private final static ZE ZE = ZES.newZE(ZServer.FRAMEWORK_PROPERTIES.getThreadCount(), ZServer.FRAMEWORK_PROPERTIES.getThreadNamePrefix());

	@Override
	public void run() {
		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		if (serverConfiguration.getSslEnable()) {
			LOG.info("SSL启用，启动SSLServer,port={}", serverConfiguration.getPort());
			this.startSSLServer();
		} else {
			LOG.info("启动Server,port={}", serverConfiguration.getPort());
			this.startServer();
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
							.body(CR.error("超出QPS限制,qps = " + serverConfiguration.getConcurrentQuantity()))
							.writeAndFlushAndClose();

					socket.close();

				} else {
					ZServer.ZE.executeInQueue(() -> {
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

	private void startServer() {


		ServerSocket serverSocket = null;
		try {

			ZServer.LOG.trace("zserver开始启动,serverPort={}",ZServer.FRAMEWORK_PROPERTIES.getServerPort());
			serverSocket = new ServerSocket(ZServer.FRAMEWORK_PROPERTIES.getServerPort());
			ZServer.LOG.trace("zserver启动成功，等待连接,serverPort={}",ZServer.FRAMEWORK_PROPERTIES.getServerPort());
			while (true) {
				final Socket socket = serverSocket.accept();
				final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
				final boolean allow = Counter.allow(ZServer.Z_SERVER_QPS, serverConfiguration.getConcurrentQuantity());
				if (!allow) {

					final ZResponse response = new ZResponse(socket.getOutputStream());

					response.contentType(HeaderEnum.JSON.getType())
							.httpStatus(HttpStatus.HTTP_403.getCode())
							.body(CR.error("超出QPS限制,qps = " + serverConfiguration.getConcurrentQuantity()))
							.writeAndFlushAndClose();


					socket.close();

				} else {
					ZServer.ZE.executeInQueue(() -> {
						final Task task = new Task(socket);
						final ZRequest request = task.readAndParse();
						task.invoke(request);
					});
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (serverSocket != null) {
					serverSocket.close();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
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
