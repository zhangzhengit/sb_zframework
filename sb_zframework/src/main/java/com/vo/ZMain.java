package com.vo;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.vo.configuration.ServerConfigurationProperties;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;

/**
 * 启动类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public final class ZMain {

	private static final ZLog2 LOG = ZLog2.getInstance();

	/**
	 * 本应用所在的包
	 */
	public static final String COM_VO = "com.vo";

	public static final String STATIC_RESOURCES_PROPERTY_NAME = "resource.path-" + UUID.randomUUID();

	public static final String Z_SERVER_THREAD = "ZServer-Thread";


	public static void start(final List<String> packageNameList, final boolean httpEnable, final String[] args) {

		final ZApplicationStartupInfo startupInfo = new ZApplicationStartupInfo(packageNameList, httpEnable,  args);

		ZMain.LOG.trace("zframework开始启动");
		final Set<String> pns = Sets.newHashSet(COM_VO);
		pns.addAll(packageNameList);

		final ZApplicationStartupProcessor processor = new ZApplicationStartupAdapter();

		try {
			processor.startValidator(startupInfo);

			// 校验 @ZEventListener 方法
			processor.startEventPublisher(startupInfo);

			// 0 读取 @ZConfigurationProperties 配置，创建配置类
			processor.scanConfigurationProperties(startupInfo);

			// 0.1 扫描 @ZConfiguration类，生成配置
			processor.scanConfiguration(startupInfo);

			// 1 初始化 对象生成器
			processor.startObjectGenerator(startupInfo);

			// 2 创建 @ZComponent和@ZService 对象，如果类中有被代理的自定义注解，则创建此类的代理类
			processor.scanComponent(startupInfo);

			// 3 创建 @ZController 对象
			processor.scanController(startupInfo);
			// 3.1 扫描 @ZControllerAdvice 的类
			processor.scanControllerAdvice(startupInfo);

			// 4 扫描组件的 @ZAutowired 字段 并注入值
			processor.injectAutowired(startupInfo);

			// @ZAutowired 全部执行完了，判断一下必须的是否null
			processor.aftertAutowiredInject(startupInfo);

			// 5 扫描组件的 @ZValue 字段 并注入配置文件的值
			processor.injectValue(startupInfo);

			processor.validatedCache(startupInfo);

			processor.setStaticPath(startupInfo);

			// 验证缓存注解配置是否合理
			// 已放在 validatedCache 里了
//			ZCacheScanner.scanAndValidate();

			// 打印一下配置类信息
			processor.printZConfigurationProperties(startupInfo);

			// 扫描自定义拦截器
			processor.scanHandlerInterceptor(startupInfo);

			// 执行 ZCommandLineRunner
			processor.runCommandLineRunner(startupInfo);

			final String serverPortProperty = System.getProperty("server.port");
			// TODO : 判断 -Dserver.port=XXX 传来的参数是否合理
			final Integer serverPort  = StrUtil.isEmpty(serverPortProperty) ? ZContext.getBean(ServerConfigurationProperties.class).getPort() : Integer.valueOf(serverPortProperty);

			processor.startHttpServer(serverPort, startupInfo);

		} catch (final Exception e) {
			final String message = Task.gExceptionMessage(e);
			LOG.error("程序启动失败，请检查代码。errorMessage=\n\t{}", message);
			System.exit(0);
		}
	}

	public static void main(final String[] args) {

	}
}
