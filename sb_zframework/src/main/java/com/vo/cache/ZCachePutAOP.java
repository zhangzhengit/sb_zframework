package com.vo.cache;

import com.vo.aop.AOPParameter;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;
import com.vo.core.ZContext;

/**
 * @ZCachePut 的实现类
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZAOP(interceptType = ZCachePut.class)
public class ZCachePutAOP implements ZIAOP {

	@Override
	public Object before(final AOPParameter aopParameter) {
		return null;
	}

	@Override
	public Object around(final AOPParameter aopParameter) {
		final ZCachePut annotation = aopParameter.getMethod().getAnnotation(ZCachePut.class);
		final String key = annotation.key();
		final String cacheKey = ZCacheableAOP.gKey(aopParameter, key, annotation.group());

		final Object v = aopParameter.invoke();
		final ZCacheR r = new ZCacheR(cacheKey, v, annotation.expire(), System.currentTimeMillis());
		final ZCacheMemory bean = ZContext.getBean(ZCacheMemory.class);
		bean.add(cacheKey, r);
		return v;
	}

	@Override
	public Object after(final AOPParameter aopParameter) {
		return null;
	}

}
