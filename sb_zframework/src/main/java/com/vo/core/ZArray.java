package com.vo.core;

import java.util.concurrent.atomic.AtomicReference;

import cn.hutool.core.util.ArrayUtil;

/**
 *	动态数组
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class ZArray {

	private final AtomicReference<byte[]> ar = new AtomicReference<>(new byte[] {});

	public byte[] add(final byte[] ba) {
		this.ar.set(ArrayUtil.addAll(this.ar.get(), ba));

		return this.ar.get();
	}
}
