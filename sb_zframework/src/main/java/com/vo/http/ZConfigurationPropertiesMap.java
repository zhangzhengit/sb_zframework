package com.vo.http;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * 存放 @ZConfigurationProperties 的对象
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
public class ZConfigurationPropertiesMap {
	static ConcurrentMap<Class, Object> m = Maps.newConcurrentMap();

	public static void put(final Class<?> cls, final Object object) {
		m.put(cls, object);
	}

	public static Object get(final Class<?> cls) {
		return m.get(cls);
	}

}
