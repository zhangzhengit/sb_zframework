package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在 ZControllerInterceptor 的子类上，
 * 表示多个子类的执行顺序，从小到大执行，取值为int范围
 *
 * @author zhangzhen
 * @date 2023年7月11日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZOrder {

	int value();

}
