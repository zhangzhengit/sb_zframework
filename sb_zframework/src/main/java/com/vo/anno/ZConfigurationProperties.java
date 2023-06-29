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
 * 用在type上，表示此类是读取配置文件的配置类，此类专用于对应配置文件，
 * 不要有其他任何内容，仅作为配置类使用
 *
 * java 驼峰式名称，会首先匹配配置文件中对应的驼峰式名称，
 * 无则继续匹配[大写字母]替换为[.小写字母]形式的名称.
 *
 * 如： java 名称 orderCount
 *
 * 	会先进行匹配	orderCount
 * 	无则继续匹配	order.count
 *
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
	String prefix() default "";

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
