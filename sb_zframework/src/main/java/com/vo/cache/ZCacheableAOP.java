package com.vo.cache;

import java.lang.reflect.Parameter;
import java.util.List;

import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;
import com.vo.core.ZContext;

/**
 * @ZCacheable 的AOP实现类
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZAOP(interceptType = ZCacheable.class)
public class ZCacheableAOP implements ZIAOP {

	public static final String PREFIX = "ZCacheable";

	@Override
	public Object before(final AOPParameter aopParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter aopParameter) {

		final String key = aopParameter.getMethod().getAnnotation(ZCacheable.class).key();

		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key);
		final ZCacheMemory bean = ZContext.getBean(ZCacheMemory.class);
		final ZCacheR vC = (ZCacheR) bean.get(cacheKey);
		if (vC != null && (vC.getExpire() == ZCacheable.NEVER
				|| System.currentTimeMillis() < vC.getExpire() + vC.getCurrentTimeMillis())) {
			return vC.getValue();
		}

		final Object v = aopParameter.invoke();
		final ZCacheR r = new ZCacheR(cacheKey, v, aopParameter.getMethod().getAnnotation(ZCacheable.class).expire(),
				System.currentTimeMillis());
		bean.add(cacheKey, r);
		return v;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

	private static String gKey(final AOPParameter aopParameter, final String key) {
		final Parameter[] ps = aopParameter.getMethod().getParameters();
		for (int i = 0; i < ps.length; i++) {

			final Parameter parameter = ps[i];
			if (parameter.getName().equals(key)) {

				final String canonicalName = aopParameter.getTarget().getClass().getCanonicalName();
				final String name = aopParameter.getMethod().getName();
				final List<Object> pl = aopParameter.getParameterList();
				final String cacheKey = PREFIX + "@" + canonicalName + "@" + name + "@" + parameter.getName() + "="
						+ pl.get(i);
//				System.out.println("cacheKey = " + cacheKey);

				return cacheKey;
			}
		}

		throw new CacheKeyDeclarationException("key不存在,key = " + key + ",方法名称=" + aopParameter.getMethod().getName());
	}

}
