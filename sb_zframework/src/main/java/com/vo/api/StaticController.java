package com.vo.api;

import com.vo.anno.ZController;
import com.vo.core.ContentTypeEnum;
import com.vo.core.HRequest;
import com.vo.core.HResponse;
import com.vo.core.Task;
import com.vo.core.ZMappingRegex;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatus;
import com.vo.http.ZRequestMapping;
import com.votool.common.CR;

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

	@ZRequestMapping(mapping = {
			"/favicon\\.ico", "/.+\\.js$", "/.+\\.css$", "/.+\\.jpg$", "/.+\\.mp3$",
			"/.+\\.mp4$", "/.+\\.pdf$", "/.+\\.gif$", "/.+\\.doc$" },
			isRegex = {true,  true, true, true, true, true, true, true, true })

	public void staticResources(final HResponse response,final HRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response.write(CR.error(Task.HTTP_STATUS_500, "不支持无后缀的文件"), Task.HTTP_500);
			return;
		}

		final ContentTypeEnum cte = ContentTypeEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.write(CR.error(HttpStatus.HTTP_500.getCode(), HttpStatus.HTTP_500.getMessage()),
					HttpStatus.HTTP_500.getCode());
			return;
		}

		ResourcesLoader.writeResourceToOutputStreamThenClose(resourceName, response.getOutputStream());
	}

	/**
	 * 通用的html匹配接口
	 *
	 * @param response
	 * @param request
	 *
	 */
	@ZRequestMapping(mapping = { "/.+\\.html$" }, isRegex = { true })
	public void html(final HResponse response,final HRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response.write(CR.error(Task.HTTP_STATUS_500, "不支持无后缀的文件"), Task.HTTP_500);
			return;
		}

		final ContentTypeEnum cte = ContentTypeEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response.write(CR.error(HttpStatus.HTTP_500.getCode(), HttpStatus.HTTP_500.getMessage()),
					HttpStatus.HTTP_500.getCode());
			return;
		}

		final String html = ResourcesLoader.loadHtml(resourceName);
		response.writeOK200AndFlushAndClose(cte, html);
	}
}
