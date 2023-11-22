package com.vo.core;

import com.google.common.collect.HashBasedTable;

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

	private static final  HashBasedTable<String, Long, Integer> TABLE = HashBasedTable.create();

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

			final Integer v = TABLE.get(keyword, seconds);
			if (v == null) {
				TABLE.put(keyword, seconds, 1);
				return true;
			}

			TABLE.rowMap().remove(keyword);

			final int newCount = v.intValue() + 1;
			TABLE.put(keyword, seconds, newCount);

			return newCount <= qP100MS;
		}
	}

	private static String gKey(final String keyword, final long seconds) {
		return PREFIX + "@" + keyword + "@" + seconds;
	}

}