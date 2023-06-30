package com.vo.conf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZFrameworkProperties {

	private Integer serverPort;
	private Integer threadCount;
	private String threadNamePrefix;
	private String scanPackage;

}
