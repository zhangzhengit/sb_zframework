package com.vo.cache;

import java.util.Set;

/**
 * 缓存接口
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
public interface ZCache<V> {

	public void add(final String key, final V value, long expire);

	public V get(final String key);

	public void remove(final String key);

	public boolean contains(final String key);

	public Set<String> keySet();
}
