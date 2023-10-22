package com.vo.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * zframework 统一异常类
 *
 * @author zhangzhen
 * @date 2023年10月22日
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
class ZFException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private String message;

}
