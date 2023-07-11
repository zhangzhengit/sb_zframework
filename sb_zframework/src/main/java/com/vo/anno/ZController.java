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
public @interface ZController {

	/**
	 * 匹配的路径前缀
	 *
	 * @return
	 *
	 */
	// FIXME 2023年7月9日 上午10:19:32 zhanghen: 暂未实现，注释掉
//	String prefix() default "";

	BeanModeEnum modeEnum() default BeanModeEnum.SINGLETON;

}
