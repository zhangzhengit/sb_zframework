package com.vo;

import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
public class ZApplicationContext {

	private List<String> scanPackageNameList;
	private boolean httpEnable;
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
	public ImmutableMap<String, Object> getAllBeans() {
		return ZContext.all();
	}

}
