package com.vo.api;

import java.io.BufferedOutputStream;
import java.io.IOException;

import com.vo.anno.ZController;
import com.vo.core.ContentTypeEnum;
import com.vo.core.HResponse;
import com.vo.core.Task;
import com.vo.core.ZMappingRegex;
import com.vo.html.ResourcesLoader;
import com.vo.http.ZRequestMapping;
import com.votool.common.CR;

import cn.hutool.core.util.ArrayUtil;

/**
 *
 * 内置的处理静态资源的接口，处理比如 .css .js .jpg 等文件
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
@ZController
public class StaticController {

	@ZRequestMapping(mapping = { "/.+\\.html$", "/.+\\.js$", "/.+\\.css$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$",
			"/.+\\.pdf$", "/.+\\.gif$",
			"/.+\\.doc$" }, isRegex = { true, true, true, true, true, true, true, true, true })
	public void staticResources(final HResponse response) {

		final String rName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = rName.indexOf(".");
		if (i <= -1) {
			response.write(CR.error(Task.HTTP_STATUS_500, "不支持无后缀的文件"), Task.HTTP_500);
			return;
		}

		final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());

		final ContentTypeEnum cte = ContentTypeEnum.gType(rName.substring(i + 1));

		final byte[] ba  = ResourcesLoader.loadByteArray(rName);

		final byte[] baA = ArrayUtil.addAll(
				Task.HTTP_200.getBytes(),
				Task.NEW_LINE.getBytes(),
				cte.getValue().getBytes(),
				Task.NEW_LINE.getBytes(),
				Task.NEW_LINE.getBytes(),
				ba,
				Task.NEW_LINE.getBytes());

		try {
			bufferedOutputStream.write(baA);
			bufferedOutputStream.flush();
			bufferedOutputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}


//	@ZRequestMapping(mapping = { "/p4" }, isRegex = { false }, method = MethodEnum.GET)
//	public CR<Object> p456() {
//		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
//				+ "StaticController.p456()");
//		return CR.ok();
//	}
//
//	@ZRequestMapping(mapping = { "/p1", "/p2", "/p3" }, isRegex = { false, false, false }, method = MethodEnum.GET)
//	public CR<Object> p123() {
//		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
//				+ "StaticController.p123()");
//		return CR.ok();
//	}


	@ZRequestMapping(mapping = { "/favicon.ico" })
	public void faviconIco(final HResponse response) {

		final byte[] ba = ResourcesLoader.loadByteArray("/image/favicon.ico");
		final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());

		final byte[] baA = ArrayUtil.addAll(Task.HTTP_200.getBytes(), Task.NEW_LINE.getBytes(),
				ContentTypeEnum.JPG.getValue().getBytes(), Task.NEW_LINE.getBytes(), Task.NEW_LINE.getBytes(), ba,
				Task.NEW_LINE.getBytes());

		try {
			bufferedOutputStream.write(baA);
			bufferedOutputStream.flush();
			bufferedOutputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}
}
