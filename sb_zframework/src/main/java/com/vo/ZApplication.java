package com.vo;

import com.vo.conf.ServerConfiguration;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;

/**
 *
 * zf 启动类，供依赖此项目的main方法启动
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
// FIXME 2023年7月1日 上午4:40:42 zhanghen: TODO :静态资源 加一个配置：指定的目录位置,不设死为 resources了

public class ZApplication {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void run(final String[] args) {
		final long t1 = System.currentTimeMillis();
		ZMain.main(args);
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
