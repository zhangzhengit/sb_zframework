package com.vo.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZControllerInterceptor;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
import com.vo.aop.AOPParameter;
import com.vo.aop.InterceptorParameter;
import com.vo.aop.ZIAOP;
import com.vo.api.StaticController;
import com.vo.cache.J;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZRequest.RequestLine;
import com.vo.core.ZRequest.RequestParam;
import com.vo.enums.MethodEnum;
import com.vo.exception.ZControllerAdviceThrowable;
import com.vo.html.ResourcesLoader;
import com.vo.http.HttpStatus;
import com.vo.http.LineMap;
import com.vo.http.ZControllerMap;
import com.vo.http.ZHtml;
import com.vo.http.ZPVTL;
import com.vo.http.ZQPSLimitation;
import com.vo.http.ZRequestParam;
import com.vo.scanner.ZControllerInterceptorScanner;
import com.vo.template.ZModel;
import com.vo.template.ZTemplate;
import com.vo.validator.FormPairParseException;
import com.vo.validator.PathVariableException;
import com.vo.validator.ZMin;
import com.vo.validator.ZPositive;
import com.vo.validator.ZValidated;
import com.vo.validator.ZValidator;
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
	private static final Map<Object, Object> CACHE_MAP = new WeakHashMap<>(1024, 1F);

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
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 *
	 */
	public ZResponse invoke(final ZRequest request) throws IllegalAccessException, InvocationTargetException {
		// 匹配path
		final RequestLine requestLine = request.getRequestLine();
		if (CollUtil.isEmpty(request.getLineList())) {
			return null;
		}

		final String path = requestLine.getPath();
		final Method method = ZControllerMap.getMethodByMethodEnumAndPath(requestLine.getMethodEnum(), path);

		// 查找对应的控制器来处理
		if (method == null) {
			return this.handleNoMethodMatche(request, requestLine, path);
		}

		try {
			final Object[] p = this.generateParameters(method, request, requestLine, path);
			if (p == null) {
				return null;
			}

			final Object zController = ZControllerMap.getObjectByMethod(method);
			final ZResponse re = this.invokeAndResponse(method, p, zController, request);
			return re;

		} catch (final Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			this.close();
		}

	}

	private ZResponse handleNoMethodMatche(final ZRequest request, final RequestLine requestLine, final String path) {
		final Map<MethodEnum, Method> methodMap = ZControllerMap.getByPath(path);
		if (CollUtil.isNotEmpty(methodMap)) {
			final String methodString = methodMap.keySet().stream().map(e -> e.getMethod()).collect(Collectors.joining(","));
			return new ZResponse(this.socketChannel)
				.header(ZRequest.ALLOW, methodString)
				.httpStatus(HttpStatus.HTTP_405.getCode())
				.contentType(HeaderEnum.JSON.getType())
				.body(J.toJSONString(CR.error(HttpStatus.HTTP_405.getCode(), "请求Method不支持："
						+ requestLine.getMethodEnum().getMethod() + ", Method: " + methodString), Include.NON_NULL));

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
					.body(J.toJSONString(CR.error(HTTP_STATUS_404, "请求方法不存在 [" + path+"]"), Include.NON_NULL))	;
	}

	public static String gExceptionMessage(final Throwable e) {

		if (Objects.isNull(e)) {
			return "";
		}

		final StringWriter stringWriter = new StringWriter();
		final PrintWriter writer = new PrintWriter(stringWriter);
		e.printStackTrace(writer);

		final String eMessage = stringWriter.toString();

		return eMessage;
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
		// 是否超过 ZRequestMapping.qps
		if (!ZServer.Counter.allow(controllerName + method.getName(), qps)) {

			final String message = "访问频繁，请稍后再试";

			final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
			response.contentType(HeaderEnum.JSON.getType())
					.httpStatus(HttpStatus.HTTP_403.getCode())
					.body(J.toJSONString(CR.error(message), Include.NON_NULL));

			return response;
		}

		// 是否超过 ZQPSLimitation.qps
		final ZQPSLimitation zqpsLimitation = ZControllerMap.getZQPSLimitationByControllerNameAndMethodName(controllerName,
				method.getName());
		if (zqpsLimitation != null) {

			switch (zqpsLimitation.type()) {
			case ZSESSIONID:
				final String keyword = controllerName
					+ "@" + method.getName()
					+ "@ZQPSLimitation" + '_'
					+ request.getSession().getId();
				if (!ZServer.Counter.allow(keyword, zqpsLimitation.qps())) {

					final String message = "接口访问频繁，请稍后再试";

					final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
					response.contentType(HeaderEnum.JSON.getType())
							.httpStatus(HttpStatus.HTTP_403.getCode())
							.body(J.toJSONString(CR.error(message), Include.NON_NULL));
					return response;
				}
				break;

			default:
				break;
			}
		}

		this.setZRequestAndZResponse(arraygP, request);

		if (Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			final Set<ZControllerInterceptor> zciSet = ZControllerInterceptorScanner.get();
			if (zciSet.isEmpty()) {

				final List<Class<? extends ZIAOP>> ziaopSubClassList = ZControllerMap.getZIAOPSubClassList(method);
				if (CollUtil.isNotEmpty(ziaopSubClassList)) {
					for (final Class<? extends ZIAOP> ziaop : ziaopSubClassList) {
						try {
							final ZIAOP newInstance = ziaop.newInstance();
							final AOPParameter parameter = new AOPParameter();
							parameter.setIsVOID(true);
							parameter.setTarget(zController);
							parameter.setMethodName(method.getName());
							parameter.setMethod(method);
							parameter.setParameterList(com.google.common.collect.Lists.newArrayList(arraygP));

							newInstance.before(parameter);
							newInstance.around(parameter);
							newInstance.after(parameter);


//							final com.vo.aop.AOPParameter parameter = new com.vo.aop.AOPParameter();
//							parameter.setIsVOID(false);
//							parameter.setTarget(com.vo.core.ZSingleton.getSingletonByClass(this.getClass().getSuperclass()));
//							parameter.setMethodName("index");
//							final java.lang.reflect.Method m = com.vo.aop.ZAOPScaner.cmap.get("com.vo.test.C2@index");
//							parameter.setMethod(m);
//							parameter.setParameterList(com.google.common.collect.Lists.newArrayList());
//							this.ziaop_index.before(parameter);
//							final Object v = this.ziaop_index.around(parameter);
//							this.ziaop_index.after(parameter);
//							return (com.votool.common.CR) v;

						} catch (InstantiationException | IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				} else {
					method.invoke(zController, arraygP);
				}

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
			final List<Class<? extends ZIAOP>> ziaopSubClassList = ZControllerMap.getZIAOPSubClassList(method);
			if (CollUtil.isNotEmpty(ziaopSubClassList)) {
				for (final Class<? extends ZIAOP> ziaop : ziaopSubClassList) {
					try {
						final ZIAOP newInstance = ziaop.newInstance();
						final AOPParameter parameter = new AOPParameter();
						parameter.setIsVOID(false);
						parameter.setTarget(zController);
						parameter.setMethodName(method.getName());
						parameter.setMethod(method);
						parameter.setParameterList(com.google.common.collect.Lists.newArrayList(arraygP));

						newInstance.before(parameter);
						r = newInstance.around(parameter);
						newInstance.after(parameter);


//						final com.vo.aop.AOPParameter parameter = new com.vo.aop.AOPParameter();
//						parameter.setIsVOID(false);
//						parameter.setTarget(com.vo.core.ZSingleton.getSingletonByClass(this.getClass().getSuperclass()));
//						parameter.setMethodName("index");
//						final java.lang.reflect.Method m = com.vo.aop.ZAOPScaner.cmap.get("com.vo.test.C2@index");
//						parameter.setMethod(m);
//						parameter.setParameterList(com.google.common.collect.Lists.newArrayList());
//						this.ziaop_index.before(parameter);
//						final Object v = this.ziaop_index.around(parameter);
//						this.ziaop_index.after(parameter);
//						return (com.votool.common.CR) v;

					} catch (InstantiationException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			} else {
//				method.invoke(zController, arraygP);
				r = method.invoke(zController, arraygP);
			}
		}

		// 响应
		if (isMethodAnnotationPresentZHtml(method)) {
			final String ss = String.valueOf(r);
			final String htmlName = ss.charAt(0) == '/' ? ss : '/' + ss;
			try {

				final String htmlContent = ResourcesLoader.loadStaticResourceString(htmlName);

				final String html = ZTemplate.freemarker(htmlContent);

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

		final String json = J.toJSONString(r, Include.NON_NULL);
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
		int zpvPI = 0;
		for (final Parameter p : ps) {
			if (p.isAnnotationPresent(ZRequestHeader.class)) {
				final ZRequestHeader a = p.getAnnotation(ZRequestHeader.class);
				final String name = a.value();
				final String headerValue = requestLine.getHeaderMap().get(name);
				if ((headerValue == null) && a.required()) {
					throw new FormPairParseException("请求方法[" + path + "]的header[" + p.getName() + "]不存在");
				}
				parametersArray[pI++] = headerValue;
			} else if (p.getType().getCanonicalName().equals(ZRequest.class.getCanonicalName())) {
				parametersArray[pI++] = request;
			} else if (p.getType().getCanonicalName().equals(ZResponse.class.getCanonicalName())) {
				final ZResponse response = new ZResponse(this.outputStream, this.socketChannel);
				parametersArray[pI++] = response;
			} else if (p.getType().getCanonicalName().equals(ZModel.class.getCanonicalName())) {
				final ZModel model = new ZModel();
				parametersArray[pI++] = model;
			} else if (p.isAnnotationPresent(ZRequestBody.class)) {
				final String body = request.getBody();
				final Object object = J.parseObject(body, p.getType());

				Task.checkZValidated(p, object);

				parametersArray[pI++] = object;

			} else if (p.isAnnotationPresent(ZRequestParam.class)) {
				pI = this.hZRequestParam(parametersArray, request, requestLine, path, pI, p);
			} else if (p.isAnnotationPresent(ZPathVariable.class)) {
				final List<Object> list = ZPVTL.get();
				final Class<?> type = p.getType();
				// FIXME 2023年11月8日 下午4:39:18 zhanghen: @ZRM 启动校验是否此类型
				try {
					final Object a = list.get(zpvPI);
					Task.setZPathVariableValue(parametersArray, pI, type, a);
					zpvPI++;


					// FIXME 2023年11月8日 下午10:47:54 zhanghen: TODO 继续支持 校验注解
					if (p.isAnnotationPresent(ZPositive.class)) {
						ZValidator.validatedZPositive(p, parametersArray[pI]);
					}
					if (p.isAnnotationPresent(ZMin.class)) {
						ZValidator.validatedZMin(p, parametersArray[pI],p.getAnnotation(ZMin.class).min());
					}


				} catch (final Exception e) {
					e.printStackTrace();
					// NumberFormatException
//					final String message = Task.gExceptionMessage(e);
					final String causedby = ZControllerAdviceThrowable.findCausedby(e);
					throw new PathVariableException(causedby);
				}

				pI++;
			} else if (p.getType().getCanonicalName().equals(ZMultipartFile.class.getCanonicalName())) {
				// FIXME 2023年10月26日 下午9:28:39 zhanghen: 写这里
				final String body = request.getBody();
				if (StrUtil.isEmpty(body)) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
				}
				final byte[] originalRequestBytes = request.getOriginalRequestBytes();

				final List<FormData> fdList = FormData.parseFormData(originalRequestBytes);
				if (CollUtil.isEmpty(fdList)) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
				}

				final Optional<FormData> findAny = fdList.stream()
						.filter(f -> StrUtil.isNotEmpty(f.getFileName()))
						.filter(f -> f.getName().equals(p.getName()))
						.findAny();
				if (!findAny.isPresent()) {
					throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
				}

				final ZMultipartFile file = new ZMultipartFile (findAny.get().getName(),
						findAny.get().getFileName(),
						findAny.get().getValue().getBytes(NioLongConnectionServer.CHARSET),
						findAny.get().getContentType(), null);

				pI = Task.setValue(parametersArray, pI, p, file);
			}

		}

		return parametersArray;
	}

	/**
	 * @ZPathVariable 支持的类型
	 */
	public final static ImmutableSet<String> ZPV_TYPE = ImmutableSet.copyOf(Lists.newArrayList(
			Byte.class.getCanonicalName(), Short.class.getCanonicalName(), Integer.class.getCanonicalName(),
			Long.class.getCanonicalName(), Float.class.getCanonicalName(), Double.class.getCanonicalName(),
			Boolean.class.getCanonicalName(), Character.class.getCanonicalName(), String.class.getCanonicalName()));

	private static void setZPathVariableValue(final Object[] parametersArray, final int pI, final Class<?> type, final Object a) {
		if (type.getCanonicalName().equals(Byte.class.getCanonicalName())) {
			parametersArray[pI] = Byte.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Short.class.getCanonicalName())) {
			parametersArray[pI] = Short.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Integer.class.getCanonicalName())) {
			parametersArray[pI] = Integer.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Long.class.getCanonicalName())) {
			parametersArray[pI] = Long.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Float.class.getCanonicalName())) {
			parametersArray[pI] = Float.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Double.class.getCanonicalName())) {
			parametersArray[pI] = Double.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			parametersArray[pI] = Boolean.valueOf(String.valueOf(a));
		} else if (type.getCanonicalName().equals(Character.class.getCanonicalName())) {
			parametersArray[pI] = Character.valueOf(String.valueOf(a).charAt(0));
		}

		else if (type.getCanonicalName().equals(String.class.getCanonicalName())) {
			parametersArray[pI] = String.valueOf(a);
		}
	}

	private int hZRequestParam(final Object[] parametersArray, final ZRequest request, final RequestLine requestLine,
			final String path, int pI, final Parameter p) {
		final Set<RequestParam> paramSet = requestLine.getParamSet();
		if (CollUtil.isNotEmpty(paramSet)) {
			final Optional<RequestParam> findAny = paramSet.stream()
					.filter(rp -> rp.getName().equals(p.getName()))
					.findAny();
			if (!findAny.isPresent()) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			pI = Task.setValue(parametersArray, pI, p, findAny.get().getValue());
		} else {
			final String body = request.getBody();
			if (StrUtil.isEmpty(body)) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}
			final byte[] originalRequestBytes = request.getOriginalRequestBytes();

			final List<FormData> fdList = FormData.parseFormData(originalRequestBytes);
			if (CollUtil.isEmpty(fdList)) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			final Optional<FormData> findAny = fdList.stream()
					.filter(f -> StrUtil.isEmpty(f.getFileName()))
					.filter(f -> f.getName().equals(p.getName()))
					.findAny();
			if (!findAny.isPresent()) {
				throw new FormPairParseException("请求方法[" + path + "]的参数[" + p.getName() + "]不存在");
			}

			pI = Task.setValue(parametersArray, pI, p, findAny.get().getValue());
		}
		return pI;
	}

	private static void checkZValidated(final Parameter p, final Object object) {
		if (!p.isAnnotationPresent(ZValidated.class)) {
			return;
		}

		final Class<?> type = p.getType();
		final Field[] fields = type.getDeclaredFields();
		for (final Field field : fields) {
			ZValidator.validatedAll(object, field);
		}

	}

	private static int setValue(final Object[] parametersArray, final int pI, final Parameter p, final Object value) {

		final AtomicInteger nI = new AtomicInteger(pI);
		if (p.getType().getCanonicalName().equals(Byte.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Byte.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Short.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Short.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Integer.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Integer.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Long.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Long.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Float.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Float.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Double.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Double.valueOf(String.valueOf(value));
		} else if (p.getType().getCanonicalName().equals(Character.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Character.valueOf(String.valueOf(value).charAt(0));
		} else if (p.getType().getCanonicalName().equals(Boolean.class.getCanonicalName())) {
			parametersArray[nI.getAndIncrement()] = Boolean.valueOf(String.valueOf(value));
		} else {
			parametersArray[nI.getAndIncrement()] = value;
		}

		return nI.get();
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


	public static Boolean isMethodAnnotationPresentZHtml(final Method method) {
		final String name = method.getName();
		final Boolean boolean1 = (Boolean) CACHE_MAP.get(name);
		if (boolean1 != null) {
			return boolean1;
		}

		synchronized (name.intern()) {

			final boolean annotationPresent = method.isAnnotationPresent(ZHtml.class);
			CACHE_MAP.put(name, annotationPresent);
			return annotationPresent;
		}
	}

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
		for (int i = x.size() - 1; i > 0; i--) {
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

//				System.out.println("contentType = " + contentType);

				if (contentType.equalsIgnoreCase(HeaderEnum.JSON.getType())
						|| contentType.toLowerCase().contains(HeaderEnum.JSON.getType().toLowerCase())) {

					final StringBuilder json = new StringBuilder();
					for (int k = i + 1; k < x.size(); k++) {
						json.append(x.get(k));
					}
					request.setBody(json.toString());
				} else if (contentType.equalsIgnoreCase(HeaderEnum.URLENCODED.getType())
						|| contentType.toLowerCase().contains(HeaderEnum.URLENCODED.getType().toLowerCase())) {

//					System.out.println("contentType = " + contentType);
					System.out.println("OKapplication/x-www-form-urlencoded");
					// id=200&name=zhangsan 格式

					final StringBuilder formBu = new StringBuilder();
					for (int k = i + 1; k < x.size(); k++) {
						formBu.append(x.get(k));
					}

					if (formBu.length() > 0) {
						request.setBody(formBu.toString());

						System.out.println("from = " + formBu);
						final String fr = formBu.toString();
						final String[] fA = fr.split("&");

						for (final String v : fA) {
							final String[] vA = v.split("=");
						}

					}

					// FORM_DATA 用getType
				} else if (contentType.toLowerCase().startsWith(HeaderEnum.FORM_DATA.getType().toLowerCase())) {

					// FIXME 2023年8月11日 下午10:19:34 zhanghen: TODO 继续支持 multipart/form-data
//					System.out.println("okContent-Type: multipart/form-data");

					final ArrayList<String> body = Lists.newArrayList();
					final StringBuilder formBu = new StringBuilder();
					for (int k = i + 1; k < x.size(); k++) {
						formBu.append(x.get(k)).append(Task.NEW_LINE);
						body.add(x.get(k));
					}


					request.setBody(formBu.toString());
//					System.out.println("formBu = \n" + formBu);

					final List<FormData> formList = FormData.parseFormData(body.toArray(new String[0]), body.get(0));
//					System.out.println("---------formList.size = " + formList.size());
					for (final FormData form: formList) {
//						System.out.println(form);
					}
//					System.out.println("---------formList.size = " + formList.size());
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
