package com.vo.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用在接口方法上，表示此接口根据什么来限制QPS
 *
 * @author zhangzhen
 * @date 2023年7月17日
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface ZQPSLimitation {

	/**
	 * 最大QPS限制
	 *
	 * @return
	 *
	 */
	int qps();

	/**
	 * 根据什么来限制
	 *
	 * @return
	 *
	 */
	ZQPSLimitationEnum type();

}
