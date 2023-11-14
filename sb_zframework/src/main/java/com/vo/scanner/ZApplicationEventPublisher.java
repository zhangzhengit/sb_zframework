package com.vo.scanner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.vo.anno.ZComponent;
import com.vo.core.ZContext;
import com.vo.validator.StartupException;
import com.votool.ze.ZE;
import com.votool.ze.ZES;

import cn.hutool.core.util.ArrayUtil;

/**
 * 事件发布者
 *
 * @author zhangzhen
 * @date 2023年11月14日
 *
 */
@ZComponent
public final class ZApplicationEventPublisher {

	private final Object lock = new Object();
	private Map<Method, Object> beanMap;

	public synchronized void publishEvent(final ZApplicationEvent event) {

		synchronized (this.lock) {
			if (this.beanMap == null) {
				this.beanMap = init();
			}
		}

		final Set<Method> ks = this.beanMap.keySet();

		for (final Method method : ks) {
			final ZEventListener el = method.getAnnotation(ZEventListener.class);
			final Class<? extends ZApplicationEvent> e = el.value();
			if (e.getCanonicalName().equals(event.getClass().getCanonicalName())) {
				final Object object = this.beanMap.get(method);
				this.invoke(method, object, event);
			}
		}
	}

	private static ZE newZE = ZES.newZE(Runtime.getRuntime().availableProcessors(),
			"applicationEventPublisher-Thread-");

	private static void invoke(final Method method, final Object object, final  ZApplicationEvent event) {

		newZE.executeInQueue(() -> {
			try {
				method.invoke(object, event);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		});
	}

	private static Map<Method, Object> init() {
		final ImmutableMap<String, Object> m = ZContext.all();
		final ImmutableCollection<Object> vs = m.values();

		final Map<Method, Object> beanMap = Maps.newHashMap();

		for (final Object bean : vs) {
			final Method[] ms = bean.getClass().getDeclaredMethods();
			for (final Method method : ms) {
				if (method.isAnnotationPresent(ZEventListener.class)) {
					beanMap.put(method, bean);
				}
			}
		}

		return beanMap;
	}

	public void publishEvent(final ZApplicationEvent... events) {
		for (final ZApplicationEvent e : events) {
			this.publishEvent(e);
		}
	}

	public static void start(final String... packageName) {
		final Set<Class<?>> csSet = ClassMap.scanPackage(packageName);
		for (final Class<?> cls : csSet) {

			final Method[] ms = cls.getDeclaredMethods();
			for (final Method m : ms) {
				final ZEventListener eventListener = m.getAnnotation(ZEventListener.class);
				if (eventListener == null) {
					continue;
				}

				final Parameter[] ps = m.getParameters();
				if (ArrayUtil.isEmpty(ps) || ps.length != 1 || !ps[0].getType().equals(eventListener.value())) {
					throw new StartupException(
							"@" + ZEventListener.class.getSimpleName() + " 标记的方法[" + m.getName() + "]必须带带有参数["
							+ eventListener.value().getSimpleName() + "]"
							);
				}
			}
		}

	}
}
