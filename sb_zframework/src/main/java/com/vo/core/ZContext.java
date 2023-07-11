package com.vo.core;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * 存取 Bean。可使用addBean方法手动注入一个bean让容器管理，使用getBean方法获取一个由容器管理的bean
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
public class ZContext {

	private static final ConcurrentMap<String, Object> BEAN_MAP = Maps.newConcurrentMap();

	@SuppressWarnings("unchecked")
	public static <T> T getBean(final Class<T> beanClass) {
		return (T) getBean(beanClass.getCanonicalName());
	}

	public static Object getBean(final String beanName) {
		return BEAN_MAP.get(beanName);
	}

	public static void addBean(final Class<?> className, final Object bean) {
		addBean(className.getCanonicalName(), bean);
	}

	public static void addBean(final String beanName, final Object bean) {
		BEAN_MAP.put(beanName, bean);
	}

}
