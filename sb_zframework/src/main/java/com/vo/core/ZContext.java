package com.vo.core;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
public class ZContext {

	private static final ThreadLocal<ZResponse> RESPONSE = new ThreadLocal<>();

	private static final ThreadLocal<ZRequest> REQUEST = new ThreadLocal<>();

	public static void setZResponse(final ZResponse response) {
		ZContext.RESPONSE.set(response);
	}

	public static void setZRequest(final ZRequest request) {
		ZContext.REQUEST.set(request);
	}

	public static ZRequest getZRequest() {
		return ZContext.REQUEST.get();
	}

	public static ZResponse getZResponseAndRemove() {
		final ZResponse v = ZContext.RESPONSE.get();
		ZContext.RESPONSE.remove();
		return v;
	}

}
