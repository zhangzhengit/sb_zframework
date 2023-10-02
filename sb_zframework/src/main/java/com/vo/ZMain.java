package com.vo;

import com.vo.anno.ZComponent;
import com.vo.anno.ZController;
import com.vo.aop.ZAOP;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZServer;
import com.vo.core.ZSessionMap;
import com.vo.core.ZSingleton;
import com.vo.scanner.ZAutowiredScanner;
import com.vo.scanner.ZComponentScanner;
import com.vo.scanner.ZConfigurationPropertiesScanner;
import com.vo.scanner.ZConfigurationScanner;
import com.vo.scanner.ZControllerInterceptorScanner;
import com.vo.scanner.ZControllerScanner;
import com.vo.scanner.ZValueScanner;

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

	public static final String STATIC_RESOURCES_PROPERTY_NAME = "resource.path-" + UUID.randomUUID();

	private static final String Z_SERVER_THREAD = "ZServer-Thread";

	public static void start(final String packageName,final boolean httpEnable, final String[] args) {

		ZMain.LOG.trace("zframework开始启动");

		// 0 读取 @ZConfigurationProperties 配置，创建配置类
		ZConfigurationPropertiesScanner.scanAndCreate(packageName);

		// 0.1 扫描 @ZConfiguration类，生成配置
		ZConfigurationScanner.scanAndCreate();

		// 1 初始化 对象生成器
		ZObjectGeneratorStarter.start();

		// 2 创建 @ZComponent 对象，如果类中有被代理的自定义注解，则创建此类的代理类
		ZComponentScanner.scanAndCreate();

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		final String scanPackage = serverConfiguration.getScanPackage();

		if (httpEnable) {
			// 3 创建 @ZController 对象
			ZControllerScanner.scanAndCreateObject(scanPackage);
			// 3.1 创建 @ZController 的拦截器对象
			ZControllerInterceptorScanner.scan();
		}

		// 4 扫描组件的 @ZAutowired 字段 并注入值
		ZAutowiredScanner.inject(scanPackage, ZComponent.class);
		ZAutowiredScanner.inject(scanPackage, ZController.class);
		ZAutowiredScanner.inject(scanPackage, ZAOP.class);

		// 5 扫描组件的 @ZValue 字段 并注入配置文件的值
		ZValueScanner.inject(scanPackage);

		if (StrUtil.isNotEmpty(serverConfiguration.getStaticPath())) {
			System.setProperty(STATIC_RESOURCES_PROPERTY_NAME, serverConfiguration.getStaticPath());
			System.out.println("staticPath = " + serverConfiguration.getStaticPath());
		}

		if (httpEnable) {
			final ZServer zs = new ZServer();
			zs.setName(Z_SERVER_THREAD);
			zs.start();

			ZSessionMap.sessionTimeoutJOB();
		}
	}


	public static void main(final String[] args) {

	}
}
