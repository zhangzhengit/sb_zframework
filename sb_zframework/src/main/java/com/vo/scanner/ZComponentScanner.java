package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import com.vo.anno.ZComponent;
import com.vo.anno.ZComponentMap;
import com.vo.aop.ZAOPScaner;
import com.vo.core.ZClass;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZSingleton;
import com.vo.enums.BeanModeEnum;

import cn.hutool.core.util.ClassUtil;
import reactor.core.publisher.SynchronousSink;

/**
 * 扫描 @ZComponent 的类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZComponentScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreateObject() {

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

			// 2

			final Object newComponent = ZObjectGeneratorStarter.generate(zc);
			final ZClass proxyClass = map.get(newComponent.getClass().getSimpleName());
			if (proxyClass != null) {
				final Object newInstance = proxyClass.newInstance();
//				// 放代理类
				ZComponentMap.put(newComponent.getClass().getCanonicalName(), newInstance);
			} else {
				ZComponentMap.put(newComponent.getClass().getCanonicalName(), newComponent);
			}

			// 1
//			final ZClass proxyClass = map.get(zc.getSimpleName());
//
//			if (proxyClass == null) {
//				try {
//					final Object newInstance = zc.newInstance();
//					ZComponentMap.put(zc.getCanonicalName(), newInstance);
//				} catch (InstantiationException | IllegalAccessException e) {
//					e.printStackTrace();
//				}
//			} else {
//				final Object newInstance = proxyClass.newInstance();
//				// 放代理类
//				ZComponentMap.put(zc.getCanonicalName(), newInstance);
//			}

		}
		LOG.info("给带有[{}]注解的类创建对象完成,个数={}", ZComponent.class.getCanonicalName(), zcSet.size());
	}

	public static Set<Class<?>> scan(final String packageName) {
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(packageName, ZComponent.class);

		System.out.println("ZComponentScanner-zcSet.size = " + zcSet.size());

//		for (final Class<?> cls : zcSet) {
//			final Object newZComponent = newZComponentObject(cls);
//			ZComponentMap.putBean(cls.getCanonicalName(), newZComponent);
//			System.out.println("ZComponentMap.putBean -name = " + cls.getCanonicalName() + "\t" + "object = " + newZComponent);
//		}


		return zcSet;
	}

	public static Object getZComponentInstance(final Class<?> zcClass){
		final ZComponent zc = zcClass.getAnnotation(ZComponent.class);
		final BeanModeEnum modeEnum = zc.modeEnum();

		switch (modeEnum) {
		case SINGLETON:
			final Object singletonByClass = ZSingleton.getSingletonByClass(zcClass);
			return singletonByClass;

		default:
			break;
		}

		return null;
	}
}
