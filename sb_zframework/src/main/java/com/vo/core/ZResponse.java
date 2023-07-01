package com.vo.core;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.vo.core.ZRequest.ZCookie;
import com.vo.core.ZRequest.ZHeader;
import com.votool.common.CR;

import cn.hutool.core.util.ArrayUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * 表示一个http 响应对象
 *
 * @author zhangzhen
 * @date 2023年6月26日
 *
 */
@Data
@AllArgsConstructor
public class ZResponse {

	private static final String HTTP_1_1 = "HTTP/1.1 ";

	public static final String SET_COOKIE = "Set-Cookie";

	private final OutputStream outputStream;

	private final ArrayList<ZHeader> headerList = Lists.newArrayList();

	public void addCookie(final ZCookie zCookie) {
		this.addHeader(new ZHeader(zCookie.getName(), zCookie.getValue()));
	}

	public void addCookie(final String name,final String value) {
		this.addCookie(new ZCookie(SET_COOKIE, name + "=" + value));
	}

	public void addHeader(final ZHeader zHeader) {
		this.headerList.add(zHeader);
	}

	public void addHeader(final String name,final String value) {
		this.addHeader(new ZHeader(name, value));
	}

	public void write(final CR<?> cr, final Integer httpStatus) {
		this.write(cr, HTTP_1_1 + httpStatus);
	}

	public void writeAndFlushAndClose(final HeaderEnum cte,final int httpStatus, final CR cr) {
		this.writeAndFlushAndClose(cte, httpStatus, JSON.toJSONString(cr));
	}

	public void writeAndFlushAndClose(final HeaderEnum cte,final int httpStatus, final String message) {
		this.writeAndFlushAndClose(cte, httpStatus, message.getBytes());
	}

	public void writeAndFlushAndClose(final HeaderEnum cte,final int httpStatus, final byte[] ba) {

		final byte[] baA = ArrayUtil.addAll(
				(HTTP_1_1 + httpStatus).getBytes(),
				Task.NEW_LINE.getBytes(),
				cte.getValue().getBytes(),
				Task.NEW_LINE.getBytes(),
				Task.NEW_LINE.getBytes(),
				ba,
				Task.NEW_LINE.getBytes());

		try {
			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(this.outputStream);

			bufferedOutputStream.write(baA);
			bufferedOutputStream.flush();
			bufferedOutputStream.close();

			this.flushAndClose();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void writeOK200AndFlushAndClose(final String string, final HeaderEnum... headerArray) {
		final byte[] ba = string.getBytes();
		this.writeOK200AndFlushAndClose(ba, headerArray);
	}

	public void writeOK200AndFlushAndClose(final byte[] ba,final HeaderEnum... headerArray) {

		final String header = Lists.newArrayList(headerArray).stream().map(c -> c.getValue() + Task.NEW_LINE).collect(Collectors.joining());

		final byte[] baA = ArrayUtil.addAll(
				Task.HTTP_200.getBytes(),
				Task.NEW_LINE.getBytes(),
				header.getBytes(),
				Task.NEW_LINE.getBytes(),
				ba,
				Task.NEW_LINE.getBytes());

		try {
			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(this.outputStream);

			bufferedOutputStream.write(baA);
			bufferedOutputStream.flush();
			bufferedOutputStream.close();

			this.flushAndClose();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void write(final CR<?> cr, final String httpStatus) {
		final byte[] baERROR = ArrayUtil.addAll(
//				Task.HTTP_200.getBytes(),
				httpStatus.getBytes(),
				Task.NEW_LINE.getBytes(),
				HeaderEnum.JSON.getValue().getBytes(),
				Task.NEW_LINE.getBytes(),
				Task.NEW_LINE.getBytes(),
				JSON.toJSONString(cr).getBytes(),
				Task.NEW_LINE.getBytes());

		try {
			this.outputStream.write(baERROR);
			this.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				this.outputStream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void writeAndFlushAndClose() {
		try {
			this.outputStream.write(Task.HTTP_200.getBytes());
			this.outputStream.write(Task.NEW_LINE.getBytes());
			this.outputStream.write(HeaderEnum.TEXT.getValue().getBytes());
			this.outputStream.write(Task.NEW_LINE.getBytes());

			for (int i = 0; i < this.headerList.size(); i++) {
				final ZHeader zHeader = this.headerList.get(i);
				this.outputStream.write((zHeader.getName() + ":" + zHeader.getValue()).getBytes());
				this.outputStream.write(Task.NEW_LINE.getBytes());
			}

			this.outputStream.write(Task.NEW_LINE.getBytes());

			this.flush();
			this.outputStream.close();

		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	public void flushAndClose() {
		this.flush();
		this.close();
	}

	public void flush() {
		try {
			this.outputStream.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			this.outputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAndFlushAndClose(final String fileName, final byte[] ba, final HeaderEnum contentTypeEnum) {
		try {

			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(this.outputStream);

			final byte[] baA = ArrayUtil.addAll(
						Task.HTTP_200.getBytes(),
						Task.NEW_LINE.getBytes(),
						contentTypeEnum.getValue().getBytes(),
						Task.NEW_LINE.getBytes(),
						("Content-Disposition:attachment;filename=" + new String(fileName.getBytes(Task.UTF_8_CHARSET), Task.UTF_8_CHARSET)).getBytes(),
						Task.NEW_LINE.getBytes(),
						Task.NEW_LINE.getBytes(),
						ba,
						Task.NEW_LINE.getBytes()
					);

			bufferedOutputStream.write(baA);
			bufferedOutputStream.flush();
			bufferedOutputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}
}
