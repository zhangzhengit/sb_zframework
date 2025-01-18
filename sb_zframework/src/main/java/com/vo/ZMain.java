package com.vo;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.vo.cache.STU;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.PortChecker;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.email.ZMail;
import com.vo.email.ZMailNotificationConfigurationProperties;

/**
 * 启动类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
final class ZMain {

	private static final ZLog2 LOG = ZLog2.getInstance();

	/**
	 * 本应用所在的包
	 */
	public static final String COM_VO = "com.vo";

	public static final String Z_SERVER_THREAD = "ZServer-Thread";

	public static void start(final List<String> packageNameList, final boolean httpEnable, final String[] args) {

		final ZApplicationStartupInfo startupInfo = new ZApplicationStartupInfo(packageNameList, httpEnable,  args);

		ZMain.LOG.info("zframework开始启动");
		final Set<String> pns = Sets.newHashSet(COM_VO);
		pns.addAll(packageNameList);

		final ZApplicationStartupProcessor processor = new ZApplicationStartupAdapter();

		try {
			processor.startValidator(startupInfo);

			// 校验 @ZEventListener 方法
			processor.startEventPublisher(startupInfo);

			// 0 读取 @ZConfigurationProperties 配置，创建配置类
			processor.scanConfigurationProperties(startupInfo);

			// 0.01 校验端口号
			// FIXME 2024年12月31日 下午6:34:24 zhangzhen : 看看把这一步放在最前面，要先更改 scanConfigurationProperties
			// 把 ServerConfigurationProperties 和zf.properties 中的server.port读取出来然后才可以把本步放最前面
			final Integer serverPort = checkPort();

			// 0.1
			// @ZConfigurationProperties 初始化之后就开始执行starter
			processor.loadStarter();

			// 0.2 扫描 @ZConfiguration类，生成配置
			processor.scanConfiguration(startupInfo);

			// 1 初始化 对象生成器
			processor.startObjectGenerator(startupInfo);

			// 2 创建 @ZComponent和@ZService 对象，如果类中有被代理的自定义注解，则创建此类的代理类
			processor.scanComponent(startupInfo);

			// 3 创建 @ZController 对象
			processor.scanController(startupInfo);

			// 3.1 扫描 @ZControllerAdvice 的类
			processor.scanControllerAdvice(startupInfo);

			// 4.1 扫描组件的 @ZAutowired 字段 并注入值
			processor.injectAutowired(startupInfo);

			// 4.2@ZAutowired 全部执行完了，判断一下必须的是否null
			processor.aftertAutowiredInject(startupInfo);

			// 5 扫描组件的 @ZValue 字段 并注入配置文件的值
			processor.injectValue(startupInfo);

			// 6 校验缓存注解是否正确使用了
			processor.validatedCache(startupInfo);

			// 7 设置静态资源的路径
			processor.setStaticPath(startupInfo);

			// 8 打印一下配置类信息
			processor.printZConfigurationProperties(startupInfo);

			// 9 扫描自定义拦截器
			processor.scanHandlerInterceptor(startupInfo);

			// 10 执行 ZCommandLineRunner
			processor.runCommandLineRunner(startupInfo);

			// 11 API文档
			// FIXME 2024年12月17日 下午6:17:04 zhangzhen : 加一个参数：是否启动apidoc
			//			DocScanner.scan(packageNameList.toArray(new String[]{}));

			// 12 最后再校验一遍 @ZAutowired 字段都有值，因为在上次校验后可能被修改了
			processor.aftertAutowiredInject(startupInfo);

			// 13 启动http服务器
			processor.startHttpServer(serverPort, startupInfo);

			// 14 画一个banner，无实际用途
			processor.showBanner();


			// FIXME 2025年1月18日 下午7:35:17 zhangzhen : 这个通知功能也抽出一个接口，可以供用户自己实现
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {

				LOG.warn("程序Shutdown");

				final ZMailNotificationConfigurationProperties mn = ZContext.getBean(ZMailNotificationConfigurationProperties.class);

				final Boolean enable = mn.getEnable();
				if (!Boolean.TRUE.equals(enable)) {
					return;
				}

				final String projectPath = System.getProperty("user.dir");
				final String projectName = projectPath.substring(projectPath.lastIndexOf(File.separator) + 1);

				final String subject = "[" + projectName + "]程序停止通知";

				final ZMail mail = ZContext.getBean(ZMail.class);
				final String body =
						"<html>\r\n"
								+ "<head>\r\n"
								+ "<meta charset=\"UTF-8\">\r\n"
								+ "</head>\r\n"
								+ "<body>\r\n"
								+ "	<h1>程序停止通知</h1>\r\n"
								+ "	<h3>如果不是你手动停止的，请立即查看原因。</h3>\r\n"
								+ "	<h3>如果是由你手动停止的，请忽略此邮件。</h3>\r\n"
								+ "	<h3>发送时间："+LocalDateTime.now()+"</h3>\r\n"
								+ "</body>\r\n"
								+ "</html>";

				final Set<String> rs = mn.getReceiver();
				for (final String receiver : rs) {
					mail.send(subject, body, receiver, "text/html;charset=UTF-8");
				}

			}));


			// FIXME 2025年1月18日 下午7:54:25 zhangzhen : ZMNCP新增配置项：关注某些事件，触发时发邮件通知，比如：OOM

			//			Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//				System.out.println(
			//						Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t" + "ZMain.start()-setDefaultUncaughtExceptionHandler-执行了");
			//				System.out.println("t = " + t);
			//				System.out.println("e = " + e);
			//			});

		} catch (final Exception e) {
			final String message = Task.gExceptionMessage(e);
			LOG.error("程序启动失败，请检查代码。\n\terrorMessage={}", message);
			LOG.error("程序启动失败，具体原因请看上面日志");
			System.exit(0);
		}
	}

	private static Integer checkPort() {
		final ServerConfigurationProperties serverConfigurationProperties = ZContext
				.getBean(ServerConfigurationProperties.class);

		final String serverPortProperty = System.getProperty("server.port");
		final Integer serverPort = STU.isEmpty(serverPortProperty) ? serverConfigurationProperties.getPort()
				: Integer.valueOf(serverPortProperty);

		if (!PortChecker.isPortIllegal(serverPort)) {
			LOG.error("端口[{}]不合法,请检查,更换端口在[{}]到[{}]之间", serverPort, PortChecker.PORT_MIN, PortChecker.PORT_MAX);
			System.exit(0);
		}

		if (PortChecker.isPortInUse(serverPort)) {
			LOG.error("端口[{}]已被占用,请检查,更换端口或者停掉正在使用此端口的进程?", serverPort);
			System.exit(0);
		}

		return serverPort;
	}

	public static void main(final String[] args) {

	}
}
