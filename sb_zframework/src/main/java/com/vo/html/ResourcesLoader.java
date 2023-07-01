package com.vo.html;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.HashBasedTable;
import com.vo.conf.ServerConfiguration;
import com.vo.core.HeaderEnum;
import com.vo.core.Task;
import com.vo.core.ZSingleton;

/**
 * 从 resources 目录加载文件
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
// FIXME 2023年7月1日 上午9:08:39 zhanghen: 静态资源支持和zf.properties一样，支持在jar包外的目录加载
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

	/**
	 * 把静态资源写入输出流，不放入缓存.
	 *
	 * @param resourceName
	 * @param cte TODO
	 * @param outputStream
	 * @return 返回写入的字节数
	 */
	public static long writeResourceToOutputStreamThenClose(final String resourceName, final HeaderEnum cte, final OutputStream outputStream) {

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		final String staticPrefix = serverConfiguration.getStaticPrefix();
		final String key = staticPrefix + resourceName;

		final InputStream inputStream = checkInputStream(key);

		final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

		try {
			outputStream.write(Task.HTTP_200.getBytes());
			outputStream.write(Task.NEW_LINE.getBytes());
			outputStream.write(cte.getValue().getBytes());
			outputStream.write(Task.NEW_LINE.getBytes());
			outputStream.write(Task.NEW_LINE.getBytes());
		} catch (final IOException e1) {
			e1.printStackTrace();
		}

		final AtomicLong write = writeToOutputStream(bufferedInputStream, outputStream);
		try {
//			outputStream.write(Task.NEW_LINE.getBytes());
			outputStream.flush();
			outputStream.close();

			bufferedInputStream.close();
			inputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return write.get();
	}

	private static AtomicLong writeToOutputStream(final BufferedInputStream bufferedInputStream,
			final OutputStream outputStream) {
		final byte[] ba = new byte[1000 * 10];
		final AtomicLong write = new AtomicLong(0);
		while (true) {
			try {
				final int read = bufferedInputStream.read(ba);
				if (read <= -1) {
					break;
				}

				write.set(write.get() + read);
				outputStream.write(ba, 0, read);
				outputStream.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return write;
	}

	/**
	 * 加载静态资源，resourceName 不用自己拼接前缀目录了，此方法内自动拼接
	 *
	 * @param resourceName
	 * @return
	 *
	 */
	public static byte[] loadStaticResource(final String resourceName) {
		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		final String staticPrefix = serverConfiguration.getStaticPrefix();
		final String key = staticPrefix + resourceName;

		final byte[] ba = loadByteArray(key);
		return ba;
	}

	/**
	 *
	 * @param resourceName
	 * @return
	 *
	 */
	public static byte[] loadByteArray(final String resourceName) {

		final Object v = CACHE_TABLE.get(ResourcesTypeEnum.BINARY, resourceName);
		if (v != null) {
			return (byte[]) v;
		}

		synchronized (resourceName) {
			final InputStream in = checkInputStream(resourceName);
			final byte[] ba2 = readByteArray0(in);

			CACHE_TABLE.put(ResourcesTypeEnum.BINARY, resourceName, ba2);
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
