package com.vo.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vo.enums.BeanModeEnum;

/**
 *
 * 用在type上，表示此类是一个ZController，用于处理http请求
 *
 * @author zhangzhen
 * @date 2023年6月12日
 *
 */
// FIXME 2023年7月11日 下午8:40:15 zhanghen:  TODO AOP 自定义注解还只支持 @ZComponent 组件，
// 还不支持 @ZController ，现在问题： ZClass 使用groovy 动态编译，构造出来的ZClass代理类，复制出原@ZController类
// 的 @ZRequestMapping注解后，使用java方式获取不到注解
// 解决方式 1 ：改用java动态编译，在试好多报错
// 		   2 ： 先支持ZGet 、 ZPost 等简单注解
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
// FIXME 2023年8月30日 下午9:02:42 zhanghen: OK @ZController 可以像 @ZComponent 组件 那样简单加入注解A，然后定义 ZIAOP的子类拦截A来实现AOP了
// 在Task中method.invoke之前调用了AOP的before、around、after方法实现的
// 但是 ZIAOP 的方法定义似乎还有问题，因为如果一个method有多个注解
// 是 把多个注解的逻辑合并到一个AOP类中？并且判断一个method 只能有一个AOP类？似乎不太合理

public @interface ZController {

	/**
	 * 接口的路径前缀，如：/test，
	 * 则本类下的接口如： @ZRequestMapping(mapping = { "/ok" })
	 * 则次接口 mapping值为加入/test后的：/test/ok。
	 *
	 * 本属性值默认为""
	 *
	 * @return
	 *
	 */
	String prefix() default "";

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
