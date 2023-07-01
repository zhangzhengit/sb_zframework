/*
 *
 */
package com.vo.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	private final static ZE ZE = ZES.newZE(FRAMEWORK_PROPERTIES.getThreadCount(), FRAMEWORK_PROPERTIES.getThreadNamePrefix());

	@Override
	public void run() {

		this.start0();
	}

	private void start0() {

		ServerSocket serverSocket = null;
		try {

			ZServer.LOG.trace("zserver开始启动,serverPort={}",FRAMEWORK_PROPERTIES.getServerPort());
			serverSocket = new ServerSocket(FRAMEWORK_PROPERTIES.getServerPort());
			ZServer.LOG.trace("zserver启动成功，等待连接,serverPort={}",FRAMEWORK_PROPERTIES.getServerPort());
			while (true) {
				final Socket socket = serverSocket.accept();
//				System.out.println("new-socket = " + socket);
				final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
				final boolean allow = Counter.allow(Z_SERVER_QPS, serverConfiguration.getConcurrentQuantity());
				if (!allow) {

					final ZResponse response = new ZResponse(socket.getOutputStream());
					response.writeAndFlushAndClose(HeaderEnum.JSON, HttpStatus.HTTP_403.getCode(),
							CR.error("超出QPS限制,qps = " + serverConfiguration.getConcurrentQuantity()));

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

				final Long v = map.get(key);
				if (v == null) {
					map.put(key, 1L);
					return true;
				}

				final long newCount = v + 1L;
				map.put(key, newCount);

				return newCount <= qps;
			}
		}

	}
}
