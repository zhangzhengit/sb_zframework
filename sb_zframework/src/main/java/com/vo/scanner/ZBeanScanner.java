package com.vo.scanner;

import java.lang.reflect.Field;
import java.util.Set;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.anno.ZController;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;

/**
 * 扫描 @ZBean 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZBeanScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();


	public static Set<Class<?>> scanAndCreateObject(final String packageName, final Class targetClass) {

		ZBeanScanner.LOG.info("开始扫描带有[{}]注解的类", targetClass.getCanonicalName());
		// XXX 不要每次都扫描
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(packageName, targetClass);
		ZBeanScanner.LOG.info("带有[{}]注解的类个数={}", targetClass.getCanonicalName(), zcSet.size());

		for (final Class<?> cls : zcSet) {
			Object o2 = null;
			if (targetClass.getCanonicalName().equals(ZController.class.getCanonicalName())) {
//				o2 = ZConMap.getByName(cls.getCanonicalName());
				o2 = ZContext.getBean(cls.getCanonicalName());
			}
			if (targetClass.getCanonicalName().equals(ZComponent.class.getCanonicalName())) {
//				o2 = ZComponentMap.getByName(cls.getCanonicalName());
				o2 = ZContext.getBean(cls.getCanonicalName());
			}
			if (o2 == null) {
				throw new IllegalArgumentException("扫描的注解类型不支持:[" + targetClass.getCanonicalName() + "]");
			}

			final Field[] fs = o2.getClass().getDeclaredFields();
			for (final Field f : fs) {}
		}

		return zcSet;
	}


	private static void setFiledValue(final Class targetClass, final Field f, final Object object, final Object value) {
		try {
			f.setAccessible(true);
			f.set(object, value);
			ZBeanScanner.LOG.info("[{}]对象的[{}]字段赋值[{}]完成", targetClass.getCanonicalName(),
					ZAutowired.class.getCanonicalName(),value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}
