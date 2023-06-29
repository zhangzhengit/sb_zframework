package com.vo.scanner;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在接口方法的参数中，表示此值是一个可变量
 *
 * @author zhangzhen
 * @date 2023年6月28日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface ZPathVariable {

	String name() default "";

}
