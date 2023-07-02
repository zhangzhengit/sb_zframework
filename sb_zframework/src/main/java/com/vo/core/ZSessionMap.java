package com.vo.core;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 存放ZSession信息
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZSessionMap {

	private final static Map<String, ZSession> SESSION_MAP = new HashMap<>(4, 1F);

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
