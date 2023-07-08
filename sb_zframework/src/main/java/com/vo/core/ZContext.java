package com.vo.core;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * 存取 Bean
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
public class ZContext {

	private static final ConcurrentMap<String, Object> MAP = Maps.newConcurrentMap();

	public static void addBean(final Class clsName, final Object bean) {
//		addBean(clsName.getCanonicalName(), bean);
	}

	public static void addBean(final String beanName, final Object bean) {
		MAP.put(beanName, bean);
	}

	public static Object getBean(final String beanName) {
		return MAP.get(beanName);
	}
}
