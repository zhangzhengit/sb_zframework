package com.vo;

import com.vo.core.ZSingleton;

/**
 * 创建一个Bean
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
public class ZBeanCreator {

	/**
	 * 根据Class对象创建一个单例Bean
	 *
	 * @param cls
	 * @return
	 *
	 */
	public static Object create(final Class cls) {
		final Object bean = ZSingleton.getSingletonByClass(cls);
		return bean;
	}

}
