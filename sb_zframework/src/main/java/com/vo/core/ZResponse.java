package com.vo.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.vo.core.ZRequest.ZHeader;
import com.vo.http.HttpStatus;
import com.vo.http.ZCookie;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

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

	@Getter
	private final AtomicBoolean write = new AtomicBoolean(false);
	private final AtomicBoolean closed  = new AtomicBoolean(false);
	private final AtomicBoolean setContentType  = new AtomicBoolean(false);

	private final AtomicReference<Integer> httpStatus = new AtomicReference<>(HttpStatus.HTTP_200.getCode());
	private final AtomicReference<String> contentType = new AtomicReference<>();

	private final OutputStream outputStream;

	private final ArrayList<ZHeader> headerList = Lists.newArrayList();
	private final ArrayList<Byte> bodyList = Lists.newArrayList();

	public synchronized ZResponse contentType(final String contentType) {
		// FIXME 2023年7月2日 下午12:05:19 zhanghen: 校验参数是否正确
		if (!this.setContentType.get()) {
			this.contentType.set(ZRequest.CONTENT_TYPE + ":" + contentType);
		}

		this.setContentType.set(true);

		return this;
	}

	public ZResponse cookie(final ZCookie zCookie) {
		this.cookie(zCookie.getName(), zCookie.toCookieString());
		return this;
	}

	public ZResponse httpStatus(final Integer httpStatus) {
		this.httpStatus.set(httpStatus);
		return this;
	}

	public ZResponse cookie(final String name,final String value) {
		this.header(new ZHeader(ZResponse.SET_COOKIE, name + "=" + value));
		return this;
	}

	public ZResponse header(final ZHeader zHeader) {
		this.headerList.add(zHeader);
		return this;
	}

	public ZResponse header(final String name,final String value) {
		if (ZRequest.CONTENT_TYPE.equals(name)) {
			throw new IllegalArgumentException(ZRequest.CONTENT_TYPE + " 使用 setContentType 方法来设置");
		}
		this.header(new ZHeader(name, value));

		return this;
	}

	public ZResponse body(final byte[] body) {
		for (final byte b : body) {
			this.bodyList.add(b);
		}
		return this;
	}

	public ZResponse body(final Object body) {
		return this.body(String.valueOf(body));
	}

	public ZResponse body(final String body) {
		return this.body(body.getBytes());
	}

	/**
	 * 根据 contentType、 header、body 写入响应结果，只写一次。
	 *
	 */
	public synchronized void writeAndFlushAndClose() {

		try {
			if (this.write.get()) {
				return;
			}
			if (this.closed.get()) {
				return;
			}
			if (StrUtil.isEmpty(this.contentType.get())) {
				throw new IllegalArgumentException(ZRequest.CONTENT_TYPE + "未设置");
			}

			this.outputStream.write((ZResponse.HTTP_1_1 + this.httpStatus.get()).getBytes());
			this.outputStream.write(Task.NEW_LINE.getBytes());

			// header-Content-Length
			if (CollUtil.isNotEmpty(this.bodyList)) {
				final int contentLenght = this.bodyList.size();
				this.outputStream.write((ZRequest.CONTENT_LENGTH + ":" + contentLenght).getBytes());
				this.outputStream.write(Task.NEW_LINE.getBytes());
			}

			this.outputStream.write(this.contentType.get().getBytes());
			this.outputStream.write(Task.NEW_LINE.getBytes());

			for (int i = 0; i < this.headerList.size(); i++) {
				final ZHeader zHeader = this.headerList.get(i);
				this.outputStream.write((zHeader.getName() + ":" + zHeader.getValue()).getBytes());
				this.outputStream.write(Task.NEW_LINE.getBytes());
			}
			this.outputStream.write(Task.NEW_LINE.getBytes());

			// body
			if (CollUtil.isNotEmpty(this.bodyList)) {
				final byte[] ba = new byte[this.bodyList.size()];
				for (int b = 0; b < this.bodyList.size(); b++) {
					ba[b] = this.bodyList.get(b);
				}
				this.outputStream.write(ba);
			} else {
				this.outputStream.write(JSON.toJSONString(CR.ok()).getBytes());
			}

			this.outputStream.write(Task.NEW_LINE.getBytes());

		} catch (final IOException e) {
			e.printStackTrace();
			this.flushAndClose();
		} finally {
			this.write.set(true);
			this.flushAndClose();
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

	public synchronized void close() {
		try {
			this.outputStream.close();
			this.closed.set(true);
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			this.closed.set(true);
		}
	}

}
