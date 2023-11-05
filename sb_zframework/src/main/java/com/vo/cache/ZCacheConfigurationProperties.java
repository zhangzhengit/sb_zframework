package com.vo.cache;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotEmtpy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * cache配置项
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "cache")
public class ZCacheConfigurationProperties {

	@ZNotEmtpy
	private String type = ZCacheConfiguration.DEFAULT;

}
