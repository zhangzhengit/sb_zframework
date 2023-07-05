package com.vo.core;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * 存放ZSession信息
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZSessionMap {
// FIXME 2023年7月5日 上午11:43:37 zhanghen: 新增超时job

	private static final ZLog2 LOG = ZLog2.getInstance();

	private final static ScheduledExecutorService TIMEOUT_ZE = Executors.newScheduledThreadPool(1);

	private final static Map<String, ZSession> SESSION_MAP = new ConcurrentHashMap<>(16, 1F);

	public static void sessionTimeoutJOB() {
		LOG.info("session超时任务启动...");

		TIMEOUT_ZE.scheduleAtFixedRate(() -> {

//			LOG.info("session超时任务 SESSION_MAP.size = " + SESSION_MAP.size());
			if (SESSION_MAP.isEmpty()) {
				return;
			}

			final long now = System.currentTimeMillis();
			final Set<Entry<String, ZSession>> es = SESSION_MAP.entrySet();
			for (final Entry<String, ZSession> entry : es) {
				final ZSession session = entry.getValue();
				final int maxInactiveInterval = session.getMaxInactiveInterval();
				final Date createTime = session.getCreateTime();

				if (now - createTime.getTime() >= maxInactiveInterval * 1000) {
					session.invalidate();
//					System.out.println("session 超时 ,id = " + session.getId());
					SESSION_MAP.remove(entry.getKey());
				}
			}

		}, 1, 1, TimeUnit.SECONDS);

	}

	public static void remove(final String zSessionId) {
		SESSION_MAP.remove(zSessionId);
	}

	public static ZSession getByZSessionId(final String zSessionId) {
		return ZSessionMap.SESSION_MAP.get(zSessionId);
	}

	public static void put(final ZSession zSession) {
		ZSessionMap.SESSION_MAP.put(zSession.getId(), zSession);
	}

}
