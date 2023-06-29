package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.groovy.runtime.dgmimpl.arrays.BooleanArrayGetAtMetaMethod;
import org.springframework.stereotype.Component;

/**
 * get 请求
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Component
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZGet {

	/**
	 * 请求路径，如： /index
	 *
	 * @return
	 *
	 */
	String path();

	/**
	 * path 是否正则表达式，默认fasle
	 *
	 * @return
	 *
	 */
	boolean isRegex() default false;

}
