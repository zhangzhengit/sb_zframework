package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在字段上，表示此字段值自动注入
 *
 * @author zhangzhen
 * @date 2023年6月19日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZAutowired {

	/**
	 * 依赖注入的组件名称，用于注入手动注入的组件或者配置类里多个@ZBean返回值相同的方法声明的组件.
	 * 不设值则默认为类的全名，如：com.vo.test.Bean
	 * 设值了则按名称来，如：name="bean1" 、 name="bean2"
	 *
	 * @return
	 *
	 */
	String name() default "";

}
