package com.vo.scanner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vo.anno.ZControllerInterceptor;
import com.vo.anno.ZOrder;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZContext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 扫描 ZControllerInterceptor 的实现类
 *
 * @author zhangzhen
 * @date 2023年7月11日
 *
 */
public class ZControllerInterceptorScanner {

	private static final Set<ZControllerInterceptor> ZCISET = Sets.newLinkedHashSet();

	public static Set<ZControllerInterceptor> get() {
		return ZCISET;
	}

	static Set<Integer> orderValue = Sets.newHashSet();

	public static void scan() {
		final ServerConfiguration serverConfiguration = ZContext.getBean(ServerConfiguration.class);
		final Set<Class<?>> clsSet = ClassMap.scanPackage(serverConfiguration.getScanPackage());

		final ArrayList<ZCIP> zcipList = Lists.newArrayList();

		for (final Class<?> class1 : clsSet) {
			final long zciCount = Lists.newArrayList(class1.getInterfaces()).stream()
					.filter(i -> i.getCanonicalName().equals(ZControllerInterceptor.class.getCanonicalName()))
					.count();
			if (zciCount >= 1) {
				// 可以有多个，但是多个子类要做好逻辑，不要重复调用目标方法

				final ZOrder order = class1.getAnnotation(ZOrder.class);
				if (order != null) {
					final int value = order.value();
					final boolean add = orderValue.add(value);
					if(!add) {
						throw new IllegalArgumentException(
								ZControllerInterceptor.class.getSimpleName()
								+ " 子类 "
								+ class1.getSimpleName()
								+ "@" + ZOrder.class.getSimpleName()
								+ " value 重复,value = " + value
								);
					}

					zcipList.add(new ZCIP(value, class1));
				}
			}

		}

		zcipList.sort(Comparator.comparing(ZCIP::getOrderValue));
		for (final ZCIP zcip : zcipList) {
			Object newInstance = null;
			try {
				newInstance = zcip.getCls().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
			ZContext.addBean(zcip.getCls().getCanonicalName(), newInstance);
			ZCISET.add((ZControllerInterceptor) newInstance);
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class ZCIP {
		private int orderValue;
		private Class cls;
	}
}
