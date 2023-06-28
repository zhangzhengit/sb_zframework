package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * 标记一个ZController中的方法，表示此方法返回HTML
 *
 * @author zhangzhen
 * @date 2023年6月24日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZHtml {

	// FIXME 2023年6月24日 下午8:52:45 zhanghen: TODO ，启动时，校验：此注解的方法的返回值必须是String
}
