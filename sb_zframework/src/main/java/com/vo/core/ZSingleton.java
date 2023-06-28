package com.vo.core;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 *
 * 获取/存放 单例对象
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZSingleton {

	private static final ConcurrentMap<String, Object> singletonMap = Maps.newConcurrentMap();

	public static Object getSingletonByClassName(final String clsName) {
		final String key = clsName;
		synchronized (key) {
			final Object v = singletonMap.get(key);
			if (v != null) {
				return v;
			}

			try {
				final Class<?> cls = Class.forName(clsName);
				final Object newInstance = cls.newInstance();
				singletonMap.put(key, newInstance);
				return newInstance;
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public static Object getSingletonByClass(final Class<?> cls) {

		final String key = cls.getCanonicalName();

		synchronized (key) {

			final Object v = singletonMap.get(key);
			if (v != null) {
				return v;
			}
			try {
				final Object newInstance = cls.newInstance();
				singletonMap.put(key, newInstance);
				return newInstance;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}

			return null;
		}
	}
}
