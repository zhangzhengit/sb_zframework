package com.vo;

import com.vo.anno.ZComponent;
import com.vo.core.ZContext;
import com.vo.validator.ZFException;
import com.votool.common.CR;

/**
 * @ZControllerAdvice 的默认处理方法，如果 @ZExceptionHandler 定义的
 *                    的处理方法中，没有任何一个匹配异常，则使用本类来处理。
 *
 * @author zhangzhen
 * @date 2023年11月4日
 *
 */
@ZComponent
public class ZControllerAdviceThrowable {

	public CR throwZ(final Throwable throwable) {
		final String m = findCausedby(throwable);
		final ZControllerAdviceThrowableConfigurationProperties conf = ZContext.getBean(ZControllerAdviceThrowableConfigurationProperties.class);
		return CR.error(conf.getErrorCode(), m);
	}

	public static String findCausedby(final Throwable e) {
		if (e instanceof ZFException) {
			return ((ZFException) e).getMessage();
		}

		if (e.getCause() != null && e.getCause() instanceof ZFException) {
			return ((ZFException) e.getCause()).getMessage();
		}

		if (e.getCause() != null) {
			return e.getCause().getClass().getCanonicalName() + ":" + e.getCause().getMessage();
		}

		return e.getClass().getCanonicalName() + ":" + e.getMessage();
	}

}
