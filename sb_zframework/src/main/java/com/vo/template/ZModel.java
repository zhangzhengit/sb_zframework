package com.vo.template;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 用于设置值，传到html页面中，使用${key}来取值
 *
 * @author zhangzhen
 * @date 2023年6月27日
 *
 */
public class ZModel {

	private static final ThreadLocal<Map<String, Object>> tl = new ThreadLocal<>();

	private final Map<String, Object> map = Maps.newHashMap();

	public void set(final String name, final Object value) {
		this.map.put(name, value);
		ZModel.tl.set(this.map);
	}

	public static Map<String, Object> get() {
		return tl.get();
	}

	public Object get(final String name) {
		return ZModel.tl.get().get(name);
	}

}
