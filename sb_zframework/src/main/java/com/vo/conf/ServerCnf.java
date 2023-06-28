package com.vo.conf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *	server相关的配置
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerCnf {

	/**
	 * 服务端口
	 */
	private Integer port;

	private Integer threadCount;

}
