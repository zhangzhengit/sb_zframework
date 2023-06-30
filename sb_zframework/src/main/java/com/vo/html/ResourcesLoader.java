package com.vo.html;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.collect.HashBasedTable;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZSingleton;

/**
 * 从 resources 目录加载文件
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
// FIXME 2023年6月29日 上午12:14:41 zhanghen:TODO 不要一次性全加载完，新增方法：一边读取一遍写入response
public class ResourcesLoader {
	public static final String NEW_LINE = "\n\r";

	private final static HashBasedTable<ResourcesTypeEnum, String, Object> CACHE_TABLE = HashBasedTable.create();

	/**
	 * 加载html页面内容，htmlName 不用自己拼接前缀目录了，此方法内自动拼接
	 *
	 * @param htmlName
	 * @return
	 */
	public static String loadHtml(final String htmlName) {
		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		final String htmlPrefix = serverConfiguration.getHtmlPrefix();
		final String key = htmlPrefix + htmlName;

		final String htmlContent = loadString(key);
		return htmlContent;
	}

	public static byte[] loadByteArray(final String name) {

		final Object v = CACHE_TABLE.get(ResourcesTypeEnum.BINARY, name);
		if (v != null) {
			return (byte[]) v;
		}

		synchronized (name) {
			final InputStream in = checkInputStream(name);
			final byte[] ba2 = readByteArray0(in);

			CACHE_TABLE.put(ResourcesTypeEnum.BINARY, name, ba2);
			return ba2;
		}
	}

	public static String loadString(final String name) {

		final Object v = CACHE_TABLE.get(ResourcesTypeEnum.STRING, name);
		if (v != null) {
			return (String) v;
		}

		synchronized (name) {
			final String v2 = loadSring0(name);
			CACHE_TABLE.put(ResourcesTypeEnum.STRING, name, v2);
			return v2;
		}
	}

	private static String loadSring0(final String name) {
		final InputStream inputStream = checkInputStream(name);
		final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		final BufferedReader reader = new BufferedReader(inputStreamReader);

		final StringBuilder builder = new StringBuilder();
		while (true) {
			try {
				final String readLine = reader.readLine();
				if (readLine != null) {
					builder.append(readLine);
					builder.append(NEW_LINE);
				} else {
					break;
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		try {
			reader.close();
			inputStreamReader.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return builder.toString();
	}

	private static byte[] readByteArray0(final InputStream inputStream) {
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
		final byte[] ba = new byte[1000 * 10];
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		while (true) {
			try {
				final int read = bufferedInputStream.read(ba);
				if (read <= -1) {
					break;
				}

				byteArrayOutputStream.write(ba, 0, read);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		try {
			// 空方法
			byteArrayOutputStream.close();
			bufferedInputStream.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		final byte[] byteArray = byteArrayOutputStream.toByteArray();
		return byteArray;
	}


	private static InputStream checkInputStream(final String name) {
		final InputStream inputStream = ResourcesLoader.class.getResourceAsStream(name);
		if (inputStream == null) {
			throw new IllegalArgumentException("资源不存在,name = " + name);
		}
		return inputStream;
	}

	public static enum ResourcesTypeEnum{

		BINARY,STRING;
	}

}
