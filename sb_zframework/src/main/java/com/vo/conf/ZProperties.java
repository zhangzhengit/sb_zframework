package com.vo.conf;

import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * 加载全部的配置文件的值
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
// FIXME 2023年6月29日 下午11:26:04 zhanghen: 是否支持配置项热更新？改为实时读取配置项？
public class ZProperties {

	public static final ThreadLocal<String> PROPERTIESCONFIGURATION_ENCODING = new ThreadLocal<>();
	public static final PropertiesConfiguration P;

	public static PropertiesConfiguration getInstance() {
		return P;
	}

	private ZProperties() {
	}

	static {
		final List<String> plist = ZFrameworkDatasourcePropertiesLoader.PROPERTIES_LIST;


		PropertiesConfiguration propertiesConfiguration = null;
		for (final String pv : plist) {
			try {
				propertiesConfiguration = new PropertiesConfiguration(pv);
			} catch (final ConfigurationException e) {
				continue;
			}
		}

		if (propertiesConfiguration == null) {
			throw new IllegalArgumentException("配置文件不存在");
		}

		PROPERTIESCONFIGURATION_ENCODING.set(propertiesConfiguration.getEncoding());
		System.out.println("propertiesConfiguration-encoding = " + propertiesConfiguration.getEncoding());
		P = propertiesConfiguration;
	}

}
