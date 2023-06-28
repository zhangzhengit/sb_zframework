package com.vo.http;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vo.enums.MethodEnum;

/**
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
public class ZControllerMap {
	static final HashBasedTable<MethodEnum, String, Method> methodTable = HashBasedTable.create();
	static final HashBasedTable<Method, String, Boolean> methodIsregexTable = HashBasedTable.create();
	static final HashMap<Method, Object> objectMap = Maps.newHashMap();
	static final HashSet<String> mappingSet = Sets.newHashSet();

	public synchronized static void put(final MethodEnum methodEnum, final String mapping, final Method method,
			final Object object, final boolean isRegex) {
		final boolean add = mappingSet.add(mapping);
		if (!add) {
			throw new IllegalArgumentException(
					"接口方法的 mapping值重复, mapping = " + mapping + "\t" + " method = " + method.getName());
		}

		methodTable.put(methodEnum, mapping, method);
		methodIsregexTable.put(method, mapping, isRegex);
		objectMap.put(method, object);
	}

	public static Object getObjectByMethod(final Method method) {
		final Object object = objectMap.get(method);
		return object;
	}

	public static Method getMethodByMethodEnumAndPath(final MethodEnum methodEnum, final String path) {
		final Method method = methodTable.get(methodEnum, path);
		return method;
	}

	public static Map<String, Method> getByMethodEnum(final MethodEnum methodEnum) {

		final Map<String, Method> row = methodTable.row(methodEnum);
		return row;
	}

	public static Boolean getIsregexByMethodEnumAndPath(final Method method, final String path) {
		return methodIsregexTable.get(method, path);
	}
}
