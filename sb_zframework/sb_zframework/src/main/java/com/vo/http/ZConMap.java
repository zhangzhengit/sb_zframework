package com.vo.http;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZConMap {

	private static ConcurrentMap<String, Object> zcMap = Maps.newConcurrentMap();

	public synchronized static void putBean(final String name, final Object zControllerObject) {
		zcMap.put(name, zControllerObject);
	}

	public static Object getByName(final String name) {
		final Object v = zcMap.get(name);
		return v;
	}

	public static void putAllClass() {

	}

}
