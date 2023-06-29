package com.vo;

import java.util.Map;
import java.util.Set;

import com.vo.anno.ZComponent;
import com.vo.anno.ZComponentMap;
import com.vo.anno.ZController;
import com.vo.aop.ZAOPScaner;
import com.vo.conf.ZFrameworkDatasourcePropertiesLoader;
import com.vo.conf.ZFrameworkProperties;
import com.vo.core.ZClass;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZServer;
import com.vo.scanner.ZAutowiredScanner;
import com.vo.scanner.ZComponentScanner;
import com.vo.scanner.ZConfigurationPropertiesScanner;
import com.vo.scanner.ZControllerScanner;
import com.vo.scanner.ZValueScanner;

/**
 * 启动类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZMain {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final String Z_SERVER_THREAD = "ZServer-Thread";

	private static final ZFrameworkProperties FRAMEWORK_PROPERTIES = ZFrameworkDatasourcePropertiesLoader
			.getFrameworkPropertiesInstance();
	public static void main(final String[] args) {
		ZMain.LOG.info("zframework开始启动");
		// 0 最先初始化 对象生成器
		ZObjectGeneratorStarter.start();

		// 1 读取配置，创建配置类
		ZConfigurationPropertiesScanner.scanAndCreateObject();

		// 2 创建 @ZComponent 对象，如果类中有被代理的自定义注解，则创建此类的代理类
		scanZComponentAndCreate();

		// 3 创建 @ZController 对象
		final String scanPackage = ZFrameworkDatasourcePropertiesLoader.getFrameworkPropertiesInstance().getScanPackage();
		ZControllerScanner.scanAndCreateObject(scanPackage);

		// 4 扫描组件的 @ZAutowired 字段 并注入值
		ZAutowiredScanner.scanAndCreateObject(scanPackage, ZComponent.class);
		ZAutowiredScanner.scanAndCreateObject(scanPackage, ZController.class);

		// 5 扫描组件的 @ZValue 字段 并注入配置文件的值
		ZValueScanner.scan(scanPackage);

		final ZServer zs = new ZServer();
		zs.setName(Z_SERVER_THREAD);
		zs.start();

		ZMain.LOG.info("zframework启动完成,serverPort={}",FRAMEWORK_PROPERTIES.getServerPort());
	}


	private static void scanZComponentAndCreate() {

		final Map<String, ZClass> map = ZAOPScaner.scanAndGenerateProxyClass1();

		// 扫描
		LOG.info("开始扫描带有[{}]注解的类", ZComponent.class.getCanonicalName());
		final Set<Class<?>> zcSet = ZComponentScanner.scan("com");
		LOG.info("扫描到带有[{}]注解的类个数={}", ZComponent.class.getCanonicalName(),zcSet.size());
		LOG.info("开始给带有[{}]注解的类创建对象",ZComponent.class.getCanonicalName());
		for (final Class<?> zc : zcSet) {
			LOG.info("开始给待有[{}]注解的类[{}]创建对象",ZComponent.class.getCanonicalName(),zc.getCanonicalName());
			final Object in = ZComponentScanner.getZComponentInstance(zc);
			LOG.info("给带有[{}]注解的类[{}]创建对象[{}]完成", ZComponent.class.getCanonicalName(),
					zc.getCanonicalName(), in);

			final ZClass proxyClass = map.get(zc.getSimpleName());

			final Object newInstance = proxyClass.newInstance();

			// 放代理类
			ZComponentMap.put(zc.getCanonicalName(), newInstance);
		}
		LOG.info("给带有[{}]注解的类创建对象完成,个数={}", ZComponent.class.getCanonicalName(), zcSet.size());
	}

}
