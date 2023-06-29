package com.vo.core;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.vo.aop.ZAOP;
import com.vo.aop.ZAOPProxyClass;
import com.vo.aop.ZAOPScaner;

/**
 *
 * 默认的生成器类
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
public class ZDefaultObjectGenerator implements ZObjectGenerator {

	@Override
	public Object generate(final Class clsName) {

		final boolean isZAOPProxyClass = clsName.isAnnotationPresent(ZAOPProxyClass.class);
		if (isZAOPProxyClass) {

			final Map<String, ZClass> zcMap = ZAOPScaner.getZCMap();
			final Collection<ZClass> values = zcMap.values();
			for (final ZClass zc : values) {
				final Object newInstance = zc.newInstance();
				final String canonicalName = newInstance.getClass().getCanonicalName();

				if (canonicalName.equals(clsName.getCanonicalName())) {
					System.out.println("匹配一个ZAOpProxyClass,name = " + canonicalName);
					return newInstance;
				}
			}

			final int n2 = 20;
		}

		return ZSingleton.getSingletonByClass(clsName);
	}

}
