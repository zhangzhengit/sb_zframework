package com.vo.scanner;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年11月15日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "application.event")
public class ZApplicationEventConfigurationProperties {

	/**
	 * 处理事件请求的最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = 100)
	private Integer threadCount = Runtime.getRuntime().availableProcessors();

	@ZNotEmtpy
	private String threadNamePrefix = "applicationEvent-Thread-";
}
