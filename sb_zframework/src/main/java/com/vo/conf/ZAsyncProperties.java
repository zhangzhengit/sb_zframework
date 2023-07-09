package com.vo.conf;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ZAsync 用到的线程池的相关配置
 *
 * @author zhangzhen
 * @date 2023年7月8日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "async")
public class ZAsyncProperties {

	/**
	 * 最大线程数量
	 */
	@ZNotNull
	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	private Integer threadCount;

	/**
	 * 线程名称前缀
	 */
	@ZNotNull
	private String threadNamePrefix;

}
