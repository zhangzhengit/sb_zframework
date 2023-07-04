package com.vo.enums;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * http-header Connection 选项
 *
 * @author zhangzhen
 * @date 2023年7月4日
 *
 */
@Getter
@AllArgsConstructor
public enum CollectionEnum {

	KEEP_ALIVE("keep-alive"),

	CLOSE("close"),

	;

	private String value;

	private final static ConcurrentMap<String, CollectionEnum> mapV = Maps.newConcurrentMap();
	static {
		final CollectionEnum[] v = values();
		for (final CollectionEnum e : v) {
			mapV.put(e.getValue(), e);
		}

	}

	public static CollectionEnum valueOfString(final String string) {
		return mapV.get(string);
	}

}
