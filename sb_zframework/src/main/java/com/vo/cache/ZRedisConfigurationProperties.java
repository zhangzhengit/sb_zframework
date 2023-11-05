package com.vo.cache;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZNotEmtpy;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * redis配置项
 *
 * @author zhangzhen
 * @date 2023年11月5日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "redis")
public class ZRedisConfigurationProperties {

	@ZNotEmtpy
	private String host = "localhost";

	@ZNotNull
	private Integer port = 6379;

	@ZNotNull
	private Integer timeout = 2000;

	@ZNotNull
	private Integer maxTotal = 20;

	@ZNotNull
	private Integer maxIdle = 2;

	private String password;

}
