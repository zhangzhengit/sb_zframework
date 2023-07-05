package com.vo.scanner;

import java.lang.reflect.Field;
import java.util.Set;

import javax.validation.constraints.Null;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.anno.ZComponentMap;
import com.vo.anno.ZController;
import com.vo.core.ZLog2;
import com.vo.http.ZConMap;
import com.vo.http.ZConfigurationPropertiesMap;

import cn.hutool.core.util.ClassUtil;

/**
 * 扫描 @ZController 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZAutowiredScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();


	public static Set<Class<?>> scanAndCreateObject(final String packageName, final Class targetClass) {


		ZAutowiredScanner.LOG.info("开始扫描带有[{}]注解的类", targetClass.getCanonicalName());
		// XXX 不要每次都扫描
		final Set<Class<?>> zcSet = ClassUtil.scanPackageByAnnotation(packageName, targetClass);
		ZAutowiredScanner.LOG.info("带有[{}]注解的类个数={}", targetClass.getCanonicalName(), zcSet.size());

		for (final Class<?> cls : zcSet) {
			Object o2 = null;
			if (targetClass.getCanonicalName().equals(ZController.class.getCanonicalName())) {
				o2 = ZConMap.getByName(cls.getCanonicalName());
			}
			if (targetClass.getCanonicalName().equals(ZComponent.class.getCanonicalName())) {
				o2 = ZComponentMap.getByName(cls.getCanonicalName());
			}

			if (o2 == null) {
//				throw new IllegalArgumentException("扫描的注解类型不支持:[" + targetClass.getCanonicalName() + "]");
				LOG.warn("无[{}]的对象,continue", cls.getCanonicalName());
				continue;
			}

			final Field[] fs = o2.getClass().getDeclaredFields();
			for (final Field f : fs) {
				final boolean isFiedlZAutowired = f.isAnnotationPresent(ZAutowired.class);
				if (isFiedlZAutowired) {
					ZAutowiredScanner.LOG.info("找到[{}]对象的[{}]字段={}", cls.getCanonicalName(), ZAutowired.class.getCanonicalName(),
							f.getType().getCanonicalName());

					if (targetClass.getCanonicalName().equals(ZComponent.class.getCanonicalName())) {
						final Object object = ZComponentMap.getByName(cls.getCanonicalName());
						final Object value = ZComponentMap.getByName(f.getType().getCanonicalName());
						ZAutowiredScanner.setFiledValue(targetClass, f, object, value);
						continue;
					}

					if (targetClass.getCanonicalName().equals(ZController.class.getCanonicalName())) {
						final Object object = ZConMap.getByName(cls.getCanonicalName());
						final Object vT = ZComponentMap.getByName(f.getType().getCanonicalName());
						final Object value = vT != null ? vT : ZConfigurationPropertiesMap.get(f.getType());

						try {
							f.setAccessible(true);
							final Object fOldV = f.get(object);
							System.out.println("对象 " + object + " 的字段f = " + f.getName() + " 赋值前，值 = " + fOldV);
							ZAutowiredScanner.setFiledValue(targetClass, f, object, value);
							final Object fNewV = f.get(object);
							System.out.println("对象 " + object + " 的字段f = " + f.getName() + " 赋值后，值 = " + fNewV);
						} catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}

						final int x = 0;
						continue;
					}



				}
			}
		}

		return zcSet;
	}


	private static void setFiledValue(final Class targetClass, final Field f, final Object object, final Object value) {
		try {
			f.setAccessible(true);
			f.set(object, value);
			ZAutowiredScanner.LOG.info("[{}]对象的[{}]字段赋值[{}]完成", targetClass.getCanonicalName(),
					ZAutowired.class.getCanonicalName(),value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}
