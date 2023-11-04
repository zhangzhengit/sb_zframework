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

		final ZCacheable annotation = aopParameter.getMethod().getAnnotation(ZCacheable.class);

		final String key = annotation.key();

		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key, annotation.group());
		final ZCacheMemory bean = ZContext.getBean(ZCacheMemory.class);
		final ZCacheR vC = (ZCacheR) bean.get(cacheKey);
		if (vC != null && (vC.getExpire() == ZCacheable.NEVER
				|| System.currentTimeMillis() < vC.getExpire() + vC.getCurrentTimeMillis())) {
			return vC.getValue();
		}

		synchronized (cacheKey.intern()) {

			// 后面排队的线程开始执行后，先判断下缓存内是否已经有结果了（是否前面的线程已经把结果放入了）。
			final ZCacheR vC2 = (ZCacheR) bean.get(cacheKey);
			if (vC2 != null && (vC2.getExpire() == ZCacheable.NEVER
					|| System.currentTimeMillis() < vC2.getExpire() + vC2.getCurrentTimeMillis())) {
				return vC2.getValue();
			}

			final Object v = aopParameter.invoke();
			final ZCacheR r = new ZCacheR(cacheKey, v, annotation.expire(),
					System.currentTimeMillis());
			bean.add(cacheKey, r);
			return v;
		}
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

	public static String gKey(final AOPParameter aopParameter, final String key, final String group) {
		final Parameter[] ps = aopParameter.getMethod().getParameters();
		for (int i = 0; i < ps.length; i++) {

			final Parameter parameter = ps[i];
			if (parameter.getName().equals(key)) {

				final String canonicalName = aopParameter.getTarget().getClass().getCanonicalName();
				final List<Object> pl = aopParameter.getParameterList();
				final String cacheKey = PREFIX + "@" + canonicalName + "@" + group + "@" + parameter.getName() + "="
						+ pl.get(i);
//				System.out.println("cacheKey = " + cacheKey);

				return cacheKey;
			}
		}

		throw new CacheKeyDeclarationException("key不存在,key = " + key + ",方法名称=" + aopParameter.getMethod().getName());
	}

}
