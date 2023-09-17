package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 用在字段上，表示此字段值从配置文件中取。
 * 如下，表示从配置文件中取 a.b 的值赋给String b字段。
 *
 *  @ZValue(name = "a.b")
 * 	private String b;
 *
 * @author zhangzhen
 * @date 2023年6月29日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZValue {

	/**
	 * 对应配置文件中的key，来获取此key对应的value
	 *
	 * @return
	 *
	 */
	String name();

	/**
	 * 是否监听配置文件变动来更新值，可选项，默认不监听
	 *
	 * @return
	 *
	 */
	boolean listenForChanges() default false;

}
