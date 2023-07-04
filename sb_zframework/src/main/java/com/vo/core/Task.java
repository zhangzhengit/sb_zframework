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

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
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
import com.vo.template.ZModel;
import com.vo.template.ZTemplate;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.URLDecoder;
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
	public static final String UTF_8 = "UTF-8";
	public static final String VOID = "void";
	public static final Charset UTF_8_CHARSET = Charset.forName(UTF_8);
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

	public static ZRequest handleRead(final String requestString) {
		final ZRequest request = new ZRequest();

		final boolean contains = requestString.contains(NEW_LINE);
		if (contains) {
			final String[] aa = requestString.split(NEW_LINE);
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

			new ZResponse(this.outputStream, this.socketChannel)
						.httpStatus(HttpStatus.HTTP_404.getCode())
						.contentType(DEFAULT_CONTENT_TYPE.getType())
						.body(JSON.toJSONString(CR.error(HTTP_STATUS_404, "请求方法不存在 [" + path+"]")))
						.writeAndFlushAndClose();

			return;
		}

		try {

			final Object[] p = this.generateParameters(method, request, requestLine, path);
			final Object zController = ZControllerMap.getObjectByMethod(method);
			this.invokeAndResponse(method, p, zController, request);

		} catch (final InvocationTargetException | IllegalAccessException e) {
			new ZResponse(this.outputStream, this.socketChannel)
					.contentType(DEFAULT_CONTENT_TYPE.getType())
					.httpStatus(HttpStatus.HTTP_500.getCode())
					.body(JSON.toJSONString(CR.error(INTERNAL_SERVER_ERROR)))
					.writeAndFlushAndClose();

			e.printStackTrace();
		} finally {
			this.close();
		}
	}

	private void close() {
		if (this.socketChannel != null) {
			try {
				this.socketChannel.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
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

	private void invokeAndResponse(final Method method, final Object[] arraygP, final Object zController, final ZRequest request)
			throws IllegalAccessException, InvocationTargetException {


		final String controllerName = zController.getClass().getCanonicalName();
		final Integer qps = ZControllerMap.getQPSByControllerNameAndMethodName(controllerName, method.getName());

		final boolean allow = ZServer.Counter.allow(controllerName + method.getName(), qps);
		if (!allow) {

			final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
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
		if (isMethodAnnotationPresentZHtml(method)) {
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

					new ZResponse(this.outputStream, this.socketChannel)
						.contentType(HeaderEnum.HTML.getType())
						.header(StaticController.CONTENT_ENCODING,ZRequest.GZIP)
						.body(compress)
						.writeAndFlushAndClose();
				} else {
					new ZResponse(this.outputStream, this.socketChannel)
						.contentType(HeaderEnum.HTML.getType())
						.body(html)
						.writeAndFlushAndClose();
				}

			} catch (final Exception e) {
				new ZResponse(this.outputStream, this.socketChannel)
					.httpStatus(HttpStatus.HTTP_500.getCode())
					.contentType(DEFAULT_CONTENT_TYPE.getType())
					.body(CR.error(HTTP_STATUS_500 + INTERNAL_SERVER_ERROR))
					.writeAndFlushAndClose();
				e.printStackTrace();
			}
		} else {
			final String json = JSON.toJSONString(r);
			final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);
			if (serverConfiguration.getGzipEnable()
					&& serverConfiguration.gzipContains(DEFAULT_CONTENT_TYPE.getType())
					&& request.isSupportGZIP()
					) {
				final byte[] compress = ZGzip.compress(json);

				new ZResponse(this.outputStream, this.socketChannel)
					 .header(StaticController.CONTENT_ENCODING,ZRequest.GZIP)
					 .contentType(DEFAULT_CONTENT_TYPE.getType())
					 .body(compress)
					 .writeAndFlushAndClose();

			} else {

				new ZResponse(this.outputStream, this.socketChannel)
					 .contentType(DEFAULT_CONTENT_TYPE.getType())
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
					new ZResponse(this.outputStream, this.socketChannel)
						.httpStatus(HttpStatus.HTTP_404.getCode())
						.contentType(DEFAULT_CONTENT_TYPE.getType())
						.body(JSON.toJSONString(CR.error(HTTP_STATUS_404, "请求方法[" + path + "]的header[" + name + "]不存在")))
						.writeAndFlushAndClose();
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
							.writeAndFlushAndClose();
					 return null;
				}

				final Optional<RequestParam> findAny = paramSet.stream().filter(rp -> rp.getName().equals(p.getName()))
						.findAny();
				if (!findAny.isPresent()) {
					new ZResponse(this.outputStream, this.socketChannel)
							.httpStatus(HttpStatus.HTTP_404.getCode())
							.contentType(Task.DEFAULT_CONTENT_TYPE.getType())
							.body(JSON.toJSONString(CR.error(HTTP_STATUS_404, "请求方法[" + path + "]的参数[" + p.getName() + "]不存在")))
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
			ZContext.setZResponse(new ZResponse(this.outputStream, this.socketChannel));
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
			final ZRequest v2 = parseRequest0(request);
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
			if (me != null) {
				requestLine.setMethodEnum(me);
			} else {
				// FIXME 2023年6月28日 下午10:04:18 zhanghen: 返回500
//				handleWrite500(DEFAULT_CONTENT_TYPE, CR.error(HTTP_STATUS_500, "不支持的请求方法 method = " + methodS), so);
				throw new IllegalArgumentException("不支持的请求方法 method = " + methodS);
			}
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
				line.setPath(simplePath);

				final String[] paramArray = param.split(SP);
				for (final String p : paramArray) {
					final String[] p0 = p.split("=");
					final ZRequest.RequestParam requestParam  = new ZRequest.RequestParam();
					requestParam.setName(p0[0]);
					if (p0.length >= 2) {
						final String v = StrUtil.isEmpty(p0[1]) ? EMPTY_STRING : URLDecoder.decode(p0[1], UTF_8_CHARSET);
						requestParam.setValue(v);
					} else {
						requestParam.setValue(EMPTY_STRING);
					}

					paramSet.add(requestParam);
				}

				line.setParamSet(paramSet);


			} else {
				line.setPath(fullPath);
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
		return parseRequest(request);
	}
}
