package com.vo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.vo.configuration.ZProperties;
import com.vo.core.ZLog2;
import com.vo.exception.StartupException;

/**
 *
 * zf 启动类，供依赖此项目启动类的main方法来调用
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZApplication {

	private static final String ZF_THREAD = "zfT";
	private static final ZLog2 LOG = ZLog2.getInstance();

	public static ZApplicationContext run(final String[] args) {
		return run(Collections.emptyList(), true, args);
	}

	public static ZApplicationContext run(final boolean httpEnable, final String[] args) {
		return run(Collections.emptyList(), httpEnable, args);
	}

	/**
	 * 启动程序，注意：本方法是提供给程序的启动类的main方法来调用的，需要并且只需要启动类调用一次本方法。
	 *
	 * @param scanPackageNameList 要扫描的包名，如：com.xx.a、com.xx.b等等。
	 * 							  如果包名都以一个相同的前缀开头，并且启动里位于顶级包路径，
	 *                            并且没有需要额外扫描的包路径，则本参数可以直接传empty；
	 *                            如：启动类位于com.vo 其他包都是比如
	 *                            com.vo.api/com.vo.core/com.vo.service 的形式。
	 *
	 *                            否则需要传入要扫描的包路径
	 *
	 * @param httpEnable          是否启用http服务器
	 * @param args                java 命令行传来的参数
	 */
	public static ZApplicationContext run(final List<String> scanPackageNameList, final boolean httpEnable, final String[] args) {

		final List<String> cpl = scanPackageNameList.isEmpty() ? new ArrayList<>() : scanPackageNameList;
		Thread.currentThread().setName(ZF_THREAD);
		LOG.info("ZApplication开始启动，scanPackageName={},httpEnable={},args={}", scanPackageNameList, httpEnable,
				Arrays.toString(args));

		final String packageName = g();
		final Optional<String> findAny = cpl.stream().filter(p -> Objects.equals(p, packageName))
				.findAny();
		if (!findAny.isPresent()) {
			cpl.add(packageName);
		}

		final long t1 = System.currentTimeMillis();
		ZMain.start(Lists.newArrayList(cpl), httpEnable, args);
		final long t2 = System.currentTimeMillis();

		final long freeMemory = Runtime.getRuntime().freeMemory();
		final long totalMemory = Runtime.getRuntime().totalMemory();
		final long maxMemory = Runtime.getRuntime().maxMemory();

		final String className = gZAppName();

		final String javaVmName = System.getProperty("java.vm.name");
		final String javaVmVersion = System.getProperty("java.vm.version");

		LOG.info("{}成功启动,耗时[{}]秒在VM[{}].freeMemory={}MB,totalMemory={}MB,maxMemory={}MB",
				className,
				((t2 - t1) / 1000.0),
				javaVmName + ' ' + javaVmVersion,
				freeMemory / 1024 / 1024,
				totalMemory / 1024 / 1024,
				maxMemory / 1024 / 1024);

		return new ZApplicationContext(ImmutableList.copyOf(scanPackageNameList),
				httpEnable, args, ZProperties.getInstance());
	}

	private static String gZAppName() {
		final StackTraceElement[] st = Thread.currentThread().getStackTrace();
		final StackTraceElement stackTraceElement = st[4];
		final String className = stackTraceElement.getClassName();

		final int i = className.lastIndexOf(".");
		if (i <= -1) {
			return className;
		}
		return className.substring(i + 1);
	}

	public static String getAppName() {
		return M.getAppName();
	}

	/**
	 * 获取本类的run的调用者所在的包名
	 *
	 * @return
	 */
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
		final String regex = "\\w+\\.\\w+";
		if (packageName.matches(regex)) {
			return packageName;
		}

		final String message = "获取程序启动类所在包名异常，当前包名为" + packageName + "，请确认启动类所在包名形式为A.B，如：com.vo";
		LOG.error("启动失败,message=[{}]", message);
		throw new StartupException(message);
	}
}
