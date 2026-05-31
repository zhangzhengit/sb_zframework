package vo.zframework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import vo.log.core.ZLog2;
import vo.zframework.configuration.ServerConfigurationProperties;
import vo.zframework.configuration.ZProperties;
import vo.zframework.core.ZContext;
import vo.zframework.exception.StartupException;

/**
 *
 * zf 启动类，供依赖此项目启动类的main方法来调用
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZApplication {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final String APP_PACKAGE_NAME = "vo";

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

		final long start = System.currentTimeMillis();

		final List<String> cpl = scanPackageNameList.isEmpty() ? new ArrayList<>() : scanPackageNameList;

		// 加入本工程的顶级包名
		if(!cpl.contains(APP_PACKAGE_NAME)) {
			cpl.add(APP_PACKAGE_NAME);
		}

		LOG.debug("APP开始启动,扫描包名={},args={},httpEnable={}",
				cpl, Arrays.toString(args), httpEnable);

		final String packageName = g();
		final Optional<String> findAny = cpl.stream().filter(p -> Objects.equals(p, packageName))
				.findAny();
		if (!findAny.isPresent()) {
			cpl.add(packageName);
		}


		ZMain.start(cpl, httpEnable, args);
		final long end = System.currentTimeMillis();

		final long maxMemory = Runtime.getRuntime().maxMemory();

		final String javaVmName = System.getProperty("java.vm.name");
		final String javaVmVersion = System.getProperty("java.vm.version");

		LOG.debug("APP启动成功,耗时[{}]秒,maxMemory=[{}]MB,vm=[{}]", (end - start) / 1000.0, maxMemory / 1024 / 1024,
				javaVmName + ' ' + javaVmVersion);

		final ServerConfigurationProperties serverConfigurationProperties = ZContext
				.getBean(ServerConfigurationProperties.class);

		final String ok =
						"   ___  _  __\r\n"
					 + "  / _ \\| |/ /\r\n"
					 + " | | | | ' / \r\n"
					 + " | |_| | . \\ \r\n"
					 + "  \\___/|_|\\_\\"
					 + "\r\n"
					 + (httpEnable ? ("httpPort=" + serverConfigurationProperties.getPort()) : "")
					 + "\r\n"
					 + "启动耗时[" +((end - start) / 1000.0) + "]秒,maxMemory=["+(maxMemory / 1024 / 1024)+"]MB,vm=["+(javaVmName + ' ' + javaVmVersion)+"]"
					 ;

		System.out.println(ok);

		return new ZApplicationContext(ImmutableList.copyOf(scanPackageNameList),
				httpEnable, args, ZProperties.getInstance());
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
		// 写死4
		final int s = 4;
		final StackTraceElement stackTraceElement = st[s];
		final String className = stackTraceElement.getClassName();

		final int i = className.lastIndexOf(".");
		if (i < 0) {
			throw new StartupException("获取程序启动类所在包名异常，请确认启动类所在包名形式为A.B，如：com.vo");
		}

		final String packageName = className.substring(0, i);
		final String regex2 = "\\w+\\.\\w+";
		final String regex3 = "\\w+\\.\\w+.\\w+";
		final String regex4 = "\\w+\\.\\w+.\\w+.\\w+";
		if (packageName.matches(regex2) || packageName.matches(regex3) || packageName.matches(regex4)) {
			return packageName;
		}

		final String message = "获取程序启动类所在包名异常，当前包名为" + packageName
				+ "，请确认启动类所在包名形式为A.B/A.B.C/A.B.C.D，如：com.vo/com.vo.app/com.vo.my.app";
		LOG.error("启动失败,message=[{}]", message);
		throw new StartupException(message);
	}
}
