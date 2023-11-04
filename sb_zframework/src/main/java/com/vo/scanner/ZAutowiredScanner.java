package com.vo.scanner;

import java.lang.reflect.Field;
import java.util.Set;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZComponent;
import com.vo.anno.ZController;
import com.vo.aop.ZAOP;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;

import cn.hutool.core.util.StrUtil;

/**
 * 扫描 @ZController 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZAutowiredScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();


	public static Set<Class<?>> inject(final Class targetClass, final String... packageName) {

		ZAutowiredScanner.LOG.info("开始扫描带有[{}]注解的类", targetClass.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(targetClass, packageName);

		ZAutowiredScanner.LOG.info("带有[{}]注解的类个数={}", targetClass.getCanonicalName(), zcSet.size());

		for (final Class<?> cls : zcSet) {
			Object o2 = null;
			if (targetClass.getCanonicalName().equals(ZController.class.getCanonicalName())) {
				o2 = ZContext.getBean(cls.getCanonicalName());
			}
			if (targetClass.getCanonicalName().equals(ZComponent.class.getCanonicalName())) {
				o2 = ZContext.getBean(cls.getCanonicalName());
			}
			if (targetClass.getCanonicalName().equals(ZAOP.class.getCanonicalName())) {
				o2 = ZSingleton.getSingletonByClass(cls);
			}

			if (o2 == null) {
				LOG.warn("无[{}]的对象,continue", cls.getCanonicalName());
				continue;
			}

			final Field[] fs = o2.getClass().getDeclaredFields();
			for (final Field f : fs) {
				inject(cls, f);

			}
		}

		return zcSet;
	}


	public static String inject(final Class<?> cls, final Field f) {
		final ZAutowired autowired = f.getAnnotation(ZAutowired.class);
		if (autowired == null) {
			return null;
		}

		ZAutowiredScanner.LOG.info("找到[{}]对象的[{}]字段={}", cls.getCanonicalName(),
				ZAutowired.class.getCanonicalName(), f.getType().getCanonicalName());

		final String name = StrUtil.isEmpty(autowired.name()) ? f.getType().getCanonicalName() : autowired.name();

		// FIXME 2023年7月5日 下午8:02:09 zhanghen: TODO ： 如果getByName 有多个返回值，则提示一下要具体注入哪个
		final Object object = cls.isAnnotationPresent(ZAOP.class)
					? ZSingleton.getSingletonByClass(cls)
					: ZContext.getBean(cls.getCanonicalName());
		final Object vT = ZContext.getBean(name);
		final Object value = vT != null ? vT : ZContext.getBean(f.getType().getCanonicalName());

		try {
			f.setAccessible(true);
			final Object fOldV = f.get(object);
			System.out.println("对象 " + object + " 的字段f = " + f.getName() + " 赋值前，值 = " + fOldV);
			ZAutowiredScanner.setFiledValue(f, object, value);
			final Object fNewV = f.get(object);
			System.out.println("对象 " + object + " 的字段f = " + f.getName() + " 赋值后，值 = " + fNewV);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return name;
	}


	private static void setFiledValue(final Field f, final Object object, final Object value) {
		try {
			f.setAccessible(true);
			f.set(object, value);
			ZAutowiredScanner.LOG.info("对象的[{}]字段赋值[{}]完成",
					ZAutowired.class.getCanonicalName(),value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}
