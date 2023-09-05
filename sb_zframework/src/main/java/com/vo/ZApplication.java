package com.vo;

import java.util.Arrays;

import com.vo.conf.ServerConfiguration;
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

}
