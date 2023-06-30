package com.vo.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.vo.conf.ServerConfiguration;
import com.vo.enums.MethodEnum;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *	表示http 的请求头
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HRequest {

	public static final String COOKIE = "Cookie";

	public static final String CONTENT_LENGTH = "Content-Length";

	public static final String CONTENT_TYPE = "Content-Type";

	public static final String HOST = "Host";



	// -------------------------------------------------------------------------------------------------
	private List<String> lineList = Lists.newArrayList();

	private String body;

	public void addLine(final String line) {
		this.getLineList().add(line);
	}

	public String getMethod() {
		final String method = Task.parseRequest(this).getMethodEnum().getMethod();
		return method;
	}

	public String getBody() {
		return this.body;
	}

    public String getServerName() {
    	final RequestLine requestLine = this.ppp();
    	final String host = requestLine.getHeaderMap().get(HOST);

		final int i = host.indexOf(":");
		if (i > -1) {
			return host.substring(0, i);
		}

    	return host;
    }

	public int getServerPort() {

		final RequestLine requestLine = this.ppp();
		final String host = requestLine.getHeaderMap().get(HOST);

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
		final RequestLine requestLine = Task.parseRequest(this);
		return requestLine;
	}

	public String getContentType() {
		final RequestLine requestLine = this.ppp();
		final String ct = requestLine.getHeaderMap().get(CONTENT_TYPE);
		return ct;
	}
	public void getSession() {


	}

	public int getContentLength() {
		final RequestLine requestLine = this.ppp();
		final String s = requestLine.getHeaderMap().get(CONTENT_LENGTH);
		return s == null ? -1 : Integer.parseInt(s);
	}

	public ZCookie[] getCookies() {

		final RequestLine requestLine = this.ppp();
		final String cookisString = requestLine.getHeaderMap().get(COOKIE);
		if(StrUtil.isEmpty(cookisString)) {
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
	public static class ZCookie {

		private String name;
		private String value;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ZHeader {

		private String name;
		private String value;
	}


}
