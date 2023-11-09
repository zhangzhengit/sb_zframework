package com.vo.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 用在 @ZConfigurationProperties 的类的List类型的字段上，表示此List的泛型类型
 *
 *
 * @author zhangzhen
 * @date 2023年11月9日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ZType {

	Class<?> type();

}
