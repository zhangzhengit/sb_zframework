package com.vo.core;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class BeanMap {

	static ConcurrentMap<String, Object> beanMap = Maps.newConcurrentMap();

	public static void putBean(final String beanName, final Object bean) {
		beanMap.put(beanName, bean);
	}



}
