package com.vo.cache;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZConfiguration;
import com.vo.validator.TypeNotSupportedExcpetion;

/**
 *	Cache配置
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
@ZConfiguration
public class ZCacheConfiguration {

	public static final String MEMORY = "MEMORY";

	public static final String REDIS = "REDIS";

	public static final String DEFAULT = MEMORY;

	@ZAutowired
	private ZCacheConfigurationProperties cacheConfigurationProperties;

	@ZBean
	public ZCache<ZCacheR> cacheBbuiltinForPackageCache() {

		final String type = this.cacheConfigurationProperties.getType();
		switch (type) {
		case MEMORY:
			return new ZCacheMemory();

		case REDIS:

			return new ZCacheRedis();

		default:
			throw new TypeNotSupportedExcpetion(
					"cache.type不支持，type=" + type + ",支持类型为：" + MEMORY + "和" + REDIS + ",默认值为 cache.type=" + DEFAULT);

		}
	}

}
