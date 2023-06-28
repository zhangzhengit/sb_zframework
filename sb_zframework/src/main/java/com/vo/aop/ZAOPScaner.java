package com.vo.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vo.core.ZClass;
import com.vo.core.ZField;
import com.vo.core.ZMethod;
import com.vo.core.ZMethodArg;
import com.vo.core.ZPackage;
import com.vo.core.ZSingleton;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ClassUtil;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月18日
 *
 */
public class ZAOPScaner {

	public static final String PROXY_ZCLASS_NAME_SUFFIX = "_ProxyZclass";
	public static final ConcurrentMap<String, Map<String, ZClass>> zcMap = Maps.newConcurrentMap();

	public static ConcurrentMap<String, Method> cmap = Maps.newConcurrentMap();

	public static Map<String, ZClass> getZCMap() {
		final Map<String, ZClass> m = zcMap.get(KEY);
		return m;
	}

	public static final String KEY = "scan";

	public static Map<String, ZClass> scanAndGenerateProxyClass() {
		synchronized (KEY) {
			final Map<String, ZClass> vv = zcMap.get(KEY);
			if (vv != null) {
				return vv;
			}

			final Map<String, ZClass> map = ZAOPScaner.scanAndGenerateProxyClass1();
			zcMap.put(KEY, map);
			return map;
		}
	}

	static Map<String, ZClass> scanAndGenerateProxyClass1() {

		final Map<String, ZClass> map = Maps.newHashMap();
		final Set<Class<?>> cs = scanPackage_COM();

		final HashBasedTable<Class, Method, Class<?>> table = extractedC(cs);

		final Set<Class> rowKeySet = table.rowKeySet();
		for (final Class cls : rowKeySet) {
			final ZClass proxyZClass = new ZClass();
			proxyZClass.setPackage1(new ZPackage(cls.getPackage().getName()));
			proxyZClass.setName(cls.getSimpleName() + PROXY_ZCLASS_NAME_SUFFIX);
			proxyZClass.setSuperClass(cls.getCanonicalName());
			proxyZClass.setAnnotationSet(Sets.newHashSet(ZAOPProxyClass.class.getCanonicalName()));

			final Method[] mss = cls.getDeclaredMethods();
			final HashSet<ZMethod> zms = Sets.newHashSet();
			for (final Method m : mss) {

				gZMethod(table, cls, proxyZClass, zms, m);

			}
			proxyZClass.setMethodSet(zms);

			final String chiS = proxyZClass.toString();
//			System.out.println("chiS = \n" + chiS);

			map.put(cls.getSimpleName(), proxyZClass);
		}

		return map;
	}

	private static void gZMethod(final HashBasedTable<Class, Method, Class<?>> table, final Class cls,
			final ZClass proxyZClass, final HashSet<ZMethod> zms, final Method m) {
		final ZMethod zm = ZMethod.copyFromMethod(m);
//		System.out.println("zm = " + zm);
		zm.setgReturn(false);
		final ArrayList<ZMethodArg> argList = ZMethod.getArgListFromMethod(m);
		final String a = argList.stream().map(ma ->  ma.getName()).collect(Collectors.joining(","));
		final Class<?> returnType = m.getReturnType();

		final Map<Method, Class<?>> row = table.row(cls);
		if(row.containsKey(m)) {

			final ZMethod copyZAOPMethod = ZMethod.copyFromMethod(m);

			final Class<?> aopClass = table.get(cls, m);

			final ZField zField = new ZField(ZIAOP.class.getCanonicalName(), "ziaop_" + m.getName(),
					"(" + ZIAOP.class.getCanonicalName() + ")" + ZSingleton.class.getCanonicalName()
							+ ".getSingletonByClassName(\"" + aopClass.getCanonicalName() + "\")");
			proxyZClass.addField(zField);

			copyZAOPMethod.setgReturn(false);

			final String nnn = cls.getCanonicalName() + "@" + m.getName();
			cmap.put(nnn, m);

			final String returnTypeT = getReturnTypeT(m);
			final String body = gZMethodBody(m, a, nnn, returnTypeT);

			copyZAOPMethod.setBody(body);
			zms.add(copyZAOPMethod);

		} else {

			final String body =
					"void".equals(returnType.getName())
					?
					"super." + m.getName() + "(" + a + ");"
					:
					"return super." + m.getName() + "(" + a + ");";
			zm.setBody(body);
			zms.add(zm);
		}
	}

	private static String gZMethodBody(final Method m, final String a, final String nnn, final String returnTypeT) {
		final String body =
				"void".equals(returnTypeT)
				?
				"final "+AOPParameter.class.getCanonicalName()+" parameter = new "+AOPParameter.class.getCanonicalName()+"();" + "\n\t"
			  + "parameter.setIsVOID(true);" + "\n\t"
			  + "parameter.setTarget("+ZSingleton.class.getCanonicalName()+".getSingletonByClass(this.getClass().getSuperclass()));" + "\n\t"
			  + "parameter.setMethodName(\"" + m.getName() + "\");" + "\n\t"
			  +  Method.class.getCanonicalName() + " m = "+ZAOPScaner.class.getCanonicalName()+".cmap.get(\""+nnn+"\");" + "\n\t"
			  + "parameter.setMethod(m);" + "\n\t"
			  + "parameter.setParameterList("+Lists.class.getCanonicalName()+".newArrayList("+a+"));" + "\n\t"
			  + "ziaop_"+m.getName()+".before(parameter);" + "\n\t"
		      + "final Object v = this.ziaop_"+m.getName()+".around(parameter);" + "\n\t"
		      + "ziaop_"+m.getName()+".after(parameter);" + "\n\t"
		      :
		    	 "final "+AOPParameter.class.getCanonicalName()+" parameter = new "+AOPParameter.class.getCanonicalName()+"();" + "\n\t"
		      + "parameter.setIsVOID(false);" + "\n\t"
		      + "parameter.setTarget("+ZSingleton.class.getCanonicalName()+".getSingletonByClass(this.getClass().getSuperclass()));" + "\n\t"
		      + "parameter.setMethodName(\"" + m.getName() + "\");" + "\n\t"
		      +  Method.class.getCanonicalName() + " m = "+ZAOPScaner.class.getCanonicalName()+".cmap.get(\""+nnn+"\");" + "\n\t"
		      + "parameter.setMethod(m);" + "\n\t"
		      + "parameter.setParameterList("+Lists.class.getCanonicalName()+".newArrayList("+a+"));" + "\n\t"
		      + "ziaop_"+m.getName()+".before(parameter);" + "\n\t"
			  + "final Object v = this.ziaop_"+m.getName()+".around(parameter);" + "\n\t"
		      + "ziaop_"+m.getName()+".after(parameter);" + "\n\t"
		      + "return (" + returnTypeT + ")v;" + "\n\t";
		return body;
	}


	public static String getReturnTypeT(final Method method) {
		final Type genericReturnType = method.getGenericReturnType();
		final String string = genericReturnType.toString();
		final int i = string.indexOf("class");
		if(i > -1) {

			return string.substring("class".length() + i);
		}
		return string;
	}

	public static Set<Class<?>> scanPackage_COM() {
		// FIXME 2023年6月17日 下午6:46:40 zhanghen: com 改为属性配置
		// FIXME 2023年6月20日 上午12:22:27 zhanghen: 改为com.vo
//		final Set<Class<?>> clsSet = ClassUtil.scanPackage("com.vo.test");
		final Set<Class<?>> clsSet = ClassUtil.scanPackage("com.vo");
		return clsSet;
	}

	/**
	 * @param cs
	 * @return <类,方法，此类此方法的AOP类>
	 */
	private static HashBasedTable<Class, Method, Class<?>> extractedC(final Set<Class<?>> cs) {
		final HashBasedTable<Class, Method, Class<?>> table = HashBasedTable.create();
		for (final Class<?> c : cs) {
			final Method[] ms = c.getDeclaredMethods();
			for (final Method m : ms) {
				final Annotation[] mas = m.getAnnotations();
				for (final Annotation  a : mas) {

					final List<Class<?>> aL = cs.stream()
							.filter(c2 -> c2.isAnnotationPresent(ZAOP.class))
							.filter(c2 -> c2.getAnnotation(ZAOP.class).interceptType().getCanonicalName()
									.equals(a.annotationType().getCanonicalName()))
							.collect(Collectors.toList());

					if (aL.size() > 1) {
						throw new IllegalArgumentException("注解 @" + a.annotationType().getCanonicalName()
								+ " 只能只允许有一个AOP类!现在有 " + aL.size() + " 个 = " + aL);
					}

					if (CollectionUtil.isNotEmpty(aL)) {
						table.put(c, m, aL.get(0));
					}
				}

			}

		}

		return table;
	}


}
