package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

import com.vo.anno.ZComponent;
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

	public static Set<Class<?>> scan(final String packageName) {
		final Set<Class<?>> zcSet = ClassUtil.scanPackageByAnnotation(packageName, ZComponent.class);

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
