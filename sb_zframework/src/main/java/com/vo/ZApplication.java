package com.vo;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.vo.configuration.CommonConfigurationProperties;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.configuration.ZProperties;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;
import com.vo.exception.StartupException;

import cn.hutool.core.collection.CollUtil;

/**
 *
 * zf 启动类，供依赖此项目的main方法启动
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
// FIXME 2023年10月23日 下午10:11:08 zhanghen: 测试遇到的问题：
/*
 * 1 linux ab 命令  http://192.168.1.14/form?name=A&id=1  测试如下方法，取不到id参数。其不支持get请求对参数进行编码
 *
 */

public class ZApplication {

	private static final ZLog2 LOG = ZLog2.getInstance();

	/**
	 * 启动程序
	 *
	 * @param scanPackageNameList 要扫描的包名，如：com
	 * @param httpEnable
	 * @param args
	 */
	public static ZApplicationContext run(final List<String> scanPackageNameList, final boolean httpEnable, final String[] args) {

		LOG.info("ZApplication开始启动，scanPackageName={},	={},args={}", scanPackageNameList, httpEnable,
				Arrays.toString(args));

		if (CollUtil.isEmpty(scanPackageNameList)) {
			throw new IllegalArgumentException("启动出错,scanPackageName 不能为空");
		}

		try {
			final String packageName = g();
			final Optional<String> findAny = scanPackageNameList.stream().filter(p -> Objects.equals(p, packageName))
					.findAny();
			if (!findAny.isPresent()) {
				throw new StartupException("scanPackageNameList必须包含启动类所在的包。当前scanPackageNameList=" + scanPackageNameList
						+ ",启动类所在包名=" + packageName);
			}

		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		ZProperties.getInstance().addProperty("server.scanPackage", scanPackageNameList);


		final long t1 = System.currentTimeMillis();
		ZMain.start(Lists.newArrayList(scanPackageNameList), httpEnable, args);
		final long t2 = System.currentTimeMillis();

		loadStarter();

		final long freeMemory = Runtime.getRuntime().freeMemory();
		final long totalMemory = Runtime.getRuntime().totalMemory();
		final long maxMemory = Runtime.getRuntime().maxMemory();

		final Object serverConfiguration = ZSingleton.getSingletonByClass(ServerConfigurationProperties.class);

		LOG.info("ZApplication启动成功,耗时{}秒,freeMemory={}MB,totalMemory={}MB,maxMemory={}MB,ServerConfiguration={}",
						((t2 - t1) / 1000),
						freeMemory / 1024/1024,
						totalMemory / 1024/1024,
						maxMemory / 1024/1024,
						serverConfiguration
					);

		final ZApplicationContext context =
		new ZApplicationContext(ImmutableList.copyOf(scanPackageNameList), httpEnable, args, ZProperties.getInstance());
		return context;
	}


	/**
	 *	TODO 做一个类似spring.factories的功能，给zf做几个starter。先把zf中的通用类提取出来 一个common工程，然后zf也是依赖此common工程
	 *
	 * @author zhangzhen
	 * @date 2024年2月17日
	 */
	private static void loadStarter() {
		final ClassLoader classLoader = ZApplication.class.getClassLoader();
		try {
			final CommonConfigurationProperties common = ZContext.getBean(CommonConfigurationProperties.class);
			LOG.debug("/resources/META-INF/下指定的启动文件名称={}", common.getStarterName());
			final Enumeration<URL> resources = classLoader.getResources("META-INF/" + common.getStarterName());

//			int c = 0;
			while (resources.hasMoreElements()) {
				final URL url = resources.nextElement();
				final Properties properties = new Properties();
				properties.load(url.openStream());

				final int size = properties.size();
//				c = size;

				LOG.debug("/resources/META-INF/下文件size={}", size);
				// FIXME 2024年2月17日 下午7:40:59 zhanghen: 读取，使用 @ZCP的读取方式。然后启动对应的类，先让启动类必须实现一个接口
			}
//			if (c == 0) {
//				LOG.debug("/resources/META-INF/下无文件:{}", common.getStarterName());
//			}else {
//				LOG.debug("/resources/META-INF/文件个数=:{}", common.getStarterName());
//			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
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
