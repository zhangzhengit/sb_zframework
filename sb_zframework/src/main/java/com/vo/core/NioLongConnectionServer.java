package com.vo.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZServer.Counter;
import com.vo.enums.ConnectionEnum;
import com.vo.enums.MethodEnum;
import com.vo.http.HttpStatus;
import com.vo.http.ZCookie;
import com.votool.common.CR;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NIO长连接server
 *
 * @author zhangzhen
 * @date 2023年7月4日
 *
 */
public class NioLongConnectionServer {

//	public static final Charset CHARSET = Charset.defaultCharset();
//	public static final Charset CHARSET = Charset.forName("UTF-8");
	public static final Charset CHARSET = Charset.forName("ISO-8859-1");

	private static final String AT = "at";

	private static final String CAUSED_BY = "Caused by: ";

	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final String SERVER = HttpHeaderEnum.SERVER.getValue();

	public static final String SERVER_VALUE = ZContext.getBean(ServerConfiguration.class).getName();
	public static final String CONNECTION = HttpHeaderEnum.CONNECTION.getValue();

	/**
	 * 执行长连接超时任务的线程池
	 */
	private final static ScheduledExecutorService TIMEOUT_ZE = Executors.newScheduledThreadPool(1);

	/**
	 *	存放长连接的SocketChannel对象
	 *
	 */
	// FIXME 2023年7月5日 上午6:56:44 zhanghen: 改为自最后一次活动后开始计时，超时后关闭
	private final static Map<Long, SS> SOCKET_CHANNEL_MAP = new ConcurrentHashMap<>(16, 1F);

	private static final ServerConfiguration SERVER_CONFIGURATION = ZSingleton.getSingletonByClass(ServerConfiguration.class);

//	private static final int BUFFER_SIZE = Integer.MAX_VALUE - 2000;
//	private static final int BUFFER_SIZE = 1024 * 44;
	private static final int BUFFER_SIZE = 1024;

	public void startNIOServer(final Integer serverPort) {

		keepAliveTimeoutJOB();

		LOG.trace("zNIOServer开始启动,serverPort={}",serverPort);

		// 创建ServerSocketChannel
		Selector selector = null;
		ServerSocketChannel serverSocketChannel;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.bind(new InetSocketAddress(serverPort));

			// 创建Selector
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (final IOException e) {
			e.printStackTrace();
			LOG.error("启动失败,程序即将退出,serverPort={}", serverPort);
			System.exit(0);
		}

		LOG.trace("zNIOServer启动成功，等待连接,serverPort={}", serverPort);

		while (true) {
			try {
				selector.select();
			} catch (final IOException e) {
				e.printStackTrace();
			}

			final Set<SelectionKey> selectedKeys = selector.selectedKeys();
			final Iterator<SelectionKey> iterator = selectedKeys.iterator();

			while (iterator.hasNext()) {
				final SelectionKey key = iterator.next();
				iterator.remove();

				if (!key.isValid()) {
					continue;
				}

				if (key.isAcceptable()) {
					handleAccept(key, selector);
				} else if (key.isReadable()) {
					if (Counter.allow(ZServer.Z_SERVER_QPS, SERVER_CONFIGURATION.getQps())) {
						this.handleRead(key);
//						this.handleRead2(key);
					} else {
						final String message = "服务器访问频繁，请稍后再试";
						new ZResponse((SocketChannel) key.channel())
							.contentType(HeaderEnum.JSON.getType())
							.httpStatus(HttpStatus.HTTP_403.getCode())
							.body(JSON.toJSONString(CR.error(message)))
							.write();
					}
				}
			}
		}
	}

	private static void keepAliveTimeoutJOB() {

		final Integer keepAliveTimeout = SERVER_CONFIGURATION.getKeepAliveTimeout();
		LOG.info("长连接超时任务启动,keepAliveTimeout=[{}]秒", SERVER_CONFIGURATION.getKeepAliveTimeout());

		TIMEOUT_ZE.scheduleAtFixedRate(() -> {

			if (SOCKET_CHANNEL_MAP.isEmpty()) {
				return;
			}

			final long now = System.currentTimeMillis();

			final Set<Long> keySet = SOCKET_CHANNEL_MAP.keySet();

			final List<Long> delete = new ArrayList<>(10);

			for (final Long key : keySet) {
				if (now - key >= keepAliveTimeout * 1000) {
					delete.add(key);
				}
			}

			for (final Long k : delete) {
				final SS ss = SOCKET_CHANNEL_MAP.remove(k);
				synchronized (ss.getSocketChannel()) {
					try {
						ss.getSocketChannel().close();
						ss.getSelectionKey().cancel();
					} catch (final IOException e) {
						e.printStackTrace();
					}
				}
			}

		}, 1, 1, TimeUnit.SECONDS);
	}

	private static void handleAccept(final SelectionKey key, final Selector selector) {
		final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel socketChannel = null;
		try {
			socketChannel = serverSocketChannel.accept();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			socketChannel.configureBlocking(false);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			socketChannel.register(selector, SelectionKey.OP_READ);
		} catch (final ClosedChannelException e) {
			e.printStackTrace();
		}
//		try {
////			System.out.println("新连接： " + socketChannel.getRemoteAddress());
//		} catch (final IOException e) {
//			e.printStackTrace();
//		}

	}

	private void handleRead2(final SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		try {
			socketChannel.configureBlocking(false);
			final InputStream inputStream = socketChannel.socket().getInputStream();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				System.out.println(line);
			}
			System.out.println();

		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void handleRead(final SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		if (!socketChannel.isOpen()) {
			return;
		}

		int bytesRead = 0;
		final ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		final ZArray array = new ZArray();
		try {
			while (true) {
				final int tR = socketChannel.read(byteBuffer);
				if (tR <= 0) {
					break;
				}
				bytesRead += tR;

				final int position = byteBuffer.position();
				byteBuffer.flip();
				array.add(byteBuffer.array(), 0, position);
				byteBuffer.clear();
			}

		} catch (final IOException e) {
			return;
		}

		if (bytesRead == -1) {
			try {
				socketChannel.close();
				key.cancel();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return;
		}

		if ((bytesRead > 0) && socketChannel.isOpen()) {

			ZServer.ZE.executeInQueue(() -> {
				synchronized (socketChannel) {
					final byte[] requestData = array.get();

					final String requestString = new String(requestData,CHARSET);

					final Task task = new Task(socketChannel);
					final ZRequest request = task.handleRead(requestString);
					request.setOriginalRequestBytes(requestData);
					if (socketChannel.isOpen()) {
						NioLongConnectionServer.response(key, socketChannel, requestString, request);
					}
				}

			});

		}
	}


	private static void response(final SelectionKey key, final SocketChannel socketChannel, final String requestString, final ZRequest request) {

		final Task task = new Task(socketChannel);

		// FIXME 2023年8月11日 下午9:27:23 zhanghen: debug syso requestString
//		System.out.println("requestString = \n");
//		System.out.println(requestString);
//		System.out.println();


		final ZRequest zRequest = request;
//		final ZRequest zRequest = task.handleRead(requestString);

		final String connection = zRequest.getHeader(HttpHeaderEnum.CONNECTION.getValue());
		final boolean keepAlive = StrUtil.isNotEmpty(connection)
				&& (connection.equalsIgnoreCase(ConnectionEnum.KEEP_ALIVE.getValue())
						|| connection.toLowerCase()
								.contains(ConnectionEnum.KEEP_ALIVE.getValue().toLowerCase()));

		// 解析请求时，无匹配的Method
		if (zRequest.getRequestLine().getMethodEnum() == null) {
			final MethodEnum[] values = MethodEnum.values();
			final String methodString = Lists.newArrayList(values).stream().map(e -> e.getMethod()).collect(Collectors.joining(","));
			new ZResponse(socketChannel)
				.header(ZRequest.ALLOW, methodString)
				.httpStatus(HttpStatus.HTTP_405.getCode())
				.contentType(HeaderEnum.JSON.getType())
				.body(JSON.toJSONString(CR.error(HttpStatus.HTTP_405.getCode(), HttpStatus.HTTP_405.getMessage())))
				.write();
		} else {
			try {
				final ZResponse response = task.invoke(zRequest);
				if (response != null && !response.getWrite().get()) {

					response.header(SERVER, SERVER_VALUE);

					final ZSession sessionFALSE = zRequest.getSession(false);
					if (sessionFALSE == null) {
						final ZSession sessionTRUE = zRequest.getSession(true);
						final ZCookie cookie =
								new ZCookie(ZRequest.Z_SESSION_ID, sessionTRUE.getId())
								.path("/")
								.httpOnly(true);
						response.cookie(cookie);
					}

					if (keepAlive) {
						response.header(CONNECTION, ConnectionEnum.KEEP_ALIVE.getValue());
					}

					final Map<String, String> responseHeaders = SERVER_CONFIGURATION.getResponseHeaders();
					if (CollUtil.isNotEmpty(responseHeaders)) {
						final Set<Entry<String, String>> entrySet = responseHeaders.entrySet();
						for (final Entry<String, String> entry : entrySet) {
							response.header(entry.getKey(), entry.getValue());
						}
					}

					// 在此自动write，接口中可以不调用write
					response.write();
				}
			} catch (final Exception e) {

				// FIXME 2023年10月15日 下午7:47:12 zhanghen: XXX 直接把真实的报错信息给客户端？还是只告诉客户端一个ERROR
				final String message = Task.gExceptionMessage(e);

				LOG.error("执行错误,message={}", message);

				new ZResponse(socketChannel)
						.httpStatus(HttpStatus.HTTP_500.getCode())
						.contentType(HeaderEnum.JSON.getType())
						.body(JSON.toJSONString(
								CR.error(HttpStatus.HTTP_500.getCode(), findCausedby(e, message))))
						.write();

			}
		}

		if (keepAlive) {
			SOCKET_CHANNEL_MAP.put(System.currentTimeMillis() / 1000 * 1000, new SS(socketChannel, key));
		} else {
			try {
				socketChannel.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static String findCausedby(final Exception e, final String errorMessage) {
		if (e instanceof ZFException) {
			return ((ZFException) e).getMessage();
		}

		final String[] ma = errorMessage.split(Task.NEW_LINE);
		for (final String s : ma) {
			if(s.startsWith(CAUSED_BY)) {
				return s.substring(s.indexOf(CAUSED_BY) + CAUSED_BY.length());
			}
		}

		final int start = errorMessage.indexOf(CAUSED_BY);
		if (start > -1) {
			final int end = errorMessage.indexOf(AT, start);
			if (end > start) {
				final String e1 = errorMessage.substring(start + CAUSED_BY.length(), end);
				return e1;
			}
		}
		return errorMessage;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SS {

		private SocketChannel socketChannel;
		private SelectionKey selectionKey;

	}

}
