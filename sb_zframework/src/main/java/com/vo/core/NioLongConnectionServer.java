package com.vo.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import com.votool.common.CR;

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



	private static final ZLog2 LOG = ZLog2.getInstance();

	public static final String SERVER = "Server";
	public static final String Z_SERVER = "zserver";
	public static final String CONNECTION = "Connection";

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

	private static final int BUFFER_SIZE = 1024 * 100;

	public void startNIOServer() {

		keepAliveTimeoutJOB();

		LOG.trace("zNIOServer开始启动,serverPort={}",SERVER_CONFIGURATION.getPort());

		// 创建ServerSocketChannel
		Selector selector = null;
		ServerSocketChannel serverSocketChannel;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.bind(new InetSocketAddress(SERVER_CONFIGURATION.getPort()));

			// 创建Selector
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		LOG.trace("zNIOServer启动成功，等待连接,serverPort={}", SERVER_CONFIGURATION.getPort());

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
					if (Counter.allow(ZServer.Z_SERVER_QPS, SERVER_CONFIGURATION.getConcurrentQuantity())) {
						this.handleRead(key);
					} else {
						new ZResponse((SocketChannel) key.channel())
							.contentType(HeaderEnum.JSON.getType())
							.httpStatus(HttpStatus.HTTP_403.getCode())
							.body(JSON.toJSONString(CR.error("zserver-超出QPS限制,qps = " + SERVER_CONFIGURATION.getConcurrentQuantity())))
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

	private void handleRead(final SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		if (!socketChannel.isOpen()) {
			return;
		}

		int bytesRead = -4;
		final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		try {
			bytesRead = socketChannel.read(buffer);
		} catch (final IOException e) {
			return;
		}

		if (bytesRead == -1) {
			try {
				socketChannel.close();
				key.cancel();
//				System.out.println("bytesRead == -1 | socketChannel.close();");
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return;
		}

		if (bytesRead > 0) {
			buffer.flip();
			final byte[] requestData = new byte[buffer.remaining()];
			buffer.get(requestData);

			final String requestString = new String(requestData, StandardCharsets.UTF_8);

			if (socketChannel.isOpen()) {

				ZServer.ZE.executeInQueue(() -> {
					synchronized (socketChannel) {
						if(socketChannel.isOpen()) {

							final Task task = new Task(socketChannel);
							final ZRequest zRequest = task.handleRead(requestString);

							final String connection = zRequest.getHeader(CONNECTION);
							final boolean keepAlive = StrUtil.isNotEmpty(connection)
									&& (connection.equalsIgnoreCase(ConnectionEnum.KEEP_ALIVE.getValue())
											|| connection.toLowerCase()
													.contains(ConnectionEnum.KEEP_ALIVE.getValue().toLowerCase()));
							if (keepAlive) {
								SOCKET_CHANNEL_MAP.put(System.currentTimeMillis() / 1000 * 1000, new SS(socketChannel, key));
							} else {
								try {
									socketChannel.close();
								} catch (final IOException e) {
									e.printStackTrace();
								}
							}

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
								final ZResponse response = task.invoke(zRequest);
								if (response != null && !response.getWrite().get()) {
									// 在此自动write，接口中可以不调用write
									response.header(SERVER, Z_SERVER);
									if (keepAlive) {
										response.header(CONNECTION, ConnectionEnum.KEEP_ALIVE.getValue());
									}
									response.write();
								}
							}

						}
					}


				});

			}
		}
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class SS {

		private SocketChannel socketChannel;
		private SelectionKey selectionKey;

	}


}
