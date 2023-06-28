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
 * 用在type上，表示此类是读取配置文件的配置类
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
@Component
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface ZConfigurationProperties {

	/**
	 * 对应的配置文件的前缀
	 *
	 * @return
	 *
	 */
	String prefix();

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
