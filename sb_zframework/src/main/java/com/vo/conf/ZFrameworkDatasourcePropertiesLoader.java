package com.vo.conf;

import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.Lists;
import com.vo.core.ZRequest;
import com.vo.core.ZLog2;
import com.vo.core.ZServer;

import cn.hutool.core.util.StrUtil;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月17日
 *
 */
public class ZFrameworkDatasourcePropertiesLoader {

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final String PROPERTIES = "zframework.properties";

	public static final String PROPERTIES_1 = "config/zframework.properties";
	public static final String PROPERTIES_2 = "zframework.properties";
	public static final String PROPERTIES_3 = "src/main/resources/zframework.properties";
	public static final String PROPERTIES_4 = "src/main/resources/config/zframework.properties";

	public static final List<String> PROPERTIES_LIST = Lists.newArrayList(PROPERTIES, PROPERTIES_1, PROPERTIES_2,
			PROPERTIES_3, PROPERTIES_4);

	private static ZFrameworkProperties INSTANCE;

	public static ZFrameworkProperties getFrameworkPropertiesInstance() {
		return INSTANCE;
	}

	static {
//		INSTANCE = gs();
		final PropertiesConfiguration propertiesConfiguration  = gs();
		final ZFrameworkProperties init = newP(propertiesConfiguration);
		INSTANCE = init;
		LOG.info("初始化配置文件完成,p={}", INSTANCE);
	}

	private static ZFrameworkProperties newP(final PropertiesConfiguration propertiesConfiguration) {
		final ZFrameworkProperties zDatasourceProperties = new ZFrameworkProperties();
		zDatasourceProperties.setServerPort(propertiesConfiguration.getInt("server.port", ZServer.DEFAULT_HTTP_PORT));
		zDatasourceProperties.setThreadCount(
				propertiesConfiguration.getInt("server.thread.count", Runtime.getRuntime().availableProcessors()));
		final String threadNamePrefix = propertiesConfiguration.getString("server.thread.name.prefix");
		if (StrUtil.isEmpty(threadNamePrefix)) {
			zDatasourceProperties.setThreadNamePrefix(ZServer.DEFAULT_ZFRAMEWORK_HTTP_THREAD_NAME_PREFIX);
		} else {
			zDatasourceProperties.setThreadNamePrefix(threadNamePrefix);
		}

		final String scanPackage = propertiesConfiguration.getString("server.scan.package");
		if (StrUtil.isEmpty(scanPackage)) {
//			throw new IllegalArgumentException("server.scan.package 不能配置为空");
		}
		zDatasourceProperties.setScanPackage(scanPackage);
		return zDatasourceProperties;

	}

	private static PropertiesConfiguration gs() {
		try {
			return new PropertiesConfiguration(PROPERTIES);
		} catch (final ConfigurationException e) {
			e.printStackTrace();
			try {
				return new PropertiesConfiguration(PROPERTIES_2);
			} catch (final ConfigurationException e1) {
				e1.printStackTrace();
				try {
					return new PropertiesConfiguration(PROPERTIES_3);
				} catch (final ConfigurationException e2) {
					e2.printStackTrace();
					try {
						return new PropertiesConfiguration(PROPERTIES_4);
					} catch (final ConfigurationException e3) {
						e3.printStackTrace();
					}
				}
			}
		}

		return null;

	}
}
