package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZController;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZSingleton;
import com.vo.enums.BeanModeEnum;
import com.vo.enums.MethodEnum;
import com.vo.http.ZConMap;
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
import cn.hutool.core.util.ClassUtil;
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
		LOG.info("开始扫描带有[{}]的类", ZController.class.getCanonicalName());
		final Set<Class<?>> zcSet = ClassUtil.scanPackageByAnnotation(packageName, ZController.class);
		LOG.info("带有[{}]的类个数={}", ZController.class.getCanonicalName(), zcSet.size());

		for (final Class<?> cls : zcSet) {

			final Object newZController1 = ZObjectGeneratorStarter.generate(cls);

			LOG.info("带有[{}]的类[{}]创建对象[{}]完成", ZController.class.getCanonicalName(), cls.getCanonicalName(),
					newZController1);
			ZConMap.putBean(cls.getCanonicalName(), newZController1);


			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {

				checkZHtml(method);

				final Object newZController = getSingleton(cls);


				final ZRequestMapping requestMapping = method.getAnnotation(ZRequestMapping.class);
				if (requestMapping != null) {
					final String[] rmArray = requestMapping.mapping();
					if (ArrayUtil.isEmpty(rmArray)) {
						throw new IllegalArgumentException("接口方法 " + method.getName() + " mapping值不能为空");
					}

					final HashSet<String> temp = Sets.newHashSet();
					for (final String rm : rmArray) {

						if (StrUtil.isEmpty(rm)) {
							throw new IllegalArgumentException("接口方法 " + method.getName() + " mapping值不能为空");
						}

						if (rm.charAt(0) != '/') {
							throw new IllegalArgumentException(
									"接口方法 " + method.getName() + " mapping值必须以/开始,method = " + method.getName());
						}

						// FIXME 2023年6月28日 下午10:26:17 zhanghen: TODO 校验 [///user] 的情况，必须以且只以一个/开头

						final boolean add = temp.add(rm);
						if (!add) {
							throw new IllegalArgumentException(
									"接口方法 " + method.getName() + " mapping值不能重复,mapping = " + Arrays.toString(rmArray));
						}
					}

					final boolean[] isRA = requestMapping.isRegex();
					if (!ArrayUtil.isEmpty(isRA) && isRA.length != rmArray.length) {
						throw new IllegalArgumentException(
								"接口方法 " + method.getName() + " isRegex个数必须与mapping值个数 相匹配, isRA个数 = " + isRA.length
										+ " mapping个数 = " + rmArray.length);
					}

					for (int i = 0; i < rmArray.length; i++) {
						final String mapping = rmArray[i];
						final MethodEnum methodEnum = requestMapping.method();
						ZControllerMap.put(methodEnum, mapping, method, newZController, isRA[i]);
					}
				}

				final ZGet get = method.getAnnotation(ZGet.class);
				if (get != null) {
					checkPathVariable(get.path(), method);
					ZControllerMap.put(MethodEnum.GET, get.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.GET.getMethod() + "@" + get.path(), method, newZController);
				}

				final ZPost post = method.getAnnotation(ZPost.class);
				if (post != null) {
					ZControllerMap.put(MethodEnum.POST, post.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.POST.getMethod() + "@" + post.path(), method, newZController);
				}
				final ZPut put = method.getAnnotation(ZPut.class);
				if (put != null) {
					ZControllerMap.put(MethodEnum.PUT, put.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.PUT.getMethod() + "@" + post.path(), method, newZController);
				}
				final ZDelete delete = method.getAnnotation(ZDelete.class);
				if (delete != null) {
					ZControllerMap.put(MethodEnum.DELETE, delete.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.DELETE.getMethod() + "@" + post.path(), method, newZController);
				}
				final ZHead head = method.getAnnotation(ZHead.class);
				if (head != null) {
					ZControllerMap.put(MethodEnum.HEAD, head.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.HEAD.getMethod() + "@" + post.path(), method, newZController);
				}
				final ZConnect connect = method.getAnnotation(ZConnect.class);
				if (connect != null) {
					ZControllerMap.put(MethodEnum.CONNECT, connect.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.CONNECT.getMethod() + "@" + post.path(), method, newZController);
				}
				final ZTrace trace = method.getAnnotation(ZTrace.class);
				if (trace != null) {
					ZControllerMap.put(MethodEnum.TRACE, trace.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.TRACE.getMethod() + "@" + post.path(), method, newZController);
				}
				final ZOptions options = method.getAnnotation(ZOptions.class);
				if (options != null) {
					ZControllerMap.put(MethodEnum.OPTIONS, options.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.OPTIONS.getMethod() + "@" + post.path(), method, newZController);
				}
				final ZPatch patch = method.getAnnotation(ZPatch.class);
				if (patch != null) {
					ZControllerMap.put(MethodEnum.PATCH, patch.path() , method, newZController, false);
//					ZControllerMap.putBean(MethodEnum.PATCH.getMethod() + "@" + post.path(), method, newZController);
				}
			}

		}

		return zcSet;
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
		if (isHttpMethod(method) && method.isAnnotationPresent(ZHtml.class)) {
			final Class<?> rCls = method.getReturnType();
			final boolean isS = rCls.getCanonicalName().equals(String.class.getCanonicalName());
			if (!isS) {
				throw new IllegalArgumentException(
						"@" + ZHtml.class.getCanonicalName() + "标记的http接口的返回值必须是String : methodName = " + method.getName());
			}
		}
	}

	private static boolean isHttpMethod(final Method method) {
		for (final Class<? extends Annotation> c : HTTP_METHOD_SET) {
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
