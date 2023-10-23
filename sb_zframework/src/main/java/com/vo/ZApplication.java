package com.vo;

import java.util.Arrays;
import java.util.Objects;

import com.vo.conf.ServerConfiguration;
import com.vo.conf.ZProperties;
import com.vo.core.StartupException;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;

import cn.hutool.core.util.StrUtil;

/**
 *
 * zf 启动类，供依赖此项目的main方法启动
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZApplication {

	private static final ZLog2 LOG = ZLog2.getInstance();
	
	/**
	 * 启动程序
	 *
	 * @param scanPackageName 要扫描的包名，如：com
	 * @param httpEnable
	 * @param args
	 */
	public static void run(final String scanPackageName, final boolean httpEnable, final String[] args) {

		LOG.info("ZApplication开始启动，scanPackageName={},httpEnable={},args={}", scanPackageName, httpEnable,
				Arrays.toString(args));

		try {
			final String packageName = g();
			if (!Objects.equals(scanPackageName, packageName)) {
				throw new StartupException(
						"scanPackageName必须与启动类所在包名一致。当前scanPackageName=" + scanPackageName + ",启动类所在包名=" + packageName);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		ZProperties.getInstance().addProperty("server.scanPackage", scanPackageName);

		if (StrUtil.isEmpty(scanPackageName)) {
			throw new IllegalArgumentException("启动出错,scanPackageName 不能为空");
		}

		final long t1 = System.currentTimeMillis();
		ZMain.start(scanPackageName, httpEnable, args);
		final long t2 = System.currentTimeMillis();

		final long freeMemory = Runtime.getRuntime().freeMemory();
		final long totalMemory = Runtime.getRuntime().totalMemory();
		final long maxMemory = Runtime.getRuntime().maxMemory();

		final Object serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);

		LOG.info("ZApplication启动成功,耗时{}秒,freeMemory={}MB,totalMemory={}MB,maxMemory={}MB,ServerConfiguration={}",
						((t2 - t1) / 1000),
						freeMemory / 1024/1024,
						totalMemory / 1024/1024,
						maxMemory / 1024/1024,
						serverConfiguration
					);
	}


	private static String g() {
		final StackTraceElement[] st = Thread.currentThread().getStackTrace();
		// 写死3
		final int s = 3;
		final StackTraceElement stackTraceElement = st[s];
		final String className = stackTraceElement.getClassName();

		final int i = className.lastIndexOf(".");
		if (i < 0) {
			throw new StartupException("获取程序启动类所在包名异常，请确认启动类所在包名形式为A.B，如：com.vo");
		}

		final String packageName = className.substring(0, i);
		System.out.println("packageName = " + packageName);
		final String regex = "\\w+\\.\\w+";
		if (packageName.matches(regex)) {
			return packageName;
		}

		throw new StartupException("获取程序启动类所在包名异常，当前包名为" + packageName + "，请确认启动类所在包名形式为A.B，如：com.vo");
	}

}
