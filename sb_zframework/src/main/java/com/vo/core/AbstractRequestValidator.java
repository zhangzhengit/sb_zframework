package com.vo.core;

import com.vo.configuration.ServerConfiguration;

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

	public void handle(final ZRequest request, final TaskRequest taskRequest) {
		final boolean validated = this.validated(request, taskRequest);
		if (validated) {
			this.passed(request, taskRequest);
		} else {
			this.failed(request, taskRequest);
		}
	}

	public int qps() {
		return ZContext.getBean(ServerConfiguration.class).getClientQps();
	}

	/**
	 * 校验此请求是否放行，默认实现为：
	 *
	 * 1、如果启用了 响应 ZSESSIONID并且服务器中存在对应的session则按ZSESSIONID来判断为同一个客户端
	 * 2、没启用ZSESSIONID，则根据clientIp和User-Agent来判断为同一个客户端
	 *
	 * 	判断QPS不能超过 配置的值
	 *
	 * @param request
	 * @param taskRequest
	 * @return
	 *
	 */
	public boolean validated(final ZRequest request, final TaskRequest taskRequest) {

		// 如果启用了响应 ZSESSIONID，则认为ZSESSIONID相同就是同一个客户端(前提是服务器中存在对应的session，因为session可能是伪造的等，服务器重启就重启就认为是无效session)
		if (Boolean.TRUE.equals(ZContext.getBean(ServerConfiguration.class).getResponseZSessionId())) {
			final ZSession session = request.getSession(false);
			if (session != null) {
				final String keyword = ZRequest.Z_SESSION_ID + "@" + session.getId();
				final boolean allow = QPSCounter.allow(keyword, this.qps(), QPSEnum.CLIENT);
				return allow;
			}
		}

		// 启用了响应 ZSESSIONID，则认为 clientIp和User-Agent都相同就是同一个客户端
		final String clientIp = request.getClientIp();
		final String userAgent = request.getHeader(TaskRequestHandler.USER_AGENT);
		final String keyword = clientIp + "@" + userAgent;
		final boolean allow = QPSCounter.allow(keyword, this.qps(), QPSEnum.CLIENT);
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
