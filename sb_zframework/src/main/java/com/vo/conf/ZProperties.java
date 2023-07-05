package com.vo.conf;

import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.Lists;
import com.vo.core.ZLog2;

/**
 * zframework.properties 配置文件里全部的k=v的值
 *
 * 加载全部的配置文件的值
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
// FIXME 2023年6月29日 下午11:26:04 zhanghen: 是否支持配置项热更新？改为实时读取配置项？
public class ZProperties {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final ThreadLocal<String> PROPERTIESCONFIGURATION_ENCODING = new ThreadLocal<>();


	public static final String PROPERTIES_NAME = "zframework.properties";

	public static final String PROPERTIES = "zframework.properties";

	public static final String PROPERTIES_1 = "config/zframework.properties";
	public static final String PROPERTIES_2 = "zframework.properties";
	public static final String PROPERTIES_3 = "src/main/resources/zframework.properties";
	public static final String PROPERTIES_4 = "src/main/resources/config/zframework.properties";

	public static final List<String> PROPERTIES_LIST = Lists.newArrayList(PROPERTIES, PROPERTIES_1, PROPERTIES_2,
			PROPERTIES_3, PROPERTIES_4);

	public static final PropertiesConfiguration P;

	public static PropertiesConfiguration getInstance() {
		return P;
	}

	private ZProperties() {
	}

	static {
		PropertiesConfiguration propertiesConfiguration = null;
		for (final String pv : PROPERTIES_LIST) {
			try {
				propertiesConfiguration = new PropertiesConfiguration(pv);
			} catch (final ConfigurationException e) {
				continue;
			}
		}

		if (propertiesConfiguration == null) {
			LOG.error("找不到配置文件 [{}", ZProperties.PROPERTIES_NAME);
			throw new IllegalArgumentException("找不到配置文件 " + ZProperties.PROPERTIES_NAME);
		}

		PROPERTIESCONFIGURATION_ENCODING.set(propertiesConfiguration.getEncoding());
		System.out.println("propertiesConfiguration-encoding = " + propertiesConfiguration.getEncoding());
		P = propertiesConfiguration;
	}

}
