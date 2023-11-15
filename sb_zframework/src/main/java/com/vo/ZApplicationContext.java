package com.vo;

import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vo.core.ZContext;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 程序启动的信息
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public final class ZApplicationContext {

	/**
	 * 程序启动扫描的包铭
	 */
	private List<String> scanPackageNameList;

	/**
	 * 是否启用http服务器
	 */
	private boolean httpEnable;

	/**
	 * java命令行中传来的参数
	 */
	private String[] args;

	/**
	 * 配置文件信息
	 */
	private PropertiesConfiguration properties;

	/**
	 * 获取容器中所有的bean
	 *
	 * @return
	 *
	 */
	public ImmutableCollection<Object> getBeans() {
		final ImmutableMap<String, Object> map = ZContext.all();
		final ImmutableCollection<Object> values = map.values();
		return values;
	}

	/**
	 * 获取容器中所有的bean的名称
	 *
	 * @return
	 *
	 */
	public ImmutableSet<String> getBeanNames() {
		final ImmutableMap<String, Object> map = ZContext.all();
		final ImmutableSet<String> ks = map.keySet();
		return ks;
	}

	/**
	 * 获取容器中的beanMap
	 *
	 * @return
	 *
	 */
	public ImmutableMap<String, Object> getBeanMap() {
		return ZContext.all();
	}


	/**
	 * 手动注册一个bean
	 *
	 * @param beanClass
	 * @param bean
	 *
	 */
	public void registerBeanDefinition(final Class<?> beanClass, final Object bean) {
		ZContext.addBean(beanClass, bean);
	}

	/**
	 * 手动注册一个bean
	 *
	 * @param beanName
	 * @param bean
	 *
	 */
	public void registerBeanDefinition(final String beanName, final Object bean) {
		ZContext.addBean(beanName, bean);
	}

}
