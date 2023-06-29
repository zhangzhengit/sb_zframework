package com.vo.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.Socket;
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
import java.util.concurrent.ConcurrentMap;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
import com.vo.core.HRequest.RequestLine;
import com.vo.core.HRequest.RequestParam;
import com.vo.enums.MethodEnum;
import com.vo.html.ResourcesLoader;
import com.vo.http.LineMap;
//import com.vo.http.ZControllerMap;
import com.vo.http.ZControllerMap;
import com.vo.http.ZHtml;
import com.vo.template.ZModel;
import com.vo.template.ZTemplate;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.URLDecoder;
import cn.hutool.core.util.StrUtil;

/**
 * 读取/响应的一个任务对象
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@SuppressWarnings("ucd")
public class Task {

	private static final String UTF_8 = "UTF-8";
	private static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
	public static final int HTTP_STATUS_404 = 404;
	public static final int HTTP_STATUS_500 = 500;

	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private static final int READ_LENGTH = DEFAULT_BUFFER_SIZE / 2;
	public static final ContentTypeEnum DEFAULT_CONTENT_TYPE = ContentTypeEnum.JSON;

	public static final String EMPTY_STRING = "";

	public static final String VOID = "void";

	private static final ConcurrentMap<Object, Object> CACHE_MAP = Maps.newConcurrentMap();

	public static final String SERVER = "Server:sb_zframework";
	public static final String HTTP_500 = "HTTP/1.1 " + HTTP_STATUS_500;
	public static final String HTTP_404 = "HTTP/1.1 " + HTTP_STATUS_404;

	public static final String OK_200 = "HTTP/1.1 200";

	public static final String NEW_LINE = "\r\n";

	public static final String SP = "&";

	public static final Charset UTF_8_CHARSET = Charset.forName(UTF_8);

	private final Socket socket;
	private  InputStream inputStream;
	private BufferedInputStream bufferedInputStream;


	@SuppressWarnings("boxing")
	public Task(final Socket socket) {
		this.socket = socket;
		try {
			this.inputStream = this.socket.getInputStream();
			this.bufferedInputStream = new BufferedInputStream(this.inputStream, DEFAULT_BUFFER_SIZE);
		} catch (final IOException e1) {
			e1.printStackTrace();
		}

		final HRequest request = this.handleRead();

		// 开始解析
		final RequestLine requestLine = Task.parseRequest(request);

		// 匹配path
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
						ZMappingRegex.set(java.net.URLDecoder.decode(path, UTF_8));
						this.invokeAndResponse(methodTarget, arraygP, object);
						return;
					} catch (IllegalAccessException | InvocationTargetException | UnsupportedEncodingException e) {
						e.printStackTrace();
					}

				}
			}

			Task.handleWrite404(DEFAULT_CONTENT_TYPE, CR.error(HTTP_STATUS_404, "请求方法不存在 [" + path+"]"), socket);
			return;
		}

		try {

			final Object[] p = this.generateParameters(method, request, requestLine, path);
			final Object zController = ZControllerMap.getObjectByMethod(method);
			this.invokeAndResponse(method, p, zController);

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			final CR<Object> error = CR.error(HTTP_STATUS_500, INTERNAL_SERVER_ERROR);
			Task.handleWrite500(DEFAULT_CONTENT_TYPE, error, socket);
			e.printStackTrace();
		} finally {
			this.closeSocketAndStream();
		}

	}

	/**
	 * 返回两个字符串匹配的最大长度的前缀
	 *
	 * @param s1
	 * @param s2
	 * @return
	 *
	 */
	private static String samePrefix(final String s1, final String s2) {
		if (Objects.equals(s1, s2)) {
			return s1;
		}

		final int min = Math.min(s1.length(), s2.length());
		for (int i = 0; i < min; i++) {
			final char c1 = s1.charAt(i);
			final char c2 = s2.charAt(i);
			if (c1 != c2) {
				return s1.substring(0,i);
			}
		}

		return null;
	}

	private void closeSocketAndStream() {
		try {
			this.inputStream.close();
			this.bufferedInputStream.close();
			this.socket.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void invokeAndResponse(final Method method, final Object[] arraygP, final Object zController)
			throws IllegalAccessException, InvocationTargetException {
		if (VOID.equals(method.getReturnType().getCanonicalName())) {
			method.invoke(zController, arraygP);
			return;
		}

		final Object r = method.invoke(zController, arraygP);
		// 响应
		if (isMethodAnnotationPresentZHtml(method)) {
			final String ss = String.valueOf(r);
			final String htmlName = ss.charAt(0) == '/' ? ss : '/' + ss;
			try {
				final String htmlContent = ResourcesLoader.loadString(htmlName);

				final String html = ZTemplate.generate(htmlContent);
				this.handleWrite(ContentTypeEnum.HTML, html);
			} catch (final Exception e) {
				Task.handleWrite500(DEFAULT_CONTENT_TYPE, CR.error(HTTP_STATUS_500, INTERNAL_SERVER_ERROR), this.socket);
				e.printStackTrace();
			}
		} else {
			final String response = JSON.toJSONString(r);
			this.handleWrite(DEFAULT_CONTENT_TYPE, response);
		}
	}

	private Object[] generateParameters(final Method method, final Object[] parametersArray, final HRequest request,
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
					Task.handleWrite404(DEFAULT_CONTENT_TYPE, CR.error(HTTP_STATUS_404, "请求方法[" + path + "]的header[" + name + "]不存在"), this.socket);
					return null;
				}
				parametersArray[pI++] = headerValue;
			} else if (p.getType().getCanonicalName().equals(HRequest.class.getCanonicalName())) {
				parametersArray[pI++] = request;
			} else if (p.getType().getCanonicalName().equals(HResponse.class.getCanonicalName())) {
				try {
					final HResponse hResponse = new HResponse(this.getOS());
					parametersArray[pI++] = hResponse;
				} catch (final IOException e) {
					e.printStackTrace();
				}
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
					Task.handleWrite404(DEFAULT_CONTENT_TYPE, CR.error(HTTP_STATUS_404, "请求方法[" + path + "]的参数[" + p.getName() + "]不存在"), this.socket);
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

	private Object[] generateParameters(final Method method, final HRequest request, final RequestLine requestLine, final String path) {
		final Object[] parametersArray = new Object[method.getParameters().length];

		return this.generateParameters(method, parametersArray, request, requestLine, path);
	}

	private OutputStream getOS() throws IOException {

		final Object key = this;

		final Object os = cacheMap.get(key);
		if (os != null) {
			return (OutputStream) os;
		}

		synchronized (key) {
			final OutputStream os2 = this.socket.getOutputStream();
			cacheMap.put(key, os2);
			return os2;
		}

	}

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

	static Map<Object, Object> cacheMap = new WeakHashMap<>(1024, 1F);

	private static String gFullName(final RequestLine requestLine, final String path) {
		String fullName = requestLine.getPath();
		final Set<RequestParam> ps = requestLine.getParamSet();
		if (CollUtil.isNotEmpty(ps)) {
			for (final RequestParam rp : ps) {
				final String name = rp.getName();
				fullName = fullName + "@" + name;
			}
		}
		return fullName;
	}

	public static RequestLine parseRequest(final HRequest request) {
		final Object v = CACHE_MAP.get(request);
		if (v != null) {
			return (RequestLine) v;
		}

		synchronized (request) {
			final RequestLine v2 = parseRequest0(request);
			CACHE_MAP.put(request, v2);
			return v2;
		}
	}

	private static RequestLine parseRequest0(final HRequest request) {
		final HRequest.RequestLine requestLine = new HRequest.RequestLine();
		if (CollUtil.isEmpty(request.getLineList())) {
			return requestLine;
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

		return requestLine;
	}

	private static void parseBody(final HRequest request, final HRequest.RequestLine requestLine) {
		final List<String> x = request.getLineList();
		for (int i = 1; i < x.size(); i++) {
			final String l2 = x.get(i);
			if (EMPTY_STRING.equals(l2) && (i < x.size()) && i + 1 < x.size()) {

				final String contentType = requestLine.getHeaderMap().get(HRequest.CONTENT_TYPE);
				if (contentType.equalsIgnoreCase(ContentTypeEnum.JSON.getType())
						|| contentType.toLowerCase().contains(ContentTypeEnum.JSON.getType().toLowerCase())) {

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

	private static void paserHeader(final HRequest request, final HRequest.RequestLine requestLine) {
		final List<String> x = request.getLineList();
		final HashMap<String, String> hm = Maps.newHashMap();
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

	private static void parseVersion(final HRequest.RequestLine requestLine, final String line) {
		final int hI = line.lastIndexOf("HTTP/");
		if (hI > -1) {
			final String version = line.substring(hI);
			requestLine.setVersion(version);
		}
	}

	private static void parsePath(final String s, final HRequest.RequestLine line, final int methodIndex) {
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
					final HRequest.RequestParam requestParam  = new HRequest.RequestParam();
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

	private HRequest handleRead() {

		final HRequest request = new HRequest();

		try {

			final int nk = READ_LENGTH;
			final List<Byte> list = new ArrayList<>(nk);

			while (true) {
				final byte[] bs = new byte[nk];
				final int read = this.bufferedInputStream.read(bs);
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

//			bufferedInputStream.close();
//			inputStream.close();
		} catch (final IOException | IllegalArgumentException e) {
			e.printStackTrace();
		}

		return request;

	}

	private void handleWrite(final ContentTypeEnum contentTypeEnum, final String response) {
		try {
			final String content = response;

			final OutputStream outputStream = this.getOS();

			final PrintWriter pw = new PrintWriter(outputStream);
			this.write0(contentTypeEnum, content, pw);

			pw.flush();
			outputStream.flush();
			outputStream.close();
			pw.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private void write0(final ContentTypeEnum contentTypeEnum, final String content, final PrintWriter pw) {
		final String c = OK_200 + NEW_LINE + contentTypeEnum.getValue() + NEW_LINE + SERVER + NEW_LINE + NEW_LINE + content + NEW_LINE;
		pw.write(c);
	}

	private static void handleWrite500(final ContentTypeEnum contentTypeEnum, final CR cr, final Socket socket) {
		try {
			final String json = JSON.toJSONString(cr);

			final OutputStream outputStream = socket.getOutputStream();
//			final OutputStream outputStream = Task.this.socket.getOutputStream();

			final PrintWriter pw = new PrintWriter(outputStream);

			final String s = HTTP_500 + NEW_LINE + contentTypeEnum.getValue() + NEW_LINE + SERVER + NEW_LINE + NEW_LINE
					+ json + NEW_LINE;

			pw.write(s);

			pw.flush();
			pw.close();
			outputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static void handleWrite404(final ContentTypeEnum contentTypeEnum, final CR cr, final Socket socket) {
		try {

			final String json = JSON.toJSONString(cr);
			final OutputStream outputStream = socket.getOutputStream();

			final PrintWriter pw = new PrintWriter(outputStream);
			final String s = HTTP_404 + NEW_LINE + contentTypeEnum.getValue() + NEW_LINE + SERVER + NEW_LINE + NEW_LINE
					+ json + NEW_LINE;
			pw.write(s);

			pw.flush();
			pw.close();
			outputStream.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
