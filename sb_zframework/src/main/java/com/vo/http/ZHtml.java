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

// FIXME 2023年8月30日 下午6:22:11 zhanghen: TODO @ZController 里的方法上，加入自定义注解 A
// 像 test项目中 ZLogAOP 那样来实现AOP
public @interface ZHtml {

}
