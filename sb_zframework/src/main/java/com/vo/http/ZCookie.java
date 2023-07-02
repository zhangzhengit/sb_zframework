package com.vo.http;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cookie
 *
 * @author zhangzhen
 * @date 2023年7月2日
 *
 */
@Data
@NoArgsConstructor
public class ZCookie {

	private final List<Node> nodeList = new ArrayList<>(8);
	// FIXME 2023年7月2日 上午1:37:50 zhanghen:ZCookie 加入更多属性：如下
//	Set-Cookie: sessionId=abc123; Expires=Sat, 01 Jan 2022 00:00:00 GMT; Max-Age=3600; Domain=example.com; Path=/; Secure; HttpOnly; SameSite=Strict
//	在上述示例中，Cookie响应头包含了以下属性：
//
//	sessionId=abc123：Cookie的名称为sessionId，值为abc123。
//	Expires=Sat, 01 Jan 2022 00:00:00 GMT：Cookie的过期时间为2022年1月1日。
//	Max-Age=3600：Cookie的最大存活时间为3600秒（1小时）。
//	Domain=example.com：Cookie适用于example.com域名及其子域名。
//	Path=/：Cookie适用于网站的根路径。
//	Secure：Cookie只能通过HTTPS协议传输。
//	HttpOnly：Cookie只能通过HTTP协议访问，而无法通过JavaScript等客户端脚本访问。
//	SameSite=Strict：Cookie只能在同一站点上进行请求，不会在跨站点请求中发送。

	private String name;
	private String value;

	public ZCookie sameSiteEnum(final SameSiteEnum sameSiteEnum) {
		this.nodeList.add(new Node("SameSite", sameSiteEnum.getValue()));
		return this;
	}

	public ZCookie httpOnly(final Boolean httpOnly) {
		if (Boolean.TRUE.equals(httpOnly)) {
			this.nodeList.add(new Node("HttpOnly", null));
		}
		return this;
	}

	public ZCookie secure(final Boolean secure) {
		if (Boolean.TRUE.equals(secure)) {
			this.nodeList.add(new Node("Secure", null));
		}
		return this;
	}

	public ZCookie path(final String path) {
		this.nodeList.add(new Node("Path", path));
		return this;
	}

	public ZCookie domain(final String domain) {
		this.nodeList.add(new Node("Domain", domain));
		return this;
	}

	public ZCookie maxAge(final Long maxAge) {
		this.nodeList.add(new Node("Max-Age", maxAge));
		return this;
	}

	public ZCookie expires(final Date date) {
		this.nodeList.add(new Node("Expires", date));
		return this;
	}

	public ZCookie(final String name, final String value) {
		this.name = name;
		this.value = value;
	}

	public String toCookieString() {
//		Set-Cookie: sessionId=abc123; Expires=Sat, 01 Jan 2022 00:00:00 GMT; Max-Age=3600;
//		Domain=example.com; Path=/; Secure; HttpOnly; SameSite=Strict

		final StringJoiner joiner = new StringJoiner("");
		// 不取name
		joiner.add(this.getValue()).add(";");
		for (final Node node : this.nodeList) {
			joiner.add(node.getName());
			if (node.getValue() == null) {
				joiner.add(";");
			} else {
				joiner.add("=").add(String.valueOf(node.getValue())).add(";");
			}
		}

		return joiner.toString();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Node{
		private String name;
		private Object value;
	}



}