package com.vo.core;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.vo.enums.MethodEnum;
import com.vo.http.ZCookie;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示http 的请求信息
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZRequest {

	public static final String Z_SESSION_ID = "ZSESSIONID";

	public static final String GZIP = "gzip";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";

	public static final String COOKIE = "Cookie";

	public static final String ALLOW = "Allow";
	public static final String CONTENT_LENGTH = "Content-Length";

	public static final String CONTENT_TYPE = "Content-Type";

	public static final String HOST = "Host";

	private static final AtomicLong GZSESSIONID = new AtomicLong(1L);


	// -------------------------------------------------------------------------------------------------
	private List<String> lineList = Lists.newArrayList();

	private String body;
	private RequestLine requestLine;

	public void addLine(final String line) {
		this.getLineList().add(line);
	}

	public boolean isSupportGZIP() {
		final String a = this.getHeader(ZRequest.ACCEPT_ENCODING);
		if (StrUtil.isEmpty(a)) {
			return false;
		}

		final String[] array = a.split(",");
		for (final String a2 : array) {
			if (ZRequest.GZIP.equalsIgnoreCase(a2)) {
				return true;
			}
		}

		return false;
	}

	public String getMethod() {
		final RequestLine ppp = this.ppp();
		return ppp.getMethodEnum().getMethod();
	}

	public String getBody() {
		return this.body;
	}

    public String getServerName() {
    	final String host = this.ppp().getHeaderMap().get(ZRequest.HOST);

		final int i = host.indexOf(":");
		if (i > -1) {
			return host.substring(0, i);
		}

    	return host;
    }

	public int getServerPort() {

		final RequestLine requestLine = this.ppp();
		final String host = requestLine.getHeaderMap().get(ZRequest.HOST);

		final int i = host.indexOf(":");
		if (i > -1) {
			final String port = host.substring(i + 1);
			return Integer.parseInt(port);
		}

		return ZServer.DEFAULT_HTTP_PORT;
	}

	public String getRequestURL() {
		final RequestLine requestLine = this.ppp();
		final String serverName = this.getServerName();
		return serverName + requestLine.getPath();
	}

	public String getRequestURI() {
		final RequestLine requestLine = this.ppp();
		final String path = requestLine.getPath();
		return path;
	}

	public String getQueryString() {
		final RequestLine requestLine = this.ppp();
		return requestLine.getQueryString();
	}

	private RequestLine ppp() {
		final RequestLine requestLine = Task.parseRequest(this).getRequestLine();
		return requestLine;
	}

	public String getContentType() {
		final RequestLine requestLine = this.ppp();
		final String ct = requestLine.getHeaderMap().get(ZRequest.CONTENT_TYPE);
		return ct;
	}


	private static String gSessionID() {
		final Hasher putString = Hashing.sha256()
				.newHasher()
				.putString(ZRequest.Z_SESSION_ID + System.currentTimeMillis() + ZRequest.GZSESSIONID.getAndDecrement(),
				Charset.defaultCharset());

		final HashCode hash = putString.hash();
		return hash.toString();
	}

	public ZSession getSession() {
		return this.getSession(true);
	}

	/**
	 * 获取Session，如需写入到Cookie，需要自己处理 ZResponse.cookie.write................
	 *
	 * @param create
	 * @return
	 */
	public synchronized ZSession getSession(final boolean create) {
		final ZCookie[] cs = this.getCookies();

		if (ArrayUtil.isNotEmpty(cs)) {
			for (final ZCookie zc : cs) {
				if (ZRequest.Z_SESSION_ID.equals(zc.getName())) {
					final ZSession session = ZSessionMap.getByZSessionId(zc.getValue());

					if (session != null) {
						return session;
					}

					// session == null 可能是服务器重启了等
					if (!create) {
						return null;
					}

					final ZSession newSession = ZRequest.newSession();
					ZSessionMap.put(newSession);

					return newSession;
				}
			}
		}

		if (!create) {
			return null;
		}

		final ZSession session = ZRequest.newSession();
		ZSessionMap.put(session);
		return session;
	}

	private static ZSession newSession() {
		final String zSessionID = ZRequest.gSessionID();
		final ZSession session = new ZSession(zSessionID, new Date());

		ZSessionMap.put(session);

		return session;
	}

	public int getContentLength() {
		final RequestLine requestLine = this.ppp();
		final String s = requestLine.getHeaderMap().get(ZRequest.CONTENT_LENGTH);
		return s == null ? -1 : Integer.parseInt(s);
	}

	public ZCookie[] getCookies() {

		final RequestLine requestLine = this.ppp();
		final String cookisString = requestLine.getHeaderMap().get(ZRequest.COOKIE);
		if (StrUtil.isEmpty(cookisString)) {
			return null;
		}

		final String[] a = cookisString.split(";");
		final ZCookie[] c = new ZCookie[a.length];
		int cI = 0;
		for (final String s : a) {
			final String[] c1 = s.split("=");
			final ZCookie zCookie = new ZCookie(c1[0].trim(),c1[1].trim());

			c[cI++] = zCookie;
		}

		return c;
	}

	public ZCookie getZSESSIONID() {
		final ZCookie[] cookies = this.getCookies();
		if (ArrayUtil.isEmpty(cookies)) {
			return null;
		}

		for (final ZCookie zCookie : cookies) {
			if(ZRequest.Z_SESSION_ID.equals(zCookie.getName())) {
				return zCookie;
			}
		}

		return null;
	}

	public String getHeader(final String name) {
		final RequestLine requestLine = this.ppp();
		final String header = requestLine.getHeaderMap().get(name);
		return header;
	}

	public Object getParameter(final String name) {
		final RequestLine requestLine = this.ppp();
		final Set<RequestParam> ps = requestLine.getParamSet();
		if (CollUtil.isEmpty(ps)) {
			return null;
		}

		for (final RequestParam requestParam : ps) {
			if (requestParam.getName().equals(name)) {
				return requestParam.getValue();
			}
		}

		return null;
	}

// -------------------------------------------------------------------------------------------------
	/**
	 *  请求头的 请求行
	 *  如： GET / HTTP/1.1
	 *
	 * @author zhangzhen
	 * @date 2023年6月12日
	 *
	 */
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class RequestLine {

		String original;

		String queryString;

		/**
		 * 请求方法
		 */
		MethodEnum methodEnum;

		/**
		 * 完整的path，包含参数的,如：/hello?name=z&age=20
		 */
		String fullpath;
		/**
		 * 简单的path，不含参数，如：/hello
		 */
		String path;

		Set<RequestParam> paramSet;

		/**
		 * http版本
		 */
		String version;

		/**
		 * 请求头,如： Accept-Encoding: gzip, deflate
		 */
		Map<String, String> headerMap;

	}
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class RequestParam {

		private String name;
		private Object value;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ZHeader {

		private String name;
		private String value;
	}

}
