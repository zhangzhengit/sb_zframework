package com.vo.conf;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月17日
 *
 */
public class ZFrameworkDatasourcePropertiesLoader {


	public static final String DATASOURCE_PROPERTIES = "zframework.properties";

	public static final String DATASOURCE_PROPERTIES_1 = "config/zframework.properties";
	public static final String DATASOURCE_PROPERTIES_2 = "zframework.properties";
	public static final String DATASOURCE_PROPERTIES_3 = "src/main/resources/zframework.properties";
	public static final String DATASOURCE_PROPERTIES_4 = "src/main/resources/config/zframework.properties";

	private static ZFrameworkProperties INSTANCE;

	public static ZFrameworkProperties getFrameworkPropertiesInstance() {
		return INSTANCE;
	}

	static {
//		INSTANCE = gs();
		final PropertiesConfiguration propertiesConfiguration  = gs();
		final ZFrameworkProperties init = newWriteDP(propertiesConfiguration);
		INSTANCE = init;
	}

	private static ZFrameworkProperties newWriteDP(final PropertiesConfiguration propertiesConfiguration) {
		final ZFrameworkProperties zDatasourceProperties = new ZFrameworkProperties();
		zDatasourceProperties.setServerPort(propertiesConfiguration.getInt("server.port"));
		zDatasourceProperties.setThreadCount(propertiesConfiguration.getInt("server.thread.count"));
		return zDatasourceProperties;

	}

	private static PropertiesConfiguration gs() {
		try {
			return new PropertiesConfiguration(DATASOURCE_PROPERTIES);
		} catch (final ConfigurationException e) {
			e.printStackTrace();
			try {
				return new PropertiesConfiguration(DATASOURCE_PROPERTIES_2);
			} catch (final ConfigurationException e1) {
				e1.printStackTrace();
				try {
					return new PropertiesConfiguration(DATASOURCE_PROPERTIES_3);
				} catch (final ConfigurationException e2) {
					e2.printStackTrace();
					try {
						return new PropertiesConfiguration(DATASOURCE_PROPERTIES_4);
					} catch (final ConfigurationException e3) {
						e3.printStackTrace();
					}
				}
			}
		}

		return null;

	}
}
