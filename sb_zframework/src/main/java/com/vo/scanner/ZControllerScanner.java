package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZController;
import com.vo.aop.ZAOP;
import com.vo.aop.ZIAOP;
import com.vo.api.StaticController;
import com.vo.conf.ServerConfiguration;
import com.vo.core.Task;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZPathVariable;
import com.vo.core.ZResponse;
import com.vo.core.ZSingleton;
import com.vo.enums.BeanModeEnum;
import com.vo.enums.MethodEnum;
import com.vo.http.ZControllerMap;
import com.vo.http.ZHtml;
import com.vo.http.ZRequestMapping;
import com.vo.validator.StartupException;

import cn.hutool.core.collection.CollUtil;
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
	private static final HashSet<Class<? extends Annotation>> HTTP_METHOD_SET = Sets.newHashSet(ZRequestMapping.class);

	public static Set<Class<?>> scanAndCreateObject(final String... packageName) {
		ZControllerScanner.LOG.info("开始扫描带有[{}]的类", ZController.class.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(ZController.class, packageName);
		ZControllerScanner.LOG.info("带有[{}]的类个数={}", ZController.class.getCanonicalName(), zcSet.size());

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);

		for (final Class<?> cls : zcSet) {
			final Boolean staticControllerEnable = serverConfiguration.getStaticControllerEnable();
			if (Boolean.FALSE.equals(staticControllerEnable)
				&& cls.getCanonicalName().equals(StaticController.class.getCanonicalName())) {

				ZControllerScanner.LOG.info("[{}] 未启用，不创建[{}]对象", StaticController.class.getSimpleName(),
						StaticController.class.getSimpleName());

				continue;
			}

			final Object newZController1 = ZObjectGeneratorStarter.generate(cls);

			LOG.info("带有[{}]的类[{}]创建对象[{}]完成", ZController.class.getCanonicalName(), cls.getCanonicalName(),
					newZController1);

			ZContext.addBean(cls.getCanonicalName(), newZController1);

			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {
				ZControllerScanner.checkZHtml(method);

				checkNoVoidWithZResponse(method);

				final Object controllerObject = ZControllerScanner.getSingleton(cls);

				// 校验 @ZRequestMapping
				final ZRequestMapping requestMappingAnnotation = method.getAnnotation(ZRequestMapping.class);
				if (requestMappingAnnotation != null) {
					final String[] requestMappingArray = requestMappingAnnotation.mapping();

					ZControllerScanner.checkZRequestMapping(method, requestMappingAnnotation, requestMappingArray);

					final boolean[] isRegex = requestMappingAnnotation.isRegex();
					for (int i = 0; i < requestMappingArray.length; i++) {
						final String mapping = requestMappingArray[i];
						final MethodEnum methodEnum = requestMappingAnnotation.method();
						ZControllerMap.put(methodEnum, mapping, method, controllerObject, isRegex[i]);
					}
				}


				// FIXME 2023年8月30日 下午6:55:14 zhanghen: TODO 查找方法上的自定义A，看A是否被ZIAOP的子类拦截了，如果拦截了，则执行
				final Annotation[] as = method.getAnnotations();
				for (final Annotation annotation : as) {

					final ServerConfiguration serverConfigurationx = ZContext.getBean(ServerConfiguration.class);
					final Set<Class<?>> clsSet = ClassMap.scanPackage(serverConfigurationx.getScanPackage());

					final List<Class<? extends ZIAOP>> ziaopSubClassList = findZIAOPSubClass(clsSet);
					final List<Class<? extends ZIAOP>> collect = ziaopSubClassList.stream()
							.filter(c -> c.getAnnotation(ZAOP.class) != null)
							.filter(c -> c.getAnnotation(ZAOP.class).interceptType()
								.getCanonicalName().equals(annotation.annotationType().getCanonicalName())
							).collect(Collectors.toList());

					if (CollUtil.isNotEmpty(collect)) {
//						System.out.println("annotation = " + annotation);
//						System.out.println("clsSet = " + clsSet);
//						System.out.println("collect = " + collect);

						ZControllerMap.putMyAnnotation(method, null, collect);
					}
				}


			}

		}

		return zcSet;
	}

	private static List<Class<? extends ZIAOP>> findZIAOPSubClass(final Set<Class<?>> clsSet ) {
		final List<Class<? extends ZIAOP>> r = Lists.newArrayList();
		for (final Class<?> cls : clsSet) {
			final Class<?>[] is = cls.getInterfaces();
			if (ArrayUtil.isEmpty(is)) {
				continue;
			}

			final List<Class<?>> ziaopSubClassList = Lists.newArrayList(is).stream()
					.filter(i -> i.getCanonicalName().equals(ZIAOP.class.getCanonicalName()))
					.collect(Collectors.toList());
			if (CollUtil.isNotEmpty(ziaopSubClassList)) {
				r.add((Class<? extends ZIAOP>) cls);
			}
		}

		return r;
	}

	private static void checkNoVoidWithZResponse(final Method method) {
		if (!Task.VOID.equals(method.getReturnType().getCanonicalName())) {
			final Parameter[] ps = method.getParameters();
			final Optional<Parameter> ro = Lists.newArrayList(ps).stream()
					.filter(p -> p.getType().getCanonicalName().equals(ZResponse.class.getCanonicalName()))
					.findAny();
			if (ro.isPresent()) {
				throw new StartupException(
						"接口方法 " + method.getName() + " 带返回值不允许使用 " + ZResponse.class.getSimpleName() + " 参数，去掉 "
								+ ZResponse.class.getSimpleName() + " 参数，或者返回值改为 void");
			}
		}
	}

	private static void checkZRequestMapping(final Method method, final ZRequestMapping requestMappingAnnotation,
			final String[] requestMappingArray) {

		if (ArrayUtil.isEmpty(requestMappingArray)) {
			throw new StartupException("接口方法 " + method.getName() + " mapping值不能为空");
		}

		// requestMappingArray 如果有 @ZPathVariable，则长度只能为1
		for (final String mapping : requestMappingArray) {
			final String p1 = mapping.replaceAll("//+", "/");
			if (!mapping.equals(p1)) {
				throw new StartupException(
						"接口方法mapping 必须使用一个/分隔,接口方法=" + method.getName() + ",mapping=" + mapping);
			}

			final String[] sa = p1.split("/");
			final List<String> zpvNameList = Lists.newArrayList();
			for (final String s : sa) {
				if (s.length() <= 1) {
					continue;
				}
				if ((s.charAt(0) == '{' && s.charAt(s.length() - 1) == '}')) {
					zpvNameList.add(s.substring(1, s.length() - 1));
					if ((requestMappingArray.length > 1)) {
						throw new StartupException(
								"接口方法[" + method.getName() + "]参数使用@" + ZPathVariable.class.getSimpleName()
										+ ",则mapping只能声明一个,当前声明了" + requestMappingArray.length + "个"
										+ Arrays.toString(requestMappingArray) + ",请修改");
					}
				}
			}
			final Parameter[] ps = method.getParameters();

			final List<Parameter> zpvPList = Lists.newArrayList(ps).stream()
					.filter(p -> p.isAnnotationPresent(ZPathVariable.class)).collect(Collectors.toList());

			if (zpvPList.size() != zpvNameList.size()) {
				throw new StartupException("接口方法mapping 声明@" + ZPathVariable.class.getSimpleName() + "个数["
						+ zpvNameList.size() + "]与方法参数个数[" + zpvPList.size() + "]不一致,接口方法=" + method.getName()
						+ ",mapping=" + mapping);
			}

			if (!zpvPList.isEmpty()) {
				for (int i = 0; i < zpvPList.size(); i++) {
					// 可以按名称顺序判断，因为一个方法中参数名称不可以重复
					if (!zpvPList.get(i).getName().equals(zpvNameList.get(i))) {
						final List<String> pnl = zpvPList.stream().map(p -> p.getName())
								.collect(Collectors.toList());
						throw new StartupException("接口方法mapping 声明@" + ZPathVariable.class.getSimpleName()
								+ "参数顺序" + pnl + "与方法mapping" + zpvNameList + "顺序不一致,请修改参数顺序。接口方法=" + method.getName()
								+ ",mapping=" + mapping);
					}
				}
			}
		}

		final boolean[] isRegex = requestMappingAnnotation.isRegex();
		if (!ArrayUtil.isEmpty(isRegex) && isRegex.length != requestMappingArray.length) {
			throw new StartupException("接口方法 " + method.getName() + " isRegex个数必须与mapping值个数 相匹配, isRegex个数 = "
					+ isRegex.length + " mapping个数 = " + requestMappingArray.length);
		}

		final Set<String> temp = Sets.newHashSet();

		for (final String requestMapping : requestMappingArray) {

			if (StrUtil.isEmpty(requestMapping)) {
				throw new StartupException("接口方法 " + method.getName() + " mapping值不能为空");
			}

			ZControllerScanner.checkRequestMapping(method, requestMapping);

			final boolean add = temp.add(requestMapping + "@" + requestMappingAnnotation.method().getMethod());
			if (!add) {
				throw new StartupException(
						"接口方法 " + method.getName() + " mapping值不能重复,mapping = " + Arrays.toString(requestMappingArray));
			}
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
			throw new StartupException("接口方法 " + method.getName() + " mapping值必须以/开始,method = "
					+ method.getName() + " requestMapping = " + requestMapping);
		}

		if (requestMapping.length() <= 1) {
			return;
		}

		final char charAt = requestMapping.charAt(1);
		if (charAt == '/') {
			throw new StartupException("接口方法 " + method.getName() + " mapping值必须以/开始,method = "
					+ method.getName() + " requestMapping = " + requestMapping);
		}

	}

	private static void checkZHtml(final Method method) {
		if (ZControllerScanner.isHttpMethod(method) && method.isAnnotationPresent(ZHtml.class)) {
			final Class<?> rCls = method.getReturnType();
			final boolean isS = rCls.getCanonicalName().equals(String.class.getCanonicalName());
			if (!isS) {
				throw new StartupException(
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
