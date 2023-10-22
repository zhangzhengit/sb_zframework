package com.vo.core;

/**
 * http接口参数对解析异常
 *
 * @author zhangzhen
 * @date 2023年10月22日
 *
 */
public class FormPairParseException extends ZFException {

	private static final long serialVersionUID = 1L;

	public static final String PREFIX = "接口参数解析异常：";

	public FormPairParseException(final String message) {
		super(PREFIX + message);
	}

}
