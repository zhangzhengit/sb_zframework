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

import org.apache.catalina.Server;
import org.apache.catalina.webresources.CachedResource;
import org.checkerframework.checker.units.qual.m;

import com.google.common.collect.Maps;
import com.vo.conf.ServerConfiguration;
import com.vo.conf.ZFrameworkDatasourcePropertiesLoader;
import com.vo.conf.ZFrameworkProperties;
import com.vo.core.HRequest.RequestLine;
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
				final boolean allow = Counter.allow(serverConfiguration.getConcurrentQuantity());
				if (!allow) {

					System.out.println("qps 超了,time = " + LocalDateTime.now() + "\t" + "qps = "
							+ serverConfiguration.getConcurrentQuantity());

					final HResponse response = new HResponse(socket.getOutputStream());
					response.writeAndFlushAndClose(ContentTypeEnum.JSON, HttpStatus.HTTP_403.getCode(),
							CR.error(HttpStatus.HTTP_403.getCode(), "超出QPS限制,qps = " + serverConfiguration.getConcurrentQuantity()));

					socket.close();
				} else {
					ZServer.ZE.executeInQueue(() -> {
						final Task task = new Task(socket);
						final HRequest request = task.readAndParse();
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

		private static final Map<Long, Long> map = new ConcurrentHashMap<>(4, 1F);

		public static boolean allow(final long qps) {
			final long currentTimeMillis = System.currentTimeMillis();
			final long seconds = currentTimeMillis / 1000L;

			final Long v = map.get(seconds);
			if (v == null) {
				map.clear();
			}
			final long count = v == null ? 1 : v + 1;
			map.put(seconds, count);

			return count <= qps;
		}

	}
}
