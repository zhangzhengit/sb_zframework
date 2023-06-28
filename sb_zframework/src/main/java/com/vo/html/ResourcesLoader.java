package com.vo.html;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 从 resources 目录加载文件
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
public class ResourcesLoader {

	private final static Map<String, Object> CACHE_MAP = Maps.newConcurrentMap();

	public static final String NEW_LINE = "\n\r";

	public static byte[] loadByteArray(final String name) {

		final Object ba = CACHE_MAP.get(name);
		if (ba != null) {
			return (byte[]) ba;
		}

		synchronized (name) {
			final InputStream in = checkIN(name);
			final byte[] ba2 = readByteArray(in);

			CACHE_MAP.put(name, ba2);
			return ba2;
		}
	}

	private static byte[] readByteArray(final InputStream in) {
		final BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
		final byte[] ba = new byte[1000 * 100];
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

		final byte[] byteArray = byteArrayOutputStream.toByteArray();
		return byteArray;
	}

	public static String loadString(final String name) {
		final String v = (String) CACHE_MAP.get(name);
		if (v != null) {
			return v;
		}

		synchronized (name) {
			final String v2 = load0(name);
			CACHE_MAP.put(name, v2);
			return v2;
		}
	}



	private static String load0(final String name) {
		final InputStream in = checkIN(name);

		final BufferedReader reader = new BufferedReader(new InputStreamReader(in));

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

		return builder.toString();
	}

	private static InputStream checkIN(final String name) {
		final InputStream in = ResourcesLoader.class.getResourceAsStream(name);
		if (in == null) {
			throw new IllegalArgumentException("文件不存在,name = " + name);
		}
		return in;
	}

}
