package com.vo.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.vo.anno.ZComponent;

/**
 * 内存实现的缓存
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZComponent
public class ZCacheMemory implements ZCache {

	// FIXME 2023年11月4日 下午9:53:44 zhanghen: 新增配置类，可以配置 weakhashmap、容量、过期时间、key前缀等等
	private final ConcurrentMap<String, Object> map = Maps.newConcurrentMap();

	@Override
	public synchronized void add(final String key, final Object value,final long expire) {
		this.map.put(key, value);
	}

	@Override
	public synchronized Object get(final String key) {
		return this.map.get(key);
	}

	@Override
	public synchronized void remove(final String key) {
		this.map.remove(key);
	}

	@Override
	public synchronized boolean contains(final String key) {
		return this.map.containsKey(key);
	}

	@Override
	public synchronized Set<String> keySet() {
		return this.map.keySet();
	}

}
