package com.vo.validator;

/**
 *
 * @Validated 所校验的注解所抛出的异常
 *
 * @author zhangzhen
 * @date 2023年10月22日
 *
 */
public class ValidatedException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "validate异常：";

	public ValidatedException(final String message) {
		super(PREFIX + message);
	}

}
