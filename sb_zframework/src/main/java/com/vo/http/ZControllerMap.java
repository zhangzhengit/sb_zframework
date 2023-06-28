package com.vo.http;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.vo.anno.ZRequestBody;
import com.vo.anno.ZRequestHeader;
import com.vo.core.HRequest;
import com.vo.core.HResponse;
import com.vo.template.ZModel;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;


/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZControllerMap {

	static ConcurrentMap<String, Method> nameMap = Maps.newConcurrentMap();
	/**
	 * <path,Method>
	 */
	static ConcurrentMap<String, Method> zcMap = Maps.newConcurrentMap();
	static HashBasedTable<String, Method, Object> table = HashBasedTable.create();

	public synchronized static void putBean(final String path, final Method method, final Object zControllerObject) {
		table.put(path, method, zControllerObject);
		zcMap.put(path, method);

		String fullName = path;
		final Parameter[] ps = method.getParameters();
		if(ArrayUtil.isNotEmpty(ps)) {
			for (final Parameter parameter : ps) {
				final String name = parameter.getName();
				final Class<?> type = parameter.getType();
				final boolean isZHeader = parameter.isAnnotationPresent(ZRequestHeader.class);
				final boolean isHRequest = type.getCanonicalName().equals(HRequest.class.getCanonicalName());
				final boolean isHResponse = type.getCanonicalName().equals(HResponse.class.getCanonicalName());
				final boolean isZModel = type.getCanonicalName().equals(ZModel.class.getCanonicalName());
				final boolean isZRequestBodyAnnotation = parameter.isAnnotationPresent(ZRequestBody.class);

				final boolean annotationPresent = type.isAnnotationPresent(ZRequestBody.class);

				if (!isZHeader && !isHRequest && !isHResponse && !isZModel && !isZRequestBodyAnnotation) {
					fullName = fullName + "@" + name;
				}
//				System.out.println("p-name = " + name);
			}
		}
//		System.out.println("put-fullName = " + fullName);
		nameMap.put(fullName, method);
	}

	public static Method getMethodByFullName(final String fullName) {
		final Method m = nameMap.get(fullName);
		return m;
	}

	public static Method getMethodByPath(final String path) {
		final Method v = zcMap.get(path);
		return v;
	}

	public static Object getZControllerByPath(final String path) {
		final Method m = getMethodByPath(path);
		final Object zc = table.get(path, m);
		return zc;
	}

}
