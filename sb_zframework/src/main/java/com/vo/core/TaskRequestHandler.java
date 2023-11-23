package com.vo.core;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableCollection;
import com.vo.exception.StartupException;

/**
 * 处理请求
 *
 * @author zhangzhen
 * @date 2023年11月23日
 *
 */
public class TaskRequestHandler extends Thread {

	public static final String NAME = "request-Dispatcher-Thread";

	public static final String USER_AGENT = "User-Agent";
	private final BlockingQueue<TaskRequest> queue = new LinkedBlockingQueue<>();
	private final AbstractRequestValidator requestValidator;

	public TaskRequestHandler() {
		this.setName(NAME);

		final ImmutableCollection<Object> beanConnection = ZContext.all().values();

		final List<RequestValidatorAdapter> childList = beanConnection.stream()
				.filter(bean -> bean.getClass().getSuperclass().getCanonicalName()
						.equals(RequestValidatorAdapter.class.getCanonicalName()))
				.map(bean -> (RequestValidatorAdapter) bean).collect(Collectors.toList());

		if (childList.isEmpty()) {
			final RequestValidatorAdapter requestValidatorDefault = ZSingleton
					.getSingletonByClass(RequestValidatorAdapter.class);
			ZContext.addBean(RequestValidatorAdapter.class, requestValidatorDefault);
			this.requestValidator = requestValidatorDefault;
		} else {
			if (childList.size() > 1) {
				final String beanName = childList.stream().map(bean -> bean.getClass().getSimpleName())
						.collect(Collectors.joining(","));
				final String message = RequestValidatorAdapter.class.getCanonicalName() + " 只能有一个子类，当前有["
						+ childList.size() + "]个,[" + beanName + "]";
				throw new StartupException(message);
			}

			this.requestValidator = childList.get(0);
		}

	}

	@Override
	public void run() {
		while (true) {
			try {
				final TaskRequest taskRequest = this.queue.take();
				final String requestString = new String(taskRequest.getRequestData(), NioLongConnectionServer.CHARSET)
						.intern();
				final Task task = new Task(taskRequest.getSocketChannel());
				final ZRequest request = task.handleRead(requestString);
				this.requestValidator.hand(request, taskRequest);
			} catch (final Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	public void responseAsync(final TaskRequest taskRequest) {
		this.queue.add(taskRequest);
	}
}
