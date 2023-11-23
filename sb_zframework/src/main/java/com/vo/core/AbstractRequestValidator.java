package com.vo.core;

import com.vo.conf.ServerConfiguration;

/**
 *
 * 请求验证的默认实现，默认实现为根据请求的clientip和User-Agent来判断QPS不能超过 [server.client.qps] 配置项的值，
 * 超过则返回429，不超过则正常处理请求。
 *
 * 如需自定义，覆盖 RequestValidator 的方法
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
abstract class AbstractRequestValidator {

	public void hand(final ZRequest request, final TaskRequest taskRequest) {
		final boolean validated = this.validated(request, taskRequest);
		if (validated) {
			this.passed(request, taskRequest);
		} else {
			this.failed(request, taskRequest);
		}
	}

	public int qps() {
		return ZContext.getBean(ServerConfiguration.class).getClientQPS();
	}

	/**
	 * 校验 此请求是否放行，默认实现为 根据clientIp和User-Agent判断QPS不能超过 DEFAULT_QPS
	 *
	 * @param request
	 * @param taskRequest TODO
	 * @return
	 *
	 */
	public boolean validated(final ZRequest request, final TaskRequest taskRequest) {
		final String clientIp = request.getClientIp();
		final String userAgent = request.getHeader(TaskRequestHandler.USER_AGENT);
		final String keyword = clientIp + "@" + userAgent;
		final boolean allow = QPSCounter.allow(keyword, this.qps());
		return allow;
	}

	/**
	 * 不放行怎么处理，默认实现为返回 429 并且关闭连接
	 *
	 * @param request
	 *
	 */
	public void failed(final ZRequest request, final TaskRequest taskRequest) {
		NioLongConnectionServer.response429(taskRequest.getSelectionKey());
	}

	/**
	 * 放行怎么处理，默认实现为继续走后面的流程
	 *
	 * @param request
	 *
	 */
	public void passed(final ZRequest request, final TaskRequest taskRequest) {
		NioLongConnectionServer.responseAsync(request, taskRequest);
	}

}
