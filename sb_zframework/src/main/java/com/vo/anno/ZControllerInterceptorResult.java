package com.vo.anno;

/**
 *	暂存 ZControllerInterceptor 的子类的around里
 *	执行目标方法的结果，暂存起来，在 @ZOrder.value
 *	靠后的 子类里使用 get方法来获取，防止重复执行目标方法.
 *
 * @author zhangzhen
 * @date 2023年7月11日
 *
 */
public class ZControllerInterceptorResult {

	static ThreadLocal<Object> r = new ThreadLocal<>();

	public static void set(final Object result) {
		r.set(result);
	}

	public static Object get() {
		final Object v = r.get();
		r.remove();
		return v;
	}
}
