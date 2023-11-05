package com.vo.cache;

import com.vo.anno.ZAutowired;
import com.vo.anno.ZBean;
import com.vo.anno.ZConfiguration;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * 配置一个Bean给RedisCache用
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
@ZConfiguration
public class ZRedisConfiguration {

	private static final ZLog2 LOG = ZLog2.getInstance();

	@ZAutowired
	private ZRedisConfigurationProperties redisConfigurationProperties;

	@ZBean
	public JedisPool jedisPool() {
		LOG.info("开始初始化jedisPool,host={},port={}",
		this.redisConfigurationProperties.getHost(),
			   this.redisConfigurationProperties.getPort());

		final JedisPoolConfig poolConfig = new JedisPoolConfig();
		// 设置最大连接数
		poolConfig.setMaxTotal(this.redisConfigurationProperties.getMaxTotal());
		// 设置最大空闲连接数
		poolConfig.setMaxIdle(this.redisConfigurationProperties.getMaxIdle());

		final JedisPool jedisPool = new JedisPool(poolConfig,
				this.redisConfigurationProperties.getHost(),
				this.redisConfigurationProperties.getPort(),
				this.redisConfigurationProperties.getTimeout(),
				this.redisConfigurationProperties.getPassword());

		ZContext.addBean(jedisPool.getClass(), jedisPool);
		return jedisPool;
	}

}
