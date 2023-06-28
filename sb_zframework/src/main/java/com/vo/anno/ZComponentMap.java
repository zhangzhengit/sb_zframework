package com.vo.anno;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZComponentMap {

	private static ConcurrentMap<String, Object> zcMap = Maps.newConcurrentMap();

	public synchronized static void putBeanIfAbsent(final String name, final Object zComponentObject) {
		if (zcMap.containsKey(name)) {
			return;
		}
		zcMap.put(name, zComponentObject);
	}

	public static Object getByName(final String name) {
		final Object v = zcMap.get(name);
		return v;
	}
}
