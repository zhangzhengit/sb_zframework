package com.vo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tomcat.jni.Proc;
import org.aspectj.apache.bcel.generic.ReturnaddressType;

import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZComponent;
import com.vo.anno.ZComponentMap;
import com.vo.anno.ZController;
import com.vo.aop.ZAOPProxyClass;
import com.vo.aop.ZAOPScaner;
import com.vo.conf.ZFrameworkDatasourcePropertiesLoader;
import com.vo.conf.ZFrameworkProperties;
import com.vo.core.ZClass;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZServer;
import com.vo.core.ZSingleton;
import com.vo.scanner.ZAutowiredScanner;
import com.vo.scanner.ZComponentScanner;
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

	private static final ZFrameworkProperties FRAMEWORK_PROPERTIES = ZFrameworkDatasourcePropertiesLoader
			.getFrameworkPropertiesInstance();
	public static void main(final String[] args) {
		ZMain.LOG.info("zframework开始启动");
		// 0 最先初始化 对象生成器
		ZObjectGeneratorStarter.start();


		// 2
		// FIXME 2023年6月29日 下午7:28:31 zhanghen: 改为 ZC创建时，如果有代理注解，则直接注入代理类，不用原类了
		scanZComponentAndCreate();

		// 3
		final String scanPackage = ZFrameworkDatasourcePropertiesLoader.getFrameworkPropertiesInstance().getScanPackage();
		ZControllerScanner.scanAndCreateObject(scanPackage);

		// 4 扫描组件的 @ZAutowired 字段
		ZAutowiredScanner.scanAndCreateObject(scanPackage, ZComponent.class);
		ZAutowiredScanner.scanAndCreateObject(scanPackage, ZController.class);

		// 5 // FIXME 2023年6月29日 下午6:58:39 zhanghen: 扫描 @ZValue 注入配置文件的值
		ZValueScanner.scan(scanPackage);

//		// 1 AOP
//		// 来实现AOP自定义替换bean为生成的子类
//		final Map<String, ZClass> proxyZClassMap = ZAOPScaner.scanAndGenerateProxyClass();
//		final Set<Entry<String, ZClass>> es = proxyZClassMap.entrySet();
//		for (final Entry<String, ZClass> entry : es) {
//			final ZClass zclass = entry.getValue();
//			final Object ob = zclass.newInstance();
//			final Object zAopProxyClass = ZObjectGeneratorStarter.generate(ob.getClass());
//			final Class<?> superclass = ob.getClass().getSuperclass();
//			final String canonicalName = superclass.getCanonicalName();
//			// 这里使用put，直接放，覆盖之前的，因为这是放代理类
//			ZComponentMap.put(canonicalName, zAopProxyClass);
//		}

		final ZServer zs = new ZServer();
		zs.setName("ZServer-Thread");
		zs.start();

		ZMain.LOG.info("zframework启动完成,serverPort={}",FRAMEWORK_PROPERTIES.getServerPort());
	}


	private static void scanZComponentAndCreate() {
		final Set<Class<?>> csAllSet = ZAOPScaner.scanPackage_COM();

		final Map<String, ZClass> map = ZAOPScaner.scanAndGenerateProxyClass1();

		final int size = map.size();


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

			// 1 放元类
//			ZComponentMap.put(zc.getCanonicalName(), in);

			final ZClass proxyClass = map.get(zc.getSimpleName());

			final Object newInstance = proxyClass.newInstance();
			// 2 放代理类
			ZComponentMap.put(zc.getCanonicalName(), newInstance);

//			final HashBasedTable<Class, Method, Class<?>> table = ZAOPScaner.extractedC(Sets.newHashSet(csAllSet));
//			final int size = table.size();
		}
		LOG.info("给带有[{}]注解的类创建对象完成,个数={}", ZComponent.class.getCanonicalName(), zcSet.size());
	}

	/**
	 *	查找一个类，里面的方法的注解，是否有AOP类
	 * @param class1
	 *
	 */
	public static void findClassAOPClass(final Class class1) {
		final Method[] ms = class1.getDeclaredMethods();
		for (final Method method : ms) {
			final Annotation[] as = method.getAnnotations();
			for (final Annotation a : as) {

			}

		}

	}

}


