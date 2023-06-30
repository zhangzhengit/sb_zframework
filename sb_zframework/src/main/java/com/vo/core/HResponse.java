package com.vo.core;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.util.ArrayList;

import org.codehaus.groovy.tools.shell.commands.HelpCommand;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.vo.core.HRequest.ZCookie;
import com.vo.core.HRequest.ZHeader;
import com.votool.common.CR;
import com.votool.redis.mq.TETS_MQ_1;

import cn.hutool.core.util.ArrayUtil;
import freemarker.core._DelayedGetMessageWithoutStackTop;
import lombok.AllArgsConstructor;
import lombok.Data;
import reactor.core.publisher.SynchronousSink;

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
public class HResponse {

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
		this.write(cr, "HTTP/1.1 " + httpStatus);
	}

	public void write200AndFlushAndClose(final byte[] ba,final ContentTypeEnum cte) {
		final byte[] baA = ArrayUtil.addAll(
				Task.HTTP_200.getBytes(),
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

			this.flush();
			this.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public  void write(final CR<?> cr,final String httpStatus) {
		final byte[] baERROR = ArrayUtil.addAll(
//				Task.HTTP_200.getBytes(),
				httpStatus.getBytes(),
				Task.NEW_LINE.getBytes(),
				ContentTypeEnum.JSON.getValue().getBytes(),
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
			this.outputStream.write(ContentTypeEnum.TEXT.getValue().getBytes());
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

	public void writeAndFlushAndClose(final String fileName, final byte[] ba, final ContentTypeEnum contentTypeEnum) {
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
