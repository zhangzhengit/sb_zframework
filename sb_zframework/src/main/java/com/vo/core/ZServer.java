/*
 *
 */
package com.vo.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.vo.conf.ZFrameworkDatasourcePropertiesLoader;
import com.vo.conf.ZFrameworkProperties;
import com.vo.core.ZLog2;
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

	public static final String DEFAULT_ZFRAMEWORK_HTTP_THREAD_NAME_PREFIX = "zframework-http-thread-";

	private static final ZLog2 LOG = ZLog2.getInstance();
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
				ZServer.ZE.executeInQueue(() -> {
					final Task task = new Task(socket);
				});

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
}
