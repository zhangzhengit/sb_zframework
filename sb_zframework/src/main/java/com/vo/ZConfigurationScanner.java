package com.vo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import com.vo.anno.ZBean;
import com.vo.anno.ZComponentMap;
import com.vo.anno.ZConfiguration;
import com.vo.conf.ServerConfiguration;
import com.vo.core.Task;
import com.vo.core.ZLog2;
import com.vo.core.ZSingleton;
import com.vo.scanner.ClassMap;

import cn.hutool.core.collection.CollUtil;

/**
 *	扫描 @ZConfiguration 注解，找到里面的 @ZBean方法，来生成一个配置类
 *
 * @author zhangzhen
 * @date 2023年7月5日
 *
 */
public class ZConfigurationScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreateObject() {
		LOG.info("开始扫描带有@{}注解的类", ZConfiguration.class.getSimpleName());

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		final Set<Class<?>> clsSet = ClassMap.scanPackageByAnnotation(serverConfiguration.getScanPackage(),
				ZConfiguration.class);
		if (CollUtil.isEmpty(clsSet)) {
			LOG.info("没有带有@{}注解的类", ZConfiguration.class.getSimpleName());
			return;
		}

		for (final Class<?> cls : clsSet) {

			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {
				final ZBean bean = method.getAnnotation(ZBean.class);
				if (bean == null) {
					continue;
				}

				if (Task.VOID.equals(method.getReturnType().getCanonicalName())) {
					throw new IllegalArgumentException(
							"@" + ZBean.class.getSimpleName() + " 方法 " + method.getName() + "返回值不能为void");
				}

				if (method.getParameterCount() >= 1) {
					throw new IllegalArgumentException(
							"@" + ZBean.class.getSimpleName() + " 方法 " + method.getName() + " 不允许有参数");
				}

				try {
					LOG.info("找到@{}类[{}]的@{}方法{},开始创建bean", ZConfiguration.class.getSimpleName(), cls.getSimpleName(),
							ZBean.class.getSimpleName(), method.getName());
					final Object newInstance = ZSingleton.getSingletonByClass(cls);
					final Object r = method.invoke(newInstance, null);
					if (r == null) {
						throw new RuntimeException("@" + ZBean.class.getSimpleName() + " 方法 " + method.getName() + " 不能返回null");
					}

					LOG.info("@{}类[{}]的@{}方法{},创建bean完成,bean={}", ZConfiguration.class.getSimpleName(), cls.getSimpleName(),
							ZBean.class.getSimpleName(), method.getName(), r);

					ZComponentMap.put(method.getName(), r);
					ZComponentMap.put(r.getClass().getCanonicalName(), r);

				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}

			}

		}

	}
}
