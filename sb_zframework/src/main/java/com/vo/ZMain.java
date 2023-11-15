package com.vo;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Sets;
import com.vo.anno.ZCommandLineRunner;
import com.vo.anno.ZCommandLineRunnerScanner;
import com.vo.anno.ZComponent;
import com.vo.anno.ZConfiguration;
import com.vo.anno.ZConfigurationProperties;
import com.vo.anno.ZController;
import com.vo.anno.ZService;
import com.vo.aop.ZAOP;
import com.vo.aop.ZCacheScanner;
import com.vo.cache.ZCacheableValidator;
import com.vo.conf.ServerConfiguration;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZServer;
import com.vo.core.ZSessionMap;
import com.vo.core.ZSingleton;
import com.vo.exception.ZControllerAdviceScanner;
import com.vo.scanner.ZApplicationEventPublisher;
import com.vo.scanner.ZAutowiredScanner;
import com.vo.scanner.ZComponentScanner;
import com.vo.scanner.ZConfigurationPropertiesScanner;
import com.vo.scanner.ZConfigurationScanner;
import com.vo.scanner.ZControllerInterceptorScanner;
import com.vo.scanner.ZControllerScanner;
import com.vo.scanner.ZHandlerInterceptorScanner;
import com.vo.scanner.ZValueScanner;
import com.vo.validator.ZValidator;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;

/**
 * 启动类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZMain {

	private static final ZLog2 LOG = ZLog2.getInstance();

	/**
	 * 本应用所在的包
	 */
	public static final String COM_VO = "com.vo";

	public static final String STATIC_RESOURCES_PROPERTY_NAME = "resource.path-" + UUID.randomUUID();

	private static final String Z_SERVER_THREAD = "ZServer-Thread";


	public static void start(final List<String> packageNameList, final boolean httpEnable, final String[] args) {

		ZMain.LOG.trace("zframework开始启动");
		final Set<String> pns = Sets.newHashSet(COM_VO);
		pns.addAll(packageNameList);

		final String[] packageName = pns.toArray(new String[0]);

		try {
			// 最先校验：校验注解用的字段是否支持
			ZValidator.start(packageName);

			// 校验 @ZEventListener 方法
			ZApplicationEventPublisher.start(packageName);

			// 0 读取 @ZConfigurationProperties 配置，创建配置类
			ZConfigurationPropertiesScanner.scanAndCreate(packageName);

			// 0.1 扫描 @ZConfiguration类，生成配置
			ZConfigurationScanner.scanAndCreate(packageName);

			// 1 初始化 对象生成器
			ZObjectGeneratorStarter.start(packageName);

			// 2 创建 @ZComponent 对象，如果类中有被代理的自定义注解，则创建此类的代理类
			ZComponentScanner.scanAndCreate(ZComponent.class,packageName);
			ZComponentScanner.scanAndCreate(ZService.class,packageName);

			if (httpEnable) {
				// 3 创建 @ZController 对象
				ZControllerScanner.scanAndCreateObject(packageName);
				// 3.1 创建 @ZController 的拦截器对象
				ZControllerInterceptorScanner.scan(packageName);
				// 3.2 扫描 @ZControllerAdvice 的类
				ZControllerAdviceScanner.scan(packageName);
			}

			// 4 扫描组件的 @ZAutowired 字段 并注入值
			ZAutowiredScanner.inject(ZService.class, packageName);
			ZAutowiredScanner.inject(ZComponent.class, packageName);
			ZAutowiredScanner.inject(ZController.class, packageName);
			ZAutowiredScanner.inject(ZConfiguration.class, packageName);
			ZAutowiredScanner.inject(ZAOP.class, packageName);

			// 5 扫描组件的 @ZValue 字段 并注入配置文件的值
			ZValueScanner.inject(packageName);


			ZCacheableValidator.validated(packageName);

			final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
			if (StrUtil.isNotEmpty(serverConfiguration.getStaticPath())) {
				System.setProperty(STATIC_RESOURCES_PROPERTY_NAME, serverConfiguration.getStaticPath());
				System.out.println("staticPath = " + serverConfiguration.getStaticPath());
			}

			// 验证缓存注解配置是否合理
			ZCacheScanner.scanAndValidate();

			// @ZAutowired 全部执行完了，判断一下必须的是否null
			ZAutowiredScanner.after();

			// 打印一下配置类信息
			if (Boolean.TRUE.equals(ZContext.getBean(ServerConfiguration.class).getPrintConfigurationProperties())) {
				printZConfigurationProperties();
			}

			// 扫描自定义拦截器
			ZHandlerInterceptorScanner.scan();

			// 执行 ZCommandLineRunner
			for (final Object zclr : ZCommandLineRunnerScanner.scan(packageName)) {
				((ZCommandLineRunner) zclr).run(args);
			}

			if (httpEnable) {
				final ZServer zs = new ZServer();
				zs.setName(Z_SERVER_THREAD);
				zs.start();
				ZSessionMap.sessionTimeoutJOB();
			}

		} catch (final Exception e) {
			final String message = Task.gExceptionMessage(e);
			LOG.error("程序启动失败，请检查代码。errorMessage=\n\t{}", message);
			System.exit(0);
		}
	}

	private static void printZConfigurationProperties() {
		LOG.info("开始打印@{}配置类信息", ZConfigurationProperties.class.getSimpleName());

		final ImmutableCollection<Object> bs = ZContext.all().values();
		for (final Object bean : bs) {
			if(!bean.getClass().isAnnotationPresent(ZConfigurationProperties.class)) {
				continue;
			}

			final Field[] fs = bean.getClass().getDeclaredFields();
			for (final Field f : fs) {
				try {
					f.setAccessible(true);
					final Object value = f.get(bean);
					// FIXME 2023年11月8日 下午9:08:00 zhanghen: XXX 是否会打印出某些敏感信息？
					// 新增注解标记下不打印？似乎没必要
					LOG.info("配置项{}.{}={}", bean.getClass().getSimpleName(), f.getName(), value);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}

		}

		LOG.info("打印@{}配置类信息完成", ZConfigurationProperties.class.getSimpleName());

	}

	public static void main(final String[] args) {

	}
}
