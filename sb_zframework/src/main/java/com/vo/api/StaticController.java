package com.vo.api;

import com.vo.anno.ZController;
import com.vo.conf.ServerConfiguration;
import com.vo.core.HeaderEnum;
import com.vo.core.ZGzip;
import com.vo.core.ZMappingRegex;
import com.vo.core.ZRequest;
import com.vo.core.ZResponse;
import com.vo.core.ZSingleton;
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

	public static final String CONTENT_ENCODING = "Content-Encoding";

	@ZRequestMapping(mapping = { "/favicon\\.ico", "/.+\\.js$", "/.+\\.jpg$", "/.+\\.mp3$", "/.+\\.mp4$", "/.+\\.pdf$",
			"/.+\\.gif$", "/.+\\.doc$" }, isRegex = { true, true, true, true, true, true, true, true })

	public void staticResources(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {

			response
				.httpStatus(HttpStatus.HTTP_500.getCode())
				.body(CR.error("不支持无后缀的文件"))
				.writeAndFlushAndClose();

			return;
		}

		final HeaderEnum cte = HeaderEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {

			response
					.httpStatus(HttpStatus.HTTP_500.getCode())
					.body(CR.error(HttpStatus.HTTP_500.getMessage()))
					.writeAndFlushAndClose();

			return;
		}

		ResourcesLoader.writeResourceToOutputStreamThenClose(resourceName, cte, response.getOutputStream(), response);
	}

	@ZRequestMapping(mapping = { "/.+\\.css$" }, isRegex = { true })
	public void css(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {

			response
				.httpStatus(HttpStatus.HTTP_500.getCode())
				.body(CR.error("不支持无后缀的文件"))
				.writeAndFlushAndClose();

			return;
		}

		final HeaderEnum cte = HeaderEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {
			response
				.httpStatus(HttpStatus.HTTP_500.getCode())
				.body(CR.error(HttpStatus.HTTP_500.getMessage()))
				.writeAndFlushAndClose();

			return;
		}

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		final String staticPrefix = serverConfiguration.getStaticPrefix();

		final Boolean gzipEnable = serverConfiguration.getGzipEnable();
		final boolean gzipContains = serverConfiguration.gzipContains(HeaderEnum.CSS.getType());
		if (gzipEnable && gzipContains && request.isSupportGZIP()) {
			final String string = ResourcesLoader.loadString(staticPrefix + resourceName);
			final byte[] ba = ZGzip.compress(string);

			response.contentType(cte.getType())
					.header(StaticController.CONTENT_ENCODING,ZRequest.GZIP)
				    .body(ba)
				    .writeAndFlushAndClose();

		} else {
			final String string = ResourcesLoader.loadString(staticPrefix + resourceName);

			response.contentType(cte.getType())
				    .body(string)
				    .writeAndFlushAndClose();
		}

	}

	/**
	 * 通用的html匹配接口
	 *
	 * @param response
	 * @param request
	 *
	 */
	@ZRequestMapping(mapping = { "/.+\\.html$" }, isRegex = { true })
	public void html(final ZResponse response,final ZRequest request) {

		final String resourceName = String.valueOf(ZMappingRegex.getAndRemove());

		final int i = resourceName.indexOf(".");
		if (i <= -1) {
			response
				.httpStatus(HttpStatus.HTTP_500.getCode())
				.body(CR.error("不支持无后缀的文件"))
				.writeAndFlushAndClose();

			return;
		}

		final HeaderEnum cte = HeaderEnum.gType(resourceName.substring(i + 1));
		if (cte == null) {

			response
				.httpStatus(HttpStatus.HTTP_500.getCode())
				.body(CR.error(HttpStatus.HTTP_500.getMessage()))
				.writeAndFlushAndClose();
			return;
		}

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		final Boolean gzipEnable = serverConfiguration.getGzipEnable();
		final boolean gzipContains = serverConfiguration.gzipContains(HeaderEnum.HTML.getType());
		final String html = ResourcesLoader.loadHtml(resourceName);

		if (gzipEnable && gzipContains && request.isSupportGZIP()) {
			final byte[] ba = ZGzip.compress(html);
			response.contentType(cte.getType())
					.header(StaticController.CONTENT_ENCODING,ZRequest.GZIP)
				    .body(ba)
				    .writeAndFlushAndClose();
		} else {
			response.contentType(cte.getType())
				    .body(html)
				    .writeAndFlushAndClose();
		}
	}
}
