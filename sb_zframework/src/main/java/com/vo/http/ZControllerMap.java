package com.vo.http;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vo.aop.ZIAOP;
import com.vo.enums.MethodEnum;

import cn.hutool.core.util.StrUtil;

/**
 * 存取接口方法
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
public class ZControllerMap {
	static final HashBasedTable<MethodEnum, String, Method> methodPathTable = HashBasedTable.create();
	static final HashBasedTable<String, String, Integer> methodQPSTable = HashBasedTable.create();
	static final HashBasedTable<String, String, ZQPSLimitation> methodZQPSLimitationTable = HashBasedTable.create();
	static final HashBasedTable<Method, String, Boolean> methodIsregexTable = HashBasedTable.create();
	static final HashMap<Method, Object> objectMap = Maps.newHashMap();
	static final HashSet<String> mappingSet = Sets.newHashSet();

	/**
	 * 注册一个接口
	 *
	 * @param methodEnum 接口请求方法，如： MethodEnum.POST
	 * @param mapping    匹配路径，如：/index
	 * @param method     具体的接口方法
	 * @param object     接口方法所在的对象
	 * @param isRegex    mapping 是否正则表达式
	 */
	public synchronized static void put(final MethodEnum methodEnum, final String mapping, final Method method,
			final Object object, final boolean isRegex) {

		final ZRequestMapping requestMapping = method.getAnnotation(ZRequestMapping.class);

		checkAPI(methodEnum, mapping, method, object, requestMapping);

		methodPathTable.put(methodEnum, mapping, method);

		methodIsregexTable.put(method, mapping, isRegex);

		objectMap.put(method, object);

		if (requestMapping != null) {
			final int qps = requestMapping.qps();
			if (qps <= 0) {
				throw new IllegalArgumentException(
						"接口qps不能设为小于0,method = " + method.getName() + "\t" + "qps = " + qps);
			}
			methodQPSTable.put(object.getClass().getCanonicalName(), method.getName(), qps);
		}

		final ZQPSLimitation zqpsl = method.getAnnotation(ZQPSLimitation.class);
		if (zqpsl != null) {
			final ZQPSLimitationEnum type = zqpsl.type();
			if (type == null) {
				throw new IllegalArgumentException(
						"@" + ZQPSLimitation.class.getSimpleName() + ".type 不能为空,method = " + method.getName());
			}
			final int qps = zqpsl.qps();
			if (qps <= 0) {
				throw new IllegalArgumentException(
						"@" + ZQPSLimitation.class.getSimpleName() + ".qps 必须大于0,method = " + method.getName());
			}
			if (qps > requestMapping.qps()) {
				throw new IllegalArgumentException("@" + ZQPSLimitation.class.getSimpleName() + ".qps 不能大于 @"
						+ ZRequestMapping.class.getSimpleName() + ".qps" + ",method = " + method.getName());
			}

			methodZQPSLimitationTable.put(object.getClass().getCanonicalName(), method.getName(), zqpsl);
		}
	}

	public static ZQPSLimitation getZQPSLimitationByControllerNameAndMethodName(final String controllerName,final String methodName) {
		final ZQPSLimitation zqpsLimitation = methodZQPSLimitationTable.get(controllerName, methodName);
		return zqpsLimitation;
	}

	public static Integer getQPSByControllerNameAndMethodName(final String controllerName,final String methodName) {
		final Integer qps = methodQPSTable.get(controllerName, methodName);
		return qps;
	}

	public static Object getObjectByMethod(final Method method) {
		final Object object = objectMap.get(method);
		return object;
	}


	public static Method getMethodByMethodEnumAndPath(final MethodEnum methodEnum, final String path) {
		final Method method = methodPathTable.get(methodEnum, path);
		return method;
	}

	public static Map<MethodEnum, Method> getByPath(final String path) {
		final Map<MethodEnum, Method> column = methodPathTable.column(path);
		return column;
	}
	public static Map<String, Method> getByMethodEnum(final MethodEnum methodEnum) {

		final Map<String, Method> row = methodPathTable.row(methodEnum);
		return row;
	}

	public static Boolean getIsregexByMethodEnumAndPath(final Method method, final String path) {
		return methodIsregexTable.get(method, path);
	}

	private static void checkAPI(final MethodEnum methodEnum, final String mapping, final Method method,
			final Object object, final ZRequestMapping requestMapping) {
		if (methodEnum == null) {
			throw new IllegalArgumentException(MethodEnum.class.getSimpleName() + " 不能为空");
		}

		if (StrUtil.isEmpty(mapping)) {
			throw new IllegalArgumentException("mapping 不能为空");
		}
		if (!mapping.startsWith("/")) {
			throw new IllegalArgumentException("mapping 必须以/开始");
		}
		if (method == null) {
			throw new IllegalArgumentException("method 不能为空");
		}
		if (object == null) {
			throw new IllegalArgumentException("object 不能为空");
		}

		final String mappingMethod = mapping + "@" + requestMapping.method().getMethod();
		final boolean add = mappingSet.add(mappingMethod);
		if (!add) {
			throw new IllegalArgumentException(
					"接口方法的 mapping和Method重复, mapping = " + mapping + "\t" + " method = " + method.getName());
		}
	}

	private static final ConcurrentMap<Method, List<Class<? extends ZIAOP>>> M_A_MAP = Maps.newConcurrentMap();

	public static void putMyAnnotation(final Method method, final Annotation annotation,
			final List<Class<? extends ZIAOP>> ziaopSubClassList) {
		M_A_MAP.put(method, ziaopSubClassList);
	}

	public static List<Class<? extends ZIAOP>> getZIAOPSubClassList(final Method method) {
		return M_A_MAP.get(method);
	}
}
