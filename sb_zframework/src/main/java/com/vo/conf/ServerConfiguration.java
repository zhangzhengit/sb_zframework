package com.vo.conf;

import com.vo.anno.ZConfigurationProperties;
import com.vo.validator.ZMin;
import com.vo.validator.ZNotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * server相关的配置
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ZConfigurationProperties(prefix = "server")
public class ServerConfiguration {

	@ZNotNull
	@ZMin(min = 1)
	private Integer port;

	@ZNotNull
	private Integer threadCount;

	@ZNotNull
	private String scanPackage;

	@ZNotNull
	// FIXME 2023年7月1日 上午4:21:59 zhanghen:  @ZMin在此设为0作为一个feature？可以配置为0让应用拒绝一切服务
	@ZMin(min = 0)
//	@ZMin(min = 520)
	private Integer concurrentQuantity;

}
