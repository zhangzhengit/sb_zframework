package com.vo.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
import com.vo.api.StaticController;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZRequest.RequestLine;
import com.vo.core.ZRequest.RequestParam;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatus;
import com.vo.http.ZControllerMap;
import com.vo.template.ZModel;
import com.vo.template.ZTemplate;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class TaskNIO {

	private final SocketChannel socketChannel;
	private final String request;

	public TaskNIO(final SocketChannel socketChannel, final String request) {
		this.socketChannel = socketChannel;
		this.request = request;
	}

	public static ZRequest handleRead(final String requestString) {
		final ZRequest request = new ZRequest();

		final boolean contains = requestString.contains(Task.NEW_LINE);
		if (contains) {
			final String[] aa = requestString.split(Task.NEW_LINE);
			for (final String string : aa) {
				request.addLine(string);
			}
		}

		return request;
	}

	public void invoke(final ZRequest request) {
		// 匹配path
		final RequestLine requestLine = request.getRequestLine();
		if (CollUtil.isEmpty(request.getLineList())) {
			return;
		}

		final String path = requestLine.getPath();

		final Method method = ZControllerMap.getMethodByMethodEnumAndPath(requestLine.getMethodEnum(), path);

		// 查找对应的控制器来处理
		if (method == null) {

			final Map<String, Method> rowMap = ZControllerMap.getByMethodEnum(requestLine.getMethodEnum());
			final Set<Entry<String, Method>> entrySet = rowMap.entrySet();
			for (final Entry<String, Method> entry : entrySet) {
				final Method methodTarget = entry.getValue();
				final String requestMapping = entry.getKey();
				if (Boolean.TRUE.equals(ZControllerMap.getIsregexByMethodEnumAndPath(methodTarget, requestMapping)) &&path.matches(requestMapping)) {

					final Object object = ZControllerMap.getObjectByMethod(methodTarget);
					final Object[] arraygP = this.generateParameters(methodTarget, request, requestLine, path);
					try {
						ZMappingRegex.set(java.net.URLDecoder.decode(path, Task.UTF_8));
						this.invokeAndResponse(methodTarget, arraygP, object, request);
						return;
					} catch (IllegalAccessException | InvocationTargetException | UnsupportedEncodingException e) {
						e.printStackTrace();
					}

				}
			}

			new ZResponse(this.socketChannel).contentType(Task.DEFAULT_CONTENT_TYPE.getValue())
				  .body(JSON.toJSONString(CR.error(Task.HTTP_STATUS_404, "请求方法不存在 [" + path+"]")))
				  .writeAndFlushAndClose();

			return;
		}

		try {

			final Object[] p = this.generateParameters(method, request, requestLine, path);
			final Object zController = ZControllerMap.getObjectByMethod(method);
			this.invokeAndResponse(method, p, zController, request);

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			new ZResponse(this.socketChannel)
					.contentType(Task.DEFAULT_CONTENT_TYPE.getValue())
					.httpStatus(HttpStatus.HTTP_500.getCode())
					.body(JSON.toJSONString(CR.error(Task.INTERNAL_SERVER_ERROR)))
					.writeAndFlushAndClose();

			e.printStackTrace();
		} finally {
			try {
				this.socketChannel.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void writeAndClose(final String r1) {
		final ByteBuffer buffer = ByteBuffer.wrap(r1.getBytes(StandardCharsets.UTF_8));
		try {
			this.socketChannel.write(buffer);
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				this.socketChannel.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void invokeAndResponse(final Method method, final Object[] arraygP, final Object zController, final ZRequest request)
			throws IllegalAccessException, InvocationTargetException {


		final String controllerName = zController.getClass().getCanonicalName();
		final Integer qps = ZControllerMap.getQPSByControllerNameAndMethodName(controllerName, method.getName());

		final boolean allow = ZServer.Counter.allow(controllerName + method.getName(), qps);
		if (!allow) {

			final ZResponse response = new ZResponse(this.socketChannel);
			response.contentType(HeaderEnum.JSON.getType())
					.httpStatus(HttpStatus.HTTP_403.getCode())
					.body(JSON.toJSONString(CR.error("接口[" + method.getName() + "]超出QPS限制，请稍后再试")))
					.writeAndFlushAndClose();

			return;
		}

		this.setZRequestAndZResponse(arraygP, request);

		if (Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			method.invoke(zController, arraygP);
			// XXX 在此自动response.write 会报异常
			return;
		}

		final Object r = method.invoke(zController, arraygP);
		// 响应
		if (Task.isMethodAnnotationPresentZHtml(method)) {
			final String ss = String.valueOf(r);
			final String htmlName = ss.charAt(0) == '/' ? ss : '/' + ss;
			try {

				final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
				final String htmlContent = ResourcesLoader.loadString(serverConfiguration.getHtmlPrefix() + htmlName);

				final String html = ZTemplate.generate(htmlContent);

				if (serverConfiguration.getGzipEnable()
						&& serverConfiguration.gzipContains(HeaderEnum.HTML.getType())
						&& request.isSupportGZIP()) {
					final byte[] compress = ZGzip.compress(html);

					new ZResponse(this.socketChannel)
						.contentType(HeaderEnum.HTML.getType())
						.header(StaticController.CONTENT_ENCODING,ZRequest.GZIP)
						.body(compress)
						.writeAndFlushAndClose();
				} else {
					new ZResponse(this.socketChannel)
						.contentType(HeaderEnum.HTML.getType())
						.body(html)
						.writeAndFlushAndClose();
				}

			} catch (final Exception e) {
				new ZResponse(this.socketChannel)
					.httpStatus(HttpStatus.HTTP_500.getCode())
					.contentType(Task.DEFAULT_CONTENT_TYPE.getType())
					.body(CR.error(Task.HTTP_STATUS_500 + Task.INTERNAL_SERVER_ERROR))
					.writeAndFlushAndClose();
				e.printStackTrace();
			}
		} else {
			final String json = JSON.toJSONString(r);
			final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
			if (serverConfiguration.getGzipEnable()
					&& serverConfiguration.gzipContains(Task.DEFAULT_CONTENT_TYPE.getType())
					&& request.isSupportGZIP()
					) {
				final byte[] compress = ZGzip.compress(json);

				final ZResponse rrrrr = new ZResponse(this.socketChannel);
				rrrrr.contentType(Task.DEFAULT_CONTENT_TYPE.getType())
					 .header(StaticController.CONTENT_ENCODING,ZRequest.GZIP)
					 .body(compress)
					 .writeAndFlushAndClose();
			} else {

				new ZResponse(this.socketChannel)
					 .contentType(Task.DEFAULT_CONTENT_TYPE.getValue())
					 .body(json)
					 .writeAndFlushAndClose();
			}
		}
	}


	private Object[] generateParameters(final Method method, final Object[] parametersArray, final ZRequest request,
			final RequestLine requestLine, final String path) {

		final Parameter[] ps = method.getParameters();
		if (ps.length < parametersArray.length) {
			throw new IllegalArgumentException("方法参数个数小于数组length,method = " + method.getName()
					+ " parametersArray.length = " + parametersArray.length);
		}

		int pI = 0;
		for (final Parameter p : ps) {
			if (p.isAnnotationPresent(ZRequestHeader.class)) {
				final ZRequestHeader a = p.getAnnotation(ZRequestHeader.class);
				final String name = a.value();
				final String headerValue = requestLine.getHeaderMap().get(name);
				if ((headerValue == null) && a.required()) {
					new ZResponse(this.socketChannel)
						.httpStatus(HttpStatus.HTTP_404.getCode())
						.contentType(Task.DEFAULT_CONTENT_TYPE.getType())
						.body(JSON.toJSONString(CR.error(Task.HTTP_STATUS_404, "请求方法[" + path + "]的header[" + name + "]不存在")))
						.writeAndFlushAndClose();
					return null;
				}
				parametersArray[pI++] = headerValue;
			} else if (p.getType().getCanonicalName().equals(ZRequest.class.getCanonicalName())) {
				parametersArray[pI++] = request;
			} else if (p.getType().getCanonicalName().equals(ZResponse.class.getCanonicalName())) {
				final ZResponse hResponse = new ZResponse(this.socketChannel);
				parametersArray[pI++] = hResponse;
			} else if (p.getType().getCanonicalName().equals(ZModel.class.getCanonicalName())) {
				final ZModel model = new ZModel();
				parametersArray[pI++] = model;
			} else if (p.isAnnotationPresent(ZRequestBody.class)) {
				final String body = request.getBody();
				final Object object = JSON.parseObject(body, p.getType());
				parametersArray[pI++] = object;
			} else {

				final Set<RequestParam> paramSet = requestLine.getParamSet();
				final Optional<RequestParam> findAny = paramSet.stream().filter(rp -> rp.getName().equals(p.getName()))
						.findAny();
				if (!findAny.isPresent()) {
					new ZResponse(this.socketChannel)
							.httpStatus(HttpStatus.HTTP_404.getCode())
							.contentType(Task.DEFAULT_CONTENT_TYPE.getType())
							.body(JSON.toJSONString(CR.error(Task.HTTP_STATUS_404, "请求方法[" + path + "]的参数[" + p.getName() + "]不存在")))
							.writeAndFlushAndClose();
					return null;
				}

				// 先看参数类型
				if (p.getType().getCanonicalName().equals(Byte.class.getCanonicalName())) {
					parametersArray[pI++] = Byte.valueOf(String.valueOf(findAny.get().getValue()));
				} else if (p.getType().getCanonicalName().equals(Short.class.getCanonicalName())) {
					parametersArray[pI++] = Short.valueOf(String.valueOf(findAny.get().getValue()));
				} else if (p.getType().getCanonicalName().equals(Integer.class.getCanonicalName())) {
					parametersArray[pI++] = Integer.valueOf(String.valueOf(findAny.get().getValue()));
				} else if (p.getType().getCanonicalName().equals(Long.class.getCanonicalName())) {
					parametersArray[pI++] = Long.valueOf(String.valueOf(findAny.get().getValue()));
				} else if (p.getType().getCanonicalName().equals(Float.class.getCanonicalName())) {
					parametersArray[pI++] = Float.valueOf(String.valueOf(findAny.get().getValue()));
				} else if (p.getType().getCanonicalName().equals(Double.class.getCanonicalName())) {
					parametersArray[pI++] = Double.valueOf(String.valueOf(findAny.get().getValue()));
				} else if (p.getType().getCanonicalName().equals(Character.class.getCanonicalName())) {
					parametersArray[pI++] = Character.valueOf(String.valueOf(findAny.get().getValue()).charAt(0));
				} else if (p.getType().getCanonicalName().equals(Boolean.class.getCanonicalName())) {
					parametersArray[pI++] = Boolean.valueOf(String.valueOf(findAny.get().getValue()));
				} else {
					parametersArray[pI++] = findAny.get().getValue();
				}

			}

		}

		return parametersArray;
	}

	private Object[] generateParameters(final Method method, final ZRequest request, final RequestLine requestLine, final String path) {
		final Object[] parametersArray = new Object[method.getParameters().length];

		return this.generateParameters(method, parametersArray, request, requestLine, path);
	}

	private void setZRequestAndZResponse(final Object[] arraygP, final ZRequest request) {
		ZContext.setZRequest(request);

		boolean sR = false;
		for (final Object object : arraygP) {
			if (object.getClass().getCanonicalName().equals(ZResponse.class.getCanonicalName())) {
				ZContext.setZResponse((ZResponse) object);
				sR = true;
				break;
			}
		}

		if (!sR) {
			ZContext.setZResponse(new ZResponse(this.socketChannel));
		}
	}
}
