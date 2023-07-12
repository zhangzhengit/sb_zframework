package com.vo.http;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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

		checkAPI(methodEnum, mapping, method, object);

		methodPathTable.put(methodEnum, mapping, method);

		methodIsregexTable.put(method, mapping, isRegex);

		objectMap.put(method, object);

		final ZRequestMapping zrp = method.getAnnotation(ZRequestMapping.class);
		if (zrp != null) {
			final int qps = zrp.qps();
			if (qps <= 0) {
				throw new IllegalArgumentException(
						"接口qps不能设为小于0,method = " + method.getName() + "\t" + "qps = " + qps);
			}
			methodQPSTable.put(object.getClass().getCanonicalName(), method.getName(), qps);
		}
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
			final Object object) {
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

		final boolean add = mappingSet.add(mapping);
		if (!add) {
			throw new IllegalArgumentException(
					"接口方法的 mapping值重复, mapping = " + mapping + "\t" + " method = " + method.getName());
		}
	}

}
