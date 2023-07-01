package com.vo.http;

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
@AllArgsConstructor
@NoArgsConstructor
public class ZCookie {
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
}