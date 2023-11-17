package com.vo.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * QPS计数器
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
public class QPSCounter {

	/**
	 * 服务器允许的QPS最小值
	 */
	public static final int ZSERVER_QPS_MIN_VALUE = 100;

	public static int getQpsMin() {
		return ZSERVER_QPS_MIN_VALUE;
	}

	public static final String PREFIX = "QPS";
	private static final Map<String, Long> MAP = new ConcurrentHashMap<>(16, 1F);

	/**
	 * 返回指定的K在的QPS是否超过了
	 *
	 * @param keyword
	 * @param qps
	 * @return
	 *
	 */
	public static boolean allow(final String keyword, final long qps) {

		if (qps <= 0) {
			return false;
		}

		final long qP100MS = qps / ZSERVER_QPS_MIN_VALUE;

		final long seconds = System.currentTimeMillis() / (1000 / (ZSERVER_QPS_MIN_VALUE));

		final String key = gKey(keyword, seconds);

		synchronized (key.intern()) {

			final Long v = QPSCounter.MAP.get(key);
			if (v == null) {
				QPSCounter.MAP.put(key, 1L);
				return true;
			}

			final List<String> removeKeyList = MAP.keySet().stream().filter(k -> k.startsWith(keyword))
					.collect(Collectors.toList());

			for (final String k : removeKeyList) {
				MAP.remove(k);
			}

			final long newCount = v + 1L;
			QPSCounter.MAP.put(key, newCount);

			return newCount <= qP100MS;
		}
	}

	private static String gKey(final String keyword, final long seconds) {
		return PREFIX + "@" + keyword + "@" + seconds;
	}

}