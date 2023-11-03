package com.vo.http;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMax;
import com.vo.validator.ZMin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ZRequestMapping 的配置类
 *
 * @author zhangzhen
 * @date 2023年11月3日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "request.mapping")
public class ZRequestMappingConf {

	@ZMin(min = 1)
	@ZMax(max = Integer.MAX_VALUE)
	private Integer qps = 5000;
}
