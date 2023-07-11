package com.vo.anno;

import com.vo.aop.InterceptorParameter;

/**
 * @ZController 拦截器，用于拦截 @ZController 里面的接口方法。 实现此类，实现方法逻辑.
 * 如：
 *       如果接口方法名称是什么/包含什么则做什么
 *       如果接口带有什么参数则做什么
 *       如果接口方法带什么注解则做什么（类似 @ZComponent 上面的AOP自定义注解） ...........
 *
 * 子类自动被容器管理，可以使用 @ZAutowired 来注入
 *
 * @author zhangzhen
 * @date 2023年7月11日
 *
 */
// FIXME 2023年7月11日 下午9:43:40 zhanghen: 考虑多个子类的情况下，是否有需要按顺序执行的情况
// 如：I1 记录日志 然后 I2 做其他什么,I3继续做什么
public interface ZControllerInterceptor {

	void before(InterceptorParameter interceptorParameter);

	/**
	 *
	 * @param interceptorParameter
	 * @param result	上一个拦截器的执行结果
	 * @return
	 */
	// FIXME 2023年7月11日 下午10:15:18 zhanghen: result暂时传入，使用 ZControllerInterceptorResult.set get
	Object around(InterceptorParameter interceptorParameter, Object result);

	void after(InterceptorParameter interceptorParameter);

}
