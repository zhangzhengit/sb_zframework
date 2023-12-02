package com.vo.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vo.cache.J;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.http.HttpStatus;
import com.votool.common.CR;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

import cn.hutool.core.util.StrUtil;

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

	private static final ServerConfigurationProperties SERVER_CONFIGURATION = ZSingleton
			.getSingletonByClass(ServerConfigurationProperties.class);

	public final static ZE ZE = ZES.newZE(SERVER_CONFIGURATION.getThreadCount(),
			DEFAULT_ZFRAMEWORK_NIO_HTTP_THREAD_NAME_PREFIX);

	@Override
	public void run() {
		final ServerConfigurationProperties serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);
		if (serverConfiguration.getSslEnable()) {
			LOG.trace("SSL启用，启动SSLServer,port={}", serverConfiguration.getPort());
			ZServer.startSSLServer();
		} else {
			final Integer port = SERVER_CONFIGURATION.getPort();
			final String serverPortProperty = System.getProperty("server.port");
			final Integer serverPort  = StrUtil.isEmpty(serverPortProperty) ? port : Integer.valueOf(serverPortProperty);

			LOG.trace("启动Server,port={}", serverPort);
			final NioLongConnectionServer nioLongConnectionServer = new NioLongConnectionServer();
			nioLongConnectionServer.startNIOServer(serverPort);
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

				final boolean allow = QPSCounter.allow(ZServer.Z_SERVER_QPS, SERVER_CONFIGURATION.getQps(), QPSEnum.SERVER);
				if (!allow) {

					final ZResponse response = new ZResponse(socket.getOutputStream());

					response
							.httpStatus(HttpStatus.HTTP_403.getCode())
							.contentType(HeaderEnum.JSON.getType())
							.body(J.toJSONString(CR.error("zserver-超出QPS限制,qps = " + SERVER_CONFIGURATION.getQps()), Include.NON_NULL))
							.write();

					socket.close();

				} else {
					ZServer.ZE.executeInQueue(() -> {
						// FIXME 2023年7月4日 上午10:26:22 zhanghen: 用TaskNIO
						final Task task = new Task(socket);
						final ZRequest request = task.readAndParse();
						try {
							// FIXME 2023年10月21日 下午7:46:20 zhanghen: ssl暂不支持了，
							// 修改此处，或者改用nio ssl server
							task.invoke(request);
						} catch (IllegalAccessException | InvocationTargetException e) {
							e.printStackTrace();
						}
					});
				}

			}
		} catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException
				| UnrecoverableKeyException | KeyManagementException e) {
			e.printStackTrace();
		}
	}

}
