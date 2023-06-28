package com.vo;

import java.util.Map;
import java.util.Map.Entry;
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
import com.vo.scanner.ZControllerScanner;

/**
 * 启动类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZMain {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final ZFrameworkProperties FRAMEWORK_PROPERTIES = ZFrameworkDatasourcePropertiesLoader
			.getFrameworkPropertiesInstance();
	public static void main(final String[] args) {
		ZMain.LOG.info("zframework开始启动");
		// 0 最先初始化 对象生成器
		ZObjectGeneratorStarter.start();

		// 1 AOP
		// FIXME 2023年6月19日 下午5:27:28 zhanghen: 加入一个类似 BeanPostProcessor 的类，可以用户自定义替换bean
		// 来实现AOP自定义替换bean为生成的子类
		final Map<String, ZClass> proxyZClassMap = ZAOPScaner.scanAndGenerateProxyClass();
		final Set<Entry<String, ZClass>> es = proxyZClassMap.entrySet();
		for (final Entry<String, ZClass> entry : es) {
			final ZClass zclass = entry.getValue();
			final Object ob = zclass.newInstance();
			final Object zAopProxyClass = ZObjectGeneratorStarter.generate(ob.getClass());
			final Class<?> superclass = ob.getClass().getSuperclass();
			final String canonicalName = superclass.getCanonicalName();
			ZComponentMap.putBeanIfAbsent(canonicalName, zAopProxyClass);
		}

		// 2
		scanZCompoentAndCreate();

		// 3
		ZControllerScanner.scanAndCreateObject("com");

		// 4 扫描组件的 @ZAutowired 字段
		ZAutowiredScanner.scanAndCreateObject("com", ZComponent.class);
		ZAutowiredScanner.scanAndCreateObject("com", ZController.class);

		final ZServer zs = new ZServer();
		zs.setName("ZServer-Thread");
		zs.start();

		ZMain.LOG.info("zframework启动完成,serverPort={}",FRAMEWORK_PROPERTIES.getServerPort());
	}


	private static void scanZCompoentAndCreate() {
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
			ZComponentMap.putBeanIfAbsent(zc.getCanonicalName(), in);
		}
		LOG.info("给带有[{}]注解的类创建对象完成,个数={}", ZComponent.class.getCanonicalName(), zcSet.size());
	}

}


