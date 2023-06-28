package com.vo.core;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

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

	private final OutputStream outputStream;

	private final ArrayList<ZHeader> headerList = Lists.newArrayList();

	public void addCookie(final ZCookie zCookie) {
		this.addHeader(new ZHeader(zCookie.getName(), zCookie.getValue()));
	}

	public void addCookie(final String name,final String value) {
		this.addCookie(new ZCookie(name, value));
	}

	public void addHeader(final ZHeader zHeader) {
		this.headerList.add(zHeader);
	}

	public void addHeader(final String name,final String value) {
		this.addHeader(new ZHeader(name, value));
	}


	public  void write(final CR cr,final String httpStatus) {
		final byte[] baERROR = ArrayUtil.addAll(
				Task.OK_200.getBytes(),
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

	public void writeAndFlush() {
		try {
			this.outputStream.write(Task.OK_200.getBytes());
			this.outputStream.write(Task.NEW_LINE.getBytes());
			this.outputStream.write(ContentTypeEnum.TEXT.getValue().getBytes());
			this.outputStream.write(Task.NEW_LINE.getBytes());

			// header
			for (final ZHeader zHeader : this.headerList) {
				final String s = zHeader.getName() + "=" + zHeader.getValue();
				// FIXME 2023年6月26日 下午11:33:23 zhanghen: cookie和header不要用一个list放，区分不开了
				this.outputStream.write(("Set-Cookie:" + s).getBytes());

				this.outputStream.write(Task.NEW_LINE.getBytes());
			}

			this.outputStream.write(Task.NEW_LINE.getBytes());
			System.out.println("this.outputStream.write(Task.NEW_LINE.getBytes());");
			this.flush();

			this.outputStream.close();

		} catch (final IOException e) {
			e.printStackTrace();
		}

		final byte[] baA = ArrayUtil.addAll(
				Task.OK_200.getBytes(),
				Task.NEW_LINE.getBytes(),
//				contentTypeEnum.getValue().getBytes(),
//				Task.NEW_LINE.getBytes(),
//				("Content-Disposition:attachment;filename=" + new String(fileName.getBytes(Task.UTF_8), Task.UTF_8)).getBytes(),
				Task.NEW_LINE.getBytes(),
				Task.NEW_LINE.getBytes(),
//				ba,
				Task.NEW_LINE.getBytes()
			);

//		outputStream.write(null);
	}
	public void flush() {
		try {
			this.outputStream.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAndFlushAndClose(final String fileName, final byte[] ba, final ContentTypeEnum contentTypeEnum) {
		try {

			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(this.outputStream);

			final byte[] baA = ArrayUtil.addAll(
						Task.OK_200.getBytes(),
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
