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
	static ConcurrentMap<Object, Object> m = Maps.newConcurrentMap();

	public static void put(final Object key, final Object value) {
		m.put(key, value);
	}

	public static Object get(final Object key) {
		return m.get(key);
	}

}
