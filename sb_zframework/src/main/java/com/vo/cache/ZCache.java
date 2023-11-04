package com.vo.cache;

import java.util.Set;

/**
 * 缓存接口
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
public interface ZCache {

	public void add(final String key, final Object value);

	public Object get(final String key);

	public void remove(final String key);
	
	public void contains(final String key);

	public Set<String> keySet();
}
