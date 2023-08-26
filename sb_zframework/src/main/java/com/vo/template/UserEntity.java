package com.vo.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年8月1日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

	private String userName;
	private Integer status;

}
