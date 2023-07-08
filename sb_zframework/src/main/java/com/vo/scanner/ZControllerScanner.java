package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZController;
import com.vo.api.StaticController;
import com.vo.conf.ServerConfiguration;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZResponse;
import com.vo.core.ZSingleton;
import com.vo.enums.BeanModeEnum;
import com.vo.enums.MethodEnum;
import com.vo.http.ZConnect;
import com.vo.http.ZControllerMap;
import com.vo.http.ZDelete;
import com.vo.http.ZGet;
import com.vo.http.ZHead;
import com.vo.http.ZHtml;
import com.vo.http.ZOptions;
import com.vo.http.ZPatch;
import com.vo.http.ZPost;
import com.vo.http.ZPut;
import com.vo.http.ZRequestMapping;
import com.vo.http.ZTrace;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 扫描 @ZController 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZControllerScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	@SuppressWarnings("unchecked")
	public static final HashSet<Class<? extends Annotation>> HTTP_METHOD_SET = Sets.newHashSet(ZGet.class, ZPost.class,
			ZPut.class, ZDelete.class, ZTrace.class, ZOptions.class, ZHead.class, ZConnect.class, ZPatch.class);

	public static Set<Class<?>> scanAndCreateObject(final String packageName) {
		ZControllerScanner.LOG.info("开始扫描带有[{}]的类", ZController.class.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(packageName, ZController.class);
		ZControllerScanner.LOG.info("带有[{}]的类个数={}", ZController.class.getCanonicalName(), zcSet.size());

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);

		for (final Class<?> cls : zcSet) {
			final Boolean staticControllerEnable = serverConfiguration.getStaticControllerEnable();
			if (Boolean.FALSE.equals(staticControllerEnable)
				&& cls.getCanonicalName().equals(StaticController.class.getCanonicalName())) {

				ZControllerScanner.LOG.info("[{}] 未启用，不创建[{}]对象",
						StaticController.class.getSimpleName(),StaticController.class.getSimpleName()
						);

				continue;
			}

			final Object newZController1 = ZObjectGeneratorStarter.generate(cls);

			LOG.info("带有[{}]的类[{}]创建对象[{}]完成", ZController.class.getCanonicalName(), cls.getCanonicalName(),
					newZController1);
//			ZConMap.putBean(cls.getCanonicalName(), newZController1);

			ZContext.addBean(cls.getCanonicalName(), newZController1);

			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {

				ZControllerScanner.checkZHtml(method);

				checkNoVoidWithZResponse(method);

				final Object controllerObject = ZControllerScanner.getSingleton(cls);

				// 接口上是否有 @ZRequestMapping
				final ZRequestMapping requestMappingAnnotation = method.getAnnotation(ZRequestMapping.class);
				if (requestMappingAnnotation != null) {
					final String[] requestMappingArray = requestMappingAnnotation.mapping();

					ZControllerScanner.checkMapping(method, requestMappingAnnotation, requestMappingArray);

					final boolean[] isRegex = requestMappingAnnotation.isRegex();
					for (int i = 0; i < requestMappingArray.length; i++) {
						final String mapping = requestMappingArray[i];
						final MethodEnum methodEnum = requestMappingAnnotation.method();
						ZControllerMap.put(methodEnum, mapping, method, controllerObject, isRegex[i]);
					}
				}

				final ZGet get = method.getAnnotation(ZGet.class);
				if (get != null) {
					ZControllerScanner.checkPathVariable(get.path(), method);
					ZControllerMap.put(MethodEnum.GET, get.path() , method, controllerObject, false);
				}

				final ZPost post = method.getAnnotation(ZPost.class);
				if (post != null) {
					ZControllerMap.put(MethodEnum.POST, post.path() , method, controllerObject, false);
				}
				final ZPut put = method.getAnnotation(ZPut.class);
				if (put != null) {
					ZControllerMap.put(MethodEnum.PUT, put.path() , method, controllerObject, false);
				}
				final ZDelete delete = method.getAnnotation(ZDelete.class);
				if (delete != null) {
					ZControllerMap.put(MethodEnum.DELETE, delete.path() , method, controllerObject, false);
				}
				final ZHead head = method.getAnnotation(ZHead.class);
				if (head != null) {
					ZControllerMap.put(MethodEnum.HEAD, head.path() , method, controllerObject, false);
				}
				final ZConnect connect = method.getAnnotation(ZConnect.class);
				if (connect != null) {
					ZControllerMap.put(MethodEnum.CONNECT, connect.path() , method, controllerObject, false);
				}
				final ZTrace trace = method.getAnnotation(ZTrace.class);
				if (trace != null) {
					ZControllerMap.put(MethodEnum.TRACE, trace.path() , method, controllerObject, false);
				}
				final ZOptions options = method.getAnnotation(ZOptions.class);
				if (options != null) {
					ZControllerMap.put(MethodEnum.OPTIONS, options.path() , method, controllerObject, false);
				}
				final ZPatch patch = method.getAnnotation(ZPatch.class);
				if (patch != null) {
					ZControllerMap.put(MethodEnum.PATCH, patch.path() , method, controllerObject, false);
				}
			}

		}

		return zcSet;
	}

	private static void checkNoVoidWithZResponse(final Method method) {
		if (!Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			final Parameter[] ps = method.getParameters();
			final Optional<Parameter> ro = Lists.newArrayList(ps).stream()
					.filter(p -> p.getType().getCanonicalName().equals(ZResponse.class.getCanonicalName()))
					.findAny();
			if (ro.isPresent()) {
				throw new IllegalArgumentException(
						"接口方法 " + method.getName() + " 带返回值不允许使用 " + ZResponse.class.getSimpleName() + " 参数，去掉 "
								+ ZResponse.class.getSimpleName() + " 参数，或者返回值改为 void");
			}
		}
	}

	private static void checkMapping(final Method method, final ZRequestMapping requestMappingAnnotation,
			final String[] requestMappingArray) {

		if (ArrayUtil.isEmpty(requestMappingArray)) {
			throw new IllegalArgumentException("接口方法 " + method.getName() + " mapping值不能为空");
		}

		final Set<String> temp = Sets.newHashSet();

		for (final String requestMapping : requestMappingArray) {

			if (StrUtil.isEmpty(requestMapping)) {
				throw new IllegalArgumentException("接口方法 " + method.getName() + " mapping值不能为空");
			}

			ZControllerScanner.checkRequestMapping(method, requestMapping);

			final boolean add = temp.add(requestMapping);
			if (!add) {
				throw new IllegalArgumentException(
						"接口方法 " + method.getName() + " mapping值不能重复,mapping = " + Arrays.toString(requestMappingArray));
			}
		}

		final boolean[] isRegex = requestMappingAnnotation.isRegex();
		if (!ArrayUtil.isEmpty(isRegex) && isRegex.length != requestMappingArray.length) {
			throw new IllegalArgumentException("接口方法 " + method.getName() + " isRegex个数必须与mapping值个数 相匹配, isRegex个数 = "
					+ isRegex.length + " mapping个数 = " + requestMappingArray.length);
		}
	}

	/**
	 * 校验requestMapping,必须以且只以一个/开头
	 *
	 * @param method
	 * @param requestMapping
	 *
	 */
	private static void checkRequestMapping(final Method method, final String requestMapping) {
		if (requestMapping.charAt(0) != '/') {
			throw new IllegalArgumentException("接口方法 " + method.getName() + " mapping值必须以/开始,method = "
					+ method.getName() + " requestMapping = " + requestMapping);
		}

		if (requestMapping.length() <= 1) {
			return;
		}

		final char charAt = requestMapping.charAt(1);
		if (charAt == '/') {
			throw new IllegalArgumentException("接口方法 " + method.getName() + " mapping值必须以/开始,method = "
					+ method.getName() + " requestMapping = " + requestMapping);
		}

	}

	private static void checkPathVariable(final String path, final Method method) {
		if (path.charAt(0) != '/') {
//			throw new IllegalArgumentException("path值必须以/开头,method = " + method.getName());
			throw new IllegalArgumentException("path值必须以/开头");
		}

		final String[] pa = path.split("/");
		System.out.println("pa = " + Arrays.toString(pa));
		final HashSet<String> pvSet = Sets.newLinkedHashSet();
		final ArrayList<String> pvList = Lists.newArrayList();
		for (final String s : pa) {
			if (StrUtil.isEmpty(s)) {
				continue;
			}

			if (s.charAt(0) != '{' && s.charAt(s.length() - 1) != '}') {
				continue;
			}

			if (((s.charAt(0) == '{') && (s.charAt(s.length() - 1) != '}'))
					|| ((s.charAt(0) != '{') && (s.charAt(s.length() - 1) == '}'))) {
				throw new IllegalArgumentException("path 中可变量必须用{}包括起来,path = " + path);
			}

			System.out.println("pv = " + s);
			pvSet.add(s);
			pvList.add(s);
		}

		if (pvSet.isEmpty()) {
			return;
		}

		final Parameter[] ps = method.getParameters();
		if (ArrayUtil.isEmpty(ps) || ps.length < pvList.size()) {
			throw new IllegalArgumentException(
					"接口方法参数与@" + ZPathVariable.class.getName() + " 个数不匹配,method = " + method.getName());
		}

		for (int i = 0; i < pvList.size(); i++) {
			final Parameter parameter = ps[i];
			final ZPathVariable zPathVariable = parameter.getAnnotation(ZPathVariable.class);
			if (zPathVariable == null) {
				throw new IllegalArgumentException("接口方法 " + method.getName() + " 第 " + (i + 1) + " 个参数必须是 @" + ZPathVariable.class.getSimpleName() + " 参数");
			}
			if (("{" + zPathVariable.name() + "}").equals(pvList.get(i))
				||	 ("{" + parameter.getName()  + "}").equals(pvList.get(i))
					) {

				System.out.println("pvList 匹配一个 = " + pvList.get(i));

			} else {
				throw new IllegalArgumentException("接口方法参数顺序必须与 @" + ZPathVariable.class.getName() + " 顺序保持一致");
			}

		}

		 int p = 0;
		for (final Parameter parameter : ps) {
			final boolean annotationPresent = parameter.isAnnotationPresent(ZPathVariable.class);
			if (annotationPresent) {
				final List<String> collect = pvSet.stream()
						.filter(pv -> ("{" + parameter.getAnnotation(ZPathVariable.class).name() + "}").equals(pv)
								|| ("{" +parameter.getName() + "}").equals(pv))
						.collect(Collectors.toList());
				if (collect.size() != 1) {
					throw new IllegalArgumentException(
							"path 中的可变量必须与" + "@" + ZPathVariable.class.getName() + "变量无法匹配");
				}
				p++;
			}
		}
		if (pvSet.size() != p) {
			throw new IllegalArgumentException(
					"path中的可变量[" + path + "]必须声明为 @" + ZPathVariable.class.getName() + "标记的参数");
		}

	}

	private static void checkZHtml(final Method method) {
		if (ZControllerScanner.isHttpMethod(method) && method.isAnnotationPresent(ZHtml.class)) {
			final Class<?> rCls = method.getReturnType();
			final boolean isS = rCls.getCanonicalName().equals(String.class.getCanonicalName());
			if (!isS) {
				throw new IllegalArgumentException(
						"@" + ZHtml.class.getCanonicalName() + "标记的http接口的返回值必须是String : methodName = " + method.getName());
			}
		}
	}

	private static boolean isHttpMethod(final Method method) {
		for (final Class<? extends Annotation> c : ZControllerScanner.HTTP_METHOD_SET) {
			if (method.isAnnotationPresent(c)) {
				return true;
			}
		}

		return false;
	}

	public static Object getSingleton(final Class<?> zcClass) {
		final ZController zc = zcClass.getAnnotation(ZController.class);
		final BeanModeEnum modeEnum = zc.modeEnum();

		switch (modeEnum) {
		case SINGLETON:
			final Object singletonByClass = ZSingleton.getSingletonByClass(zcClass);
			return singletonByClass;

		default:
			break;
		}

		return null;
	}
}
