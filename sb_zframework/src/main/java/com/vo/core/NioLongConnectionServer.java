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
import java.util.Iterator;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.vo.conf.ServerConfiguration;
import com.vo.core.ZServer.Counter;
import com.vo.http.HttpStatus;
import com.votool.common.CR;

/**
 * NIO长连接server
 *
 * @author zhangzhen
 * @date 2023年7月4日
 *
 */
public class NioLongConnectionServer {

	private static final ZLog2 LOG = ZLog2.getInstance();

	private static final int BUFFER_SIZE = 1024 * 100;

	public static void startNIOServer() {

		final ServerConfiguration serverConfiguration = ZSingleton.getSingletonByClass(ServerConfiguration.class);

		LOG.trace("zNIOServer开始启动,serverPort={}",serverConfiguration.getPort());

		// 创建ServerSocketChannel
		Selector selector = null;
		ServerSocketChannel serverSocketChannel;
		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.bind(new InetSocketAddress(serverConfiguration.getPort()));

			// 创建Selector
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		LOG.trace("zNIOServer启动成功，等待连接,serverPort={}", serverConfiguration.getPort());

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
					if (Counter.allow(ZServer.Z_SERVER_QPS, serverConfiguration.getConcurrentQuantity())) {
						handleRead(key);
					} else {
						new ZResponse((SocketChannel) key.channel())
								.contentType(HeaderEnum.JSON.getType())
								.httpStatus(HttpStatus.HTTP_403.getCode())
								.body(JSON.toJSONString(CR.error("zserver-超出QPS限制,qps = " + serverConfiguration.getConcurrentQuantity())))
								.write();
					}
				}
			}
		}
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
		try {
			System.out.println("新连接： " + socketChannel.getRemoteAddress());
		} catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private static void handleRead(final SelectionKey key) {
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
				System.out.println("bytesRead == -1 | socketChannel.close();");
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return;
		}

		if (bytesRead > 0) {
			buffer.flip();
			final byte[] requestData = new byte[buffer.remaining()];
			buffer.get(requestData);

			final String request = new String(requestData, StandardCharsets.UTF_8);

			if (socketChannel.isOpen()) {

				ZServer.ZE.executeInQueue(() -> {
					final Task taskNIO = new Task(socketChannel);
					final ZRequest requestX = Task.handleRead(request);
					final ZRequest request2 = Task.parseRequest(requestX);
					taskNIO.invoke(request2);

					if (!request.toLowerCase().contains("keep-alive")) {
						try {
							System.out.println("非长连接，关闭socketChannel");
							socketChannel.close();
						} catch (final IOException e) {
							e.printStackTrace();
						}
					}
				});

			}
		}
	}

}
