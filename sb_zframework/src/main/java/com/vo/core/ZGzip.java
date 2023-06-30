package com.vo.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cn.hutool.core.util.ArrayUtil;

/**
 *
 * gzip 压缩
 *
 * @author zhangzhen
 * @date 2023年7月1日
 *
 */
public class ZGzip {

	private static final String DEFAULT_CHARSET = Charset.defaultCharset().displayName();

	public static void main(final String[] args) throws UnsupportedEncodingException {
		test_1();
	}

	public static void test_1() throws UnsupportedEncodingException {
		System.out.println(
				java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t" + "ZGzip.compress()");

		final String string = "<!DOCTYPE html>\r\n"
				+ "<html>\r\n"
				+ "<head>\r\n"
				+ "<link rel=\"stylesheet\" type=\"text/css\" href=\"/css/index.css\">\r\n"
				+ "<meta charset=\"UTF-8\">\r\n"
				+ "<title>sb_zframework_TEST-OK</title>\r\n"
				+ "</head>\r\n"
				+ "\r\n"
				+ "\r\n"
				+ "<body>\r\n"
				+ "\r\n"
				+ "	<h1>OK-ok-TIME=ZH</h1>\r\n"
				+ "	\r\n"
				+ "	<img src=\"/image/1.jpg\"/>\r\n"
				+ "	<h2>这是h2</h2>\r\n"
				+ "	<h2>status:\r\n"
				+ "		<@switch[statusAAA]>\r\n"
				+ "			<case 1> 	状态111\r\n"
				+ "			<case 2> 	状态222\r\n"
				+ "			<case 3> 	状态333\r\n"
				+ "		</endswitch[statusAAA]>\r\n"
				+ "	</h2>\r\n"
				+ "	<h3>\r\n"
				+ "		if: \r\n"
				+ "		<@switch[i1]>\r\n"
				+ "			<case 1> 	一\r\n"
				+ "			<case 2> 	二\r\n"
				+ "			<case 3> 	三\r\n"
				+ "		</endswitch[i1]>\r\n"
				+ "	</h3>\r\n"
				+ "	\r\n"
				+ "	<h3>Date: @value[date1]</h3>\r\n"
				+ "	<h3>byte @value[byte1]</h3>\r\n"
				+ "	<h3>short @value[short1]</h3>\r\n"
				+ "	<h3>int @value[int1]</h3>\r\n"
				+ "	<h3>long @value[long1]</h3>\r\n"
				+ "	<h3>float @value[float1]</h3>\r\n"
				+ "	<h3>double @value[double1]</h3>\r\n"
				+ "	<h3>char @value[char1]</h3>\r\n"
				+ "	<h3>boolean @value[boolean1]</h3>\r\n"
				+ "	<h3>钱 @value[money1]</h3>\r\n"
				+ "	\r\n"
				+ "	<h3>欢迎 @value[name]</h3>\r\n"
				+ "	<h3>再次欢迎 @value[name]</h3>\r\n"
				+ "	<h3>第三次欢迎 @value[name]</h3>\r\n"
				+ "\r\n"
				+ "	<@list[list1] as a>\r\n"
				+ "		<h3>姓名：@value[a.name] | 年龄 @value[a.age]</h3>\r\n"
				+ "	</endlist[list1]>\r\n"
				+ "	\r\n"
				+ "	<@list[list2] as a2>\r\n"
				+ "		<h3>姓名：@value[a2.name] | 年龄 @value[a2.age]</h3>\r\n"
				+ "	</endlist[list2]>\r\n"
				+ "	\r\n"
				+ "	<@list[list3] as a3>\r\n"
				+ "		<h3>姓名：@value[a3.name] | 年龄 @value[a3.age]</h3>\r\n"
				+ "	</endlist[list3]>\r\n"
				+ "	\r\n"
				+ "	<@list[list4] as a4>\r\n"
				+ "		<h3>姓名：@value[a4.name] | 年龄 @value[a4.age]</h3>\r\n"
				+ "	</endlist[list4]>\r\n"
				+ "\r\n"
				+ "	\r\n"
				+ "	<table border=\"1px\">\r\n"
				+ "		<tr><td>姓名</td><td>年龄</td>	</tr>\r\n"
				+ "		<@list[list5] as aT>\r\n"
				+ "			<tr><td>姓名:@value[aT.name]</td><td>年龄:@value[aT.age]</td>	</tr>\r\n"
				+ "		</endlist[list5]>\r\n"
				+ "	</table>\r\n"
				+ "</body>\r\n"
				+ "\r\n"
				+ "</html>";
//		 final String str = "ADSaksdnkjasdhfoWEFHOwhenfouuishefiuqhwieufwieuhrfiuqwheriuhqweiurhwqeiurh";

		System.out.println("原字符长度 = " + string.getBytes(DEFAULT_CHARSET).length);
		System.out.println("压缩后长度 = " + compress(string).length);
		System.out.println("解压后长度 = " + decompression(compress(string)).length());

	}

	public static String decompression(final byte[] ba) {
		if (ArrayUtil.isEmpty(ba)) {
			return null;
		}

		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(ba));
			final byte[] buffer = new byte[1024];
			int n;
			while ((n = gzip.read(buffer)) != -1) {
				out.write(buffer, 0, n);
			}

			out.flush();
			out.close();
			gzip.close();

			return new String(out.toByteArray(), DEFAULT_CHARSET);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static byte[] compress(final String string) {
		if (string == null || string.length() == 0) {
			return null;
		}
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final GZIPOutputStream gzip = new GZIPOutputStream(out);
			gzip.write(string.getBytes());
			gzip.finish();

			out.close();
			gzip.close();

			return out.toByteArray();

		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
