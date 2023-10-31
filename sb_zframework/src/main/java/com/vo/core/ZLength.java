package com.vo.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 放在字段上，表示此字段值必须在 min 和max 之间。
 * 可用于String类型。
 *
 * 此注解自动包含 @ZNotNull 的功能，此注解标记的字段上自动判断值不能为null，无需再加入 @ZNotNull
 *
 * @author zhangzhen
 * @date 2023年10月31日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZLength {

	public static final String MESSAGE_MIN = "[%s]长度不能小于[%s]";

	public static final String MESSAGE_MAX= "[%s]长度不能大于[%s]";


	int min() default 0;

	int max() default Integer.MAX_VALUE;

}
