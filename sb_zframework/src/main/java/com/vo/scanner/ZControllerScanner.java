package com.vo.scanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.checkerframework.checker.units.qual.m;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.data.redis.connection.ReactiveZSetCommands.ZScoreCommand;

import com.google.common.collect.Maps;
import com.vo.anno.ZController;
import com.vo.core.ZLog2;
import com.vo.core.ZObjectGenerator;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZSingleton;
import com.vo.enums.BeanModeEnum;
import com.vo.enums.MethodEnum;
import com.vo.http.ZConMap;
import com.vo.http.ZControllerMap;
import com.vo.http.ZDelete;
import com.vo.http.ZGet;
import com.vo.http.ZHtml;
import com.vo.http.ZPost;
import com.vo.http.ZPut;
import com.votool.common.ZPU;

import cn.hutool.core.util.ClassUtil;

/**
 * 扫描 @ZController 的类，注册为一个控制类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZControllerScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static Set<Class<?>> scanAndCreateObject(final String packageName) {
		LOG.info("开始扫描带有[{}]的类", ZController.class.getCanonicalName());
		final Set<Class<?>> zcSet = ClassUtil.scanPackageByAnnotation(packageName, ZController.class);
		LOG.info("带有[{}]的类个数={}", ZController.class.getCanonicalName(), zcSet.size());

		for (final Class<?> cls : zcSet) {

//			final Object newZController1 = ZControllerScanner.getSingleton(cls);

			final Object newZController1 = ZObjectGeneratorStarter.generate(cls);

			LOG.info("带有[{}]的类[{}]创建对象[{}]完成", ZController.class.getCanonicalName(), cls.getCanonicalName(),
					newZController1);
			ZConMap.putBean(cls.getCanonicalName(), newZController1);

			final Method[] ms = cls.getDeclaredMethods();
			for (final Method method : ms) {

				checkHttpMethod(method);

				final ZGet zget = method.getAnnotation(ZGet.class);
				if (zget != null) {
					final Object newZController = getSingleton(cls);
//					final Object newZController  = ZSingleton.getSingletonByClass(cls);
					ZControllerMap.putBean(MethodEnum.GET.getMethod() + "@" + zget.path(), method, newZController);
				}

				final ZPost zpost = method.getAnnotation(ZPost.class);
				if (zpost != null) {
					final Object newZController = getSingleton(cls);
//					final Object newZController  = ZSingleton.getSingletonByClass(cls);
					ZControllerMap.putBean(MethodEnum.POST.getMethod() + "@" + zpost.path(), method, newZController);
				}

			}

		}

		return zcSet;
	}

	private static void checkHttpMethod(final Method method) {
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
		return  method.isAnnotationPresent(ZGet.class)
			 || method.isAnnotationPresent(ZPost.class)
			 || method.isAnnotationPresent(ZPut.class)
			 || method.isAnnotationPresent(ZDelete.class)
				;
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
