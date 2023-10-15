package com.vo.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示启用校验（ZNotNull等用在字段上的注解）
 *
 * @author zhangzhen
 * @date 2023年10月15日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER })
public @interface ZValidated {

}
