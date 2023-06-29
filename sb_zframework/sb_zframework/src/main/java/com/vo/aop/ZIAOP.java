package com.vo.aop;

/**
 * AOP顶级接口,所有自定义的AOP类都要实现此接口
 *
 * @param <CT>
 *
 * @author zhangzhen
 * @date 2023年6月18日
 *
 */
public interface ZIAOP {

	public Object before(AOPParameter AOPParameter);

	public Object around(AOPParameter AOPParameter);

	public Object after(AOPParameter AOPParameter);

}
