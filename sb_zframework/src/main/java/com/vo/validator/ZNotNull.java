package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 放在字段上，表示此字段不能为null
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZNotNull {

	public static final String MESSAGE = "[%s]不能为null";

	/**
	 *
	 * 当此注解标记的字段为null时的提示语，默认为上面的 ZNotNull.MESSAGE
	 *
	 * @return
	 *
	 */
//	String message() default "";

}
