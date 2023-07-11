package com.vo.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vo.anno.ZControllerInterceptor;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
import com.vo.aop.InterceptorParameter;
import com.vo.api.StaticController;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZRequest.RequestLine;
import com.vo.core.ZRequest.RequestParam;
import com.vo.enums.MethodEnum;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatus;
import com.vo.http.LineMap;
import com.vo.http.ZControllerMap;
import com.vo.http.ZHtml;
import com.vo.scanner.ZControllerInterceptorScanner;
import com.vo.template.ZModel;
import com.vo.template.ZTemplate;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class Task {

	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private static final int READ_LENGTH = DEFAULT_BUFFER_SIZE / 2;
	public static final String SP = "&";
	public static final String EMPTY_STRING = "";

	public static final String DEFAULT_CHARSET_NAME = Charset.defaultCharset().displayName();
	public static final String VOID = "void";
	public static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
	public static final String HTTP_200 = "HTTP/1.1 200";
	public static final int HTTP_STATUS_500 = 500;
	public static final int HTTP_STATUS_404 = 404;
	public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
	public static final HeaderEnum DEFAULT_CONTENT_TYPE = HeaderEnum.JSON;
	public static final String NEW_LINE = "\r\n";

	private final SocketChannel socketChannel;
	private final Socket socket;
	private BufferedInputStream bufferedInputStream;
	private InputStream inputStream;
	private OutputStream outputStream;

	public Task(final SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
		this.socket = null;
	}

	public Task(final Socket socket) {
		this.socketChannel = null;
		this.socket = socket;
		try {
			this.inputStream = socket.getInputStream();
			this.bufferedInputStream = new BufferedInputStream(this.inputStream);
			this.outputStream = socket.getOutputStream();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public ZRequest handleRead(final String requestString) {
		final ZRequest request = new ZRequest();

		final boolean contains = requestString.contains(NEW_LINE);
		if (contains) {
			final String[] aa = requestString.split(NEW_LINE);
			for (final String string : aa) {
				request.addLine(string);
			}
		}

		final ZRequest parseRequest = parseRequest(request);
		return parseRequest;
	}

	/**
	 * 执行目标方法（接口Method）
	 *
	 * @param request 请求体
	 * @return 响应结果，已根据具体的方法处理好header、cookie、body等内容，只是没write
	 *
	 */
	public ZResponse invoke(final ZRequest request) {
		// 匹配path
		final RequestLine requestLine = request.getRequestLine();
		if (CollUtil.isEmpty(request.getLineList())) {
			return null;
		}

		final String path = requestLine.getPath();

		final Method method = ZControllerMap.getMethodByMethodEnumAndPath(requestLine.getMethodEnum(), path);

		// 查找对应的控制器来处理
		if (method == null) {
			final Map<MethodEnum, Method> methodMap = ZControllerMap.getByPath(path);
			if (CollUtil.isNotEmpty(methodMap)) {
				final String methodString = methodMap.keySet().stream().map(e -> e.getMethod()).collect(Collectors.joining(","));
				return new ZResponse(this.socketChannel)
					.header(ZRequest.ALLOW, methodString)
					.httpStatus(HttpStatus.HTTP_405.getCode())
					.contentType(HeaderEnum.JSON.getType())
					.body(JSON.toJSONString(CR.error(HttpStatus.HTTP_405.getCode(), "请求Method不支持："
							+ requestLine.getMethodEnum().getMethod() + ", Method: " + methodString)));

			}

			final Map<String, Method> rowMap = ZControllerMap.getByMethodEnum(requestLine.getMethodEnum());
			final Set<Entry<String, Method>> entrySet = rowMap.entrySet();
			for (final Entry<String, Method> entry : entrySet) {
				final Method methodTarget = entry.getValue();
				final String requestMapping = entry.getKey();
				if (Boolean.TRUE.equals(ZControllerMap.getIsregexByMethodEnumAndPath(methodTarget, requestMapping)) &&path.matches(requestMapping)) {

					final Object object = ZControllerMap.getObjectByMethod(methodTarget);
					final Object[] arraygP = this.generateParameters(methodTarget, request, requestLine, path);
					try {
						ZMappingRegex.set(URLDecoder.decode(path, DEFAULT_CHARSET_NAME));
						final ZResponse invokeAndResponse = this.invokeAndResponse(methodTarget, arraygP, object, request);
						return invokeAndResponse;
					} catch (IllegalAccessException | InvocationTargetException | UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}

			// 无匹配的正则表达式接口，返回404
			return	new ZResponse(this.outputStream, this.socketChannel)
						.httpStatus(HttpStatus.HTTP_404.getCode())
						.contentType(DEFAULT_CONTENT_TYPE.getType())
						.body(JSON.toJSONString(CR.error(HTTP_STATUS_404, "请求方法不存在 [" + path+"]")))	;

		}

		try {

			final Object[] p = this.generateParameters(method, request, requestLine, path);
			if (p == null) {
				return null;
			}

			final Object zController = ZControllerMap.getObjectByMethod(method);
			final ZResponse re = this.invokeAndResponse(method, p, zController, request);
			return re;

		} catch (final InvocationTargetException | IllegalAccessException e) {
			e.printStackTrace();
			return new ZResponse(this.outputStream, this.socketChannel)
					.contentType(DEFAULT_CONTENT_TYPE.getType())
					.httpStatus(HttpStatus.HTTP_500.getCode())
					.body(JSON.toJSONString(CR.error(INTERNAL_SERVER_ERROR)));

		} finally {
			this.close();
		}

	}

	private void close() {
		// socketChannel 不关闭
//		if (this.socketChannel != null) {
//		}
		if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		if (this.bufferedInputStream != null) {
			try {
				this.bufferedInputStream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		if (this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		if (this.socket != null) {
			try {
				this.socket.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("boxing")
	private ZResponse invokeAndResponse(final Method method, final Object[] arraygP, final Object zController, final ZRequest request)
			throws IllegalAccessException, InvocationTargetException {


		final String controllerName = zController.getClass().getCanonicalName();
		final Integer qps = ZControllerMap.getQPSByControllerNameAndMethodName(controllerName, method.getName());

		final boolean allow = ZServer.Counter.allow(controllerName + method.getName(), qps);
		if (!allow) {

			final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
			response.contentType(HeaderEnum.JSON.getType())
					.httpStatus(HttpStatus.HTTP_403.getCode())
					.body(JSON.toJSONString(CR.error("接口[" + method.getName() + "]超出QPS限制，请稍后再试")));

			return response;
		}

		this.setZRequestAndZResponse(arraygP, request);

		if (Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			final Set<ZControllerInterceptor> zciSet = ZControllerInterceptorScanner.get();
			if (zciSet.isEmpty()) {
				method.invoke(zController, arraygP);
			} else {
				final InterceptorParameter interceptorParameter = new InterceptorParameter(method.getName(), method,
						method.getReturnType().getCanonicalName().equals(Void.class.getCanonicalName()),
						Lists.newArrayList(arraygP), zController);
				zciSet.forEach(zci -> zci.before(interceptorParameter));
				for (final ZControllerInterceptor zciObject : zciSet) {
					zciObject.around(interceptorParameter, null);
				}
				zciSet.forEach(zci -> zci.after(interceptorParameter));
			}
			final ZResponse response = ZHttpContext.getZResponseAndRemove();
			return response;
		}

		Object r = null;
		final Set<ZControllerInterceptor> zciSet = ZControllerInterceptorScanner.get();
		if (!zciSet.isEmpty()) {
			final InterceptorParameter interceptorParameter = new InterceptorParameter(method.getName(), method,
					method.getReturnType().getCanonicalName().equals(Void.class.getCanonicalName()),
					Lists.newArrayList(arraygP), zController);
			zciSet.forEach(zci -> zci.before(interceptorParameter));
			for (final ZControllerInterceptor zciObject : zciSet) {
				r = zciObject.around(interceptorParameter, null);
			}
			zciSet.forEach(zci -> zci.after(interceptorParameter));
		} else {
			r = method.invoke(zController, arraygP);
		}

		// 响应
		if (isMethodAnnotationPresentZHtml(method)) {
			final String ss = String.valueOf(r);
			final String htmlName = ss.charAt(0) == '/' ? ss : '/' + ss;
			try {

				final String htmlContent = ResourcesLoader.loadStaticResourceString(htmlName);

				final String html = ZTemplate.generate(htmlContent);

				final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
				if (serverConfiguration.getGzipEnable()
						&& serverConfiguration.gzipContains(HeaderEnum.HTML.getType())
						&& request.isSupportGZIP()) {
					final byte[] compress = ZGzip.compress(html);

					return new ZResponse(this.outputStream, this.socketChannel).contentType(HeaderEnum.HTML.getType())
							.header(StaticController.CONTENT_ENCODING, ZRequest.GZIP).body(compress);
				}

				return new ZResponse(this.outputStream, this.socketChannel)
						.contentType(HeaderEnum.HTML.getType()).body(html);

			} catch (final Exception e) {
				e.printStackTrace();
				return new ZResponse(this.outputStream, this.socketChannel)
					.httpStatus(HttpStatus.HTTP_500.getCode())
					.contentType(DEFAULT_CONTENT_TYPE.getType())
					.body(CR.error(HTTP_STATUS_500 + INTERNAL_SERVER_ERROR));
			}
		}

		final String json = JSON.toJSONString(r);
		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
		if (serverConfiguration.getGzipEnable()
				&& serverConfiguration.gzipContains(DEFAULT_CONTENT_TYPE.getType())
				&& request.isSupportGZIP()) {
			final byte[] compress = ZGzip.compress(json);

			return new ZResponse(this.outputStream, this.socketChannel)
					.header(StaticController.CONTENT_ENCODING, ZRequest.GZIP)
					.contentType(DEFAULT_CONTENT_TYPE.getType()).body(compress);

		}

		return new ZResponse(this.outputStream, this.socketChannel).contentType(DEFAULT_CONTENT_TYPE.getType())
				.body(json);
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
					new ZResponse(this.outputStream, this.socketChannel)
						.httpStatus(HttpStatus.HTTP_404.getCode())
						.contentType(DEFAULT_CONTENT_TYPE.getType())
						.body(JSON.toJSONString(CR.error(HTTP_STATUS_404, "请求方法[" + path + "]的header[" + name + "]不存在")))
						.write();
					return null;
				}
				parametersArray[pI++] = headerValue;
			} else if (p.getType().getCanonicalName().equals(ZRequest.class.getCanonicalName())) {
				parametersArray[pI++] = request;
			} else if (p.getType().getCanonicalName().equals(ZResponse.class.getCanonicalName())) {
				final ZResponse hResponse = new ZResponse(this.outputStream, this.socketChannel);
				parametersArray[pI++] = hResponse;
			} else if (p.getType().getCanonicalName().equals(ZModel.class.getCanonicalName())) {
				final ZModel model = new ZModel();
				parametersArray[pI++] = model;
			} else if (p.isAnnotationPresent(ZRequestBody.class)) {
				final String body = request.getBody();
				final Object object = JSON.parseObject(body, p.getType());
				parametersArray[pI++] = object;
			} else {

				// 到此 肯定是从 paramSet 取值作为参数，如果 paramSet 为空，则说明没传
				final Set<RequestParam> paramSet = requestLine.getParamSet();
				if (CollUtil.isEmpty(paramSet)) {
//					throw new IllegalArgumentException("Param 为空");
					new ZResponse(this.outputStream, this.socketChannel)
							.contentType(Task.DEFAULT_CONTENT_TYPE.getType())
							.httpStatus(HttpStatus.HTTP_404.getCode())
							.body(JSON.toJSONString(CR.error("Param 为空")))
							.write();
					 return null;
				}

				final Optional<RequestParam> findAny = paramSet.stream().filter(rp -> rp.getName().equals(p.getName()))
						.findAny();
				if (!findAny.isPresent()) {
					new ZResponse(this.outputStream, this.socketChannel)
							.httpStatus(HttpStatus.HTTP_404.getCode())
							.contentType(Task.DEFAULT_CONTENT_TYPE.getType())
							.body(JSON.toJSONString(CR.error(HTTP_STATUS_404, "请求方法[" + path + "]的参数[" + p.getName() + "]不存在")))
							.write();
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

		if (arraygP == null) {
			return;
		}

		ZHttpContext.setZRequest(request);

		boolean sR = false;
		for (final Object object : arraygP) {
			if (object.getClass().getCanonicalName().equals(ZResponse.class.getCanonicalName())) {
				ZHttpContext.setZResponse((ZResponse) object);
				sR = true;
				break;
			}
		}

		if (!sR) {
			ZHttpContext.setZResponse(new ZResponse(this.outputStream, this.socketChannel));
		}
	}

	static Map<Object, Object> cacheMap = new WeakHashMap<>(1024, 1F);
	public static Boolean isMethodAnnotationPresentZHtml(final Method method) {
		final String name = method.getName();
		final Boolean boolean1 = (Boolean) cacheMap.get(name);
		if (boolean1 != null) {
			return boolean1;
		}

		synchronized (name) {

			final boolean annotationPresent = method.isAnnotationPresent(ZHtml.class);
			cacheMap.put(name, annotationPresent);
			return annotationPresent;
		}
	}

	private static final ConcurrentMap<Object, Object> CACHE_MAP = Maps.newConcurrentMap();
	public static ZRequest parseRequest(final ZRequest request) {
		final Object v = CACHE_MAP.get(request);
		if (v != null) {
			return (ZRequest) v;
		}

		synchronized (request) {
			final ZRequest v2 = Task.parseRequest0(request);
			if (v2 == null) {
				return v2;
			}

			CACHE_MAP.put(request, v2);
			return v2;
		}
	}

	private static ZRequest parseRequest0(final ZRequest request) {
		final ZRequest.RequestLine requestLine = new ZRequest.RequestLine();
		if (CollUtil.isEmpty(request.getLineList())) {
			return request;
		}

		// 0 为 请求行
		final String line = request.getLineList().get(0);
		requestLine.setOriginal(line);

		// method
		final int methodIndex = line.indexOf(" ");
		if (methodIndex > -1) {
			final String methodS = line.substring(0, methodIndex);
			final MethodEnum me = MethodEnum.valueOfString(methodS);
			// 可能是null，在这里不管，在外面处理，返回405
			requestLine.setMethodEnum(me);
		}

		// path
		parsePath(line, requestLine, methodIndex);

		// version
		parseVersion(requestLine, line);

		LineMap.put(requestLine.getFullpath(), requestLine);

		// paserHeader
		paserHeader(request, requestLine);

		// parseBody
		parseBody(request, requestLine);

		request.setRequestLine(requestLine);

		return request;
	}

	private static void parsePath(final String s, final ZRequest.RequestLine line, final int methodIndex) {
		final int pathI = s.indexOf(" ", methodIndex + 1);
		if (pathI > methodIndex) {
			final String fullPath = s.substring(methodIndex  + 1, pathI);

			final int wenI = fullPath.indexOf("?");
			if (wenI > -1) {
				line.setQueryString(fullPath.substring("?".length() + wenI - 1));

				final Set<RequestParam> paramSet = Sets.newHashSet();
				final String param = fullPath.substring("?".length() + wenI);
				final String simplePath = fullPath.substring(0,wenI);

				try {
					line.setPath(java.net.URLDecoder.decode(simplePath, DEFAULT_CHARSET_NAME));
				} catch (final UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				final String[] paramArray = param.split(SP);
				for (final String p : paramArray) {
					final String[] p0 = p.split("=");
					final ZRequest.RequestParam requestParam = new ZRequest.RequestParam();
					requestParam.setName(p0[0]);
					if (p0.length >= 2) {
						try {
							final String v = StrUtil.isEmpty(p0[1]) ? EMPTY_STRING
									: java.net.URLDecoder.decode(p0[1], DEFAULT_CHARSET_NAME);
							requestParam.setValue(v);
						} catch (final UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					} else {
						requestParam.setValue(EMPTY_STRING);
					}

					paramSet.add(requestParam);
				}

				line.setParamSet(paramSet);

			} else {
				try {
					line.setPath(java.net.URLDecoder.decode(fullPath, DEFAULT_CHARSET_NAME));
				} catch (final UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			line.setFullpath(fullPath);
		}
	}

	private static void parseVersion(final ZRequest.RequestLine requestLine, final String line) {
		final int hI = line.lastIndexOf("HTTP/");
		if (hI > -1) {
			final String version = line.substring(hI);
			requestLine.setVersion(version);
		}
	}

	private static void paserHeader(final ZRequest request, final ZRequest.RequestLine requestLine) {
		final List<String> x = request.getLineList();
		final HashMap<String, String> hm = new HashMap<>(16, 1F);
		for (int i = 1; i < x.size(); i++) {
			final String l = x.get(i);
			if (EMPTY_STRING.equals(l)) {
				continue;
			}

			final int k = l.indexOf(":");
			if (k > -1) {
				final String key = l.substring(0, k).trim();
				final String value = l.substring(k + 1).trim();
				hm.put(key, value);
			}
		}

		requestLine.setHeaderMap(hm);
	}

	private static void parseBody(final ZRequest request, final ZRequest.RequestLine requestLine) {
		final List<String> x = request.getLineList();
		for (int i = 1; i < x.size(); i++) {
			final String l2 = x.get(i);
			if (EMPTY_STRING.equals(l2) && (i < x.size()) && i + 1 < x.size()) {

				final String contentType = requestLine.getHeaderMap().get(ZRequest.CONTENT_TYPE);
				if (contentType.equalsIgnoreCase(HeaderEnum.JSON.getType())
						|| contentType.toLowerCase().contains(HeaderEnum.JSON.getType().toLowerCase())) {

					final StringBuilder json = new StringBuilder();
					for (int k = i + 1; k < x.size(); k++) {
						json.append(x.get(k));
					}
					request.setBody(json.toString());
				}
				break;
			}

		}
	}

	public ZRequest readAndParse() {
		final ZRequest r1 = this.read();
		final ZRequest r2 = this.parse(r1);
		return r2;
	}

	public ZRequest read() {
		final ZRequest request = this.handleRead();
		return request;
	}

	private ZRequest handleRead() {

		final ZRequest request = new ZRequest();

		final int nk = READ_LENGTH;
		final List<Byte> list = new ArrayList<>(nk);

		while (true) {
			final byte[] bs = new byte[nk];
			int read = -4;
			try {
				read = this.bufferedInputStream.read(bs);
			} catch (final IOException e) {
//						e.printStackTrace();
				break;
			}
			if (read <= 0) {
				break;
			}

			for (int i = 0; i < read; i++) {
				list.add(bs[i]);
			}
			if (read <= nk) {
				break;
			}
		}

		final byte[] bsR = new byte[list.size()];
		for (int x = 0; x < list.size(); x++) {
			bsR[x] = list.get(x);
		}

		final String r = new String(bsR);

		final boolean contains = r.contains(NEW_LINE);
		if (contains) {
			final String[] aa = r.split(NEW_LINE);
			for (final String string : aa) {
				request.addLine(string);
			}
		}

//					bufferedInputStream.close();
//					inputStream.close();

		return request;

	}

	public ZRequest parse(final ZRequest request) {
		return Task.parseRequest(request);
	}

}
