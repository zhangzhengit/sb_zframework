package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

import com.vo.enums.BeanModeEnum;

/**
 *
 * 用在方法上，把此方法的返回值声明为一个Bean，用方法名称作为beanName
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Component
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZBean {

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
