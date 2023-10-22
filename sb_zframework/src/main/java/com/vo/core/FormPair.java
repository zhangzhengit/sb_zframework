package com.vo.core;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HeaderEnum.URLENCODED 中的k-v对
 *
 * @author zhangzhen
 * @date 2023年8月11日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormPair {

	private String key;

	private String value;

	/**
	 * 从一个请求体 解析出 K-V对
	 *
	 * @param body 请求体，如： id=200&name=zhangsan
	 * @return
	 *
	 */
	public static List<FormPair> parse(final String body) {
		if (StrUtil.isEmpty(body)) {
			throw new FormPairParseException("body 不能为空");
		}

		final String[] fa = body.split("&");
		if (ArrayUtil.isEmpty(fa)) {
			throw new FormPairParseException("body 必须含有&");
		}

		final ArrayList<FormPair> l = Lists.newArrayList();
		for (final String f : fa) {
			final String[] f2 = f.split("=");
			if (ArrayUtil.isEmpty(f2)) {
				throw new FormPairParseException("k-v对缺失, value = " + f);
			}

			if (f2.length != 2) {
				throw new FormPairParseException("k-v对格式错误, value = " + f);
			}

			l.add(new FormPair(f2[0], f2[1]));
		}

		return l;
	}

//	public static void main(final String[] args) {
////		final String body = "sd";
//		final String body = "id=200&name=zhangsan";
//		final List<FormPair> parse = parse(body);
//		System.out.println("parse = \n");
//		System.out.println(parse);
//	}
}
