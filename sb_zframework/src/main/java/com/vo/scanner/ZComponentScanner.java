package com.vo.scanner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZComponent;
import com.vo.aop.ZAOPProxyClass;
import com.vo.aop.ZAOPScaner;
import com.vo.conf.ServerConfiguration;
import com.vo.core.Task;
import com.vo.core.ZClass;
import com.vo.core.ZContext;
import com.vo.core.ZLog2;
import com.vo.core.ZMethod;
import com.vo.core.ZMethodArg;
import com.vo.core.ZObjectGeneratorStarter;
import com.vo.core.ZPackage;
import com.vo.core.ZSingleton;
import com.vo.validator.ZValidated;
import com.vo.validator.ZValidator;

/**
 * 扫描 @ZComponent 的类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
public class ZComponentScanner {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static void scanAndCreate() {

		final Map<String, ZClass> map = ZAOPScaner.scanAndGenerateProxyClass1();

		final ServerConfiguration serverConfiguration = ZContext.getBean(ServerConfiguration.class);

		LOG.info("开始扫描带有[{}]注解的类", ZComponent.class.getCanonicalName());
		final Set<Class<?>> zcSet = ClassMap.scanPackageByAnnotation(serverConfiguration.getScanPackage(), ZComponent.class);
		LOG.info("扫描到带有[{}]注解的类个数={}", ZComponent.class.getCanonicalName(),zcSet.size());
		LOG.info("开始给带有[{}]注解的类创建对象",ZComponent.class.getCanonicalName());
		for (final Class<?> cls : zcSet) {
			LOG.info("开始给待有[{}]注解的类[{}]创建对象",ZComponent.class.getCanonicalName(),cls.getCanonicalName());
			final Object object = ZSingleton.getSingletonByClass(cls);
			LOG.info("给带有[{}]注解的类[{}]创建对象[{}]完成", ZComponent.class.getCanonicalName(),
					cls.getCanonicalName(), object);

			final Object newComponent = ZObjectGeneratorStarter.generate(cls);
			final ZClass proxyClass = map.get(newComponent.getClass().getSimpleName());
			if (proxyClass != null) {
				final Object newInstance = proxyClass.newInstance();
				// 放代理类
				ZContext.addBean(newComponent.getClass().getCanonicalName(), newInstance);
				ZContext.addZClassBean(newComponent.getClass().getCanonicalName(), proxyClass, newInstance);
			} else {

				// 放原类情况(也是放生成的代理类)：
				// 1、@ZComponent 类中方法的参数是否带有 @ZValidated 注解，有则插入校验代码，无则super.xx(xx);

				final ZClass proxyZClass = new ZClass();
				proxyZClass.setPackage1(new ZPackage(cls.getPackage().getName()));
				proxyZClass.setName(cls.getSimpleName() + ZAOPScaner.PROXY_ZCLASS_NAME_SUFFIX);
				proxyZClass.setSuperClass(cls.getCanonicalName());
				proxyZClass.setAnnotationSet(Sets.newHashSet(ZAOPProxyClass.class.getCanonicalName()));

				final Method[] mss = cls.getDeclaredMethods();

				final HashSet<ZMethod> zms = Sets.newHashSet();
				for (final Method m : mss) {
					final ArrayList<ZMethodArg> argList = ZMethod.getArgListFromMethod(m);
					final String a = argList.stream().map(ma ->  ma.getName()).collect(Collectors.joining(","));
					final Class<?> returnType = m.getReturnType();
					if (Lists.newArrayList(m.getParameterTypes()).stream().filter(pa -> pa.isAnnotationPresent(ZValidated.class)).findAny().isPresent()) {
						final String insertBody =
								"if (zvdto.getClass().isAnnotationPresent(" + ZValidated.class.getCanonicalName() + ".class)) {"  + Task.NEW_LINE
								+  "for (final " + Field.class.getCanonicalName() + " field : zvdto.getClass().getDeclaredFields()) {"  + Task.NEW_LINE
								+  		 ZValidator.class.getCanonicalName() + ".validatedAll(zvdto, field);"  + Task.NEW_LINE
								+   "}" + Task.NEW_LINE
								+ "}";

						final String body =
								ZAOPScaner.VOID.equals(returnType.getName())
								? "super." + m.getName() + "(" + a + ");"
								: "return super." + m.getName() + "(" + a + ");";

						final ZMethod zm = ZMethod.copyFromMethod(m);
						zm.setgReturn(false);
						zm.setBody(insertBody  + Task.NEW_LINE + body);

						zms.add(zm);

					} else {

						final String body =
								ZAOPScaner.VOID.equals(returnType.getName())
								? "super." + m.getName() + "(" + a + ");"
								: "return super." + m.getName() + "(" + a + ");";

						final ZMethod zm = ZMethod.copyFromMethod(m);
						zm.setgReturn(false);
						zm.setBody(body);

						zms.add(zm);
					}
				}
				proxyZClass.setMethodSet(zms);

				ZContext.addBean(newComponent.getClass().getCanonicalName(), proxyZClass.newInstance());
			}
		}

		LOG.info("给带有[{}]注解的类创建对象完成,个数={}", ZComponent.class.getCanonicalName(), zcSet.size());
	}

}
