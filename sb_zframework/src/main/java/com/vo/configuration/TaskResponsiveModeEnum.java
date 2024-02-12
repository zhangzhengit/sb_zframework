package com.vo.configuration;

/**
 *
 * 对于请求的响应模式
 *
 * @author zhangzhen
 * @date 2024年2月10日
 *
 */
public enum TaskResponsiveModeEnum {

	/**
	 * 立即响应，如果当前有空闲线程，则立即执行；否则重新入列等待执行，知道超时了返回错误码
	 */
	IMMEDIATELY,

	/**
	 * 队列模式，放入队列等待响应
	 */
	QUEUE;

}
