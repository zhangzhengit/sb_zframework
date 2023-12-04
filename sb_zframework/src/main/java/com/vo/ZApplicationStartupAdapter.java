package com.vo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import com.google.common.collect.ImmutableCollection;
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
import com.vo.configuration.ServerConfigurationProperties;
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
import com.vo.scanner.ZControllerScanner;
import com.vo.scanner.ZHandlerInterceptorScanner;
import com.vo.scanner.ZValueScanner;
import com.vo.validator.ZValidator;

import cn.hutool.core.util.StrUtil;

/**
 * ZApplication 的启动流程适配类，如需自定义或查插入代码等，覆盖本类方法
 *
 * @author zhangzhen
 * @date 2023年12月4日
 *
 */
public class ZApplicationStartupAdapter implements ZApplicationStartupProcessor {

	private static final ZLog2 LOG = ZLog2.getInstance();

	@Override
	public void startValidator(final ZApplicationStartupInfo startupInfo) {
		ZValidator.start(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void startEventPublisher(final ZApplicationStartupInfo startupInfo) {
		ZApplicationEventPublisher.start(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void scanConfigurationProperties(final ZApplicationStartupInfo startupInfo) throws Exception {
		ZConfigurationPropertiesScanner.scanAndCreate(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void scanConfiguration(final ZApplicationStartupInfo startupInfo) throws Exception {
		ZConfigurationScanner.scanAndCreate(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void startObjectGenerator(final ZApplicationStartupInfo startupInfo) {
		ZObjectGeneratorStarter.start(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void scanComponent(final ZApplicationStartupInfo startupInfo) {
		final Class[] cA = { ZComponent.class, ZService.class };
		for (final Class<? extends Annotation> cls : cA) {
			ZComponentScanner.scanAndCreate(cls, startupInfo.getPackageNameList().toArray(new String[0]));
		}
	}

	@Override
	public void scanControllerAdvice(final ZApplicationStartupInfo startupInfo) {
		if (startupInfo.isHttpEnable()) {
			ZControllerAdviceScanner.scan(startupInfo.getPackageNameList().toArray(new String[0]));
		}
	}

	@Override
	public void scanController(final ZApplicationStartupInfo startupInfo) {
		if (startupInfo.isHttpEnable()) {
			ZControllerScanner.scanAndCreateObject(startupInfo.getPackageNameList().toArray(new String[0]));
		}
	}

	@Override
	public void injectAutowired(final ZApplicationStartupInfo startupInfo) {
		final Class[] cA = { ZService.class, ZComponent.class, ZController.class, ZConfiguration.class, ZAOP.class };
		for (final Class cls : cA) {
			ZAutowiredScanner.inject(cls, startupInfo.getPackageNameList().toArray(new String[0]));
		}
	}

	@Override
	public void injectValue(final ZApplicationStartupInfo startupInfo) {
		ZValueScanner.inject(startupInfo.getPackageNameList().toArray(new String[0]));
	}

	@Override
	public void validatedCache(final ZApplicationStartupInfo startupInfo) {
		ZCacheableValidator.validated(startupInfo.getPackageNameList().toArray(new String[0]));
		ZCacheScanner.scanAndValidate();
	}

	@Override
	public void setStaticPath(final ZApplicationStartupInfo startupInfo) {
		final ServerConfigurationProperties serverConfiguration = ZSingleton
				.getSingletonByClass(ServerConfigurationProperties.class);
		if (StrUtil.isNotEmpty(serverConfiguration.getStaticPath())) {
			System.setProperty(ZMain.STATIC_RESOURCES_PROPERTY_NAME, serverConfiguration.getStaticPath());
			System.out.println("staticPath = " + serverConfiguration.getStaticPath());
		}
	}


	@Override
	public void printZConfigurationProperties(final ZApplicationStartupInfo startupInfo) {
		if (Boolean.TRUE
				.equals(ZContext.getBean(ServerConfigurationProperties.class).getPrintConfigurationProperties())) {

			LOG.info("开始打印@{}配置类信息", ZConfigurationProperties.class.getSimpleName());

			final ImmutableCollection<Object> bs = ZContext.all().values();
			for (final Object bean : bs) {
				if (!bean.getClass().isAnnotationPresent(ZConfigurationProperties.class)) {
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

	}

	@Override
	public void aftertAutowiredInject(final ZApplicationStartupInfo startupInfo) {
		ZAutowiredScanner.after();
	}

	@Override
	public void scanHandlerInterceptor(final ZApplicationStartupInfo startupInfo) {
		ZHandlerInterceptorScanner.scan();
	}

	@Override
	public void runCommandLineRunner(final ZApplicationStartupInfo startupInfo) throws Exception {
		for (final Object zclr : ZCommandLineRunnerScanner.scan(startupInfo.getPackageNameList().toArray(new String[0]))) {
			((ZCommandLineRunner) zclr).run(startupInfo.getArgs());
		}
	}

	@Override
	public void startHttpServer(final int httpPort, final ZApplicationStartupInfo startupInfo) {
		if (startupInfo.isHttpEnable()) {
			final ZServer zs = new ZServer(httpPort);
			zs.setName(ZMain.Z_SERVER_THREAD);
			zs.start();
			ZSessionMap.sessionTimeoutJOB();
		}
	}

}