package com.vo.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.votool.ze.ZE;
import com.votool.ze.ZES;

/**
 *
 *
 * @author zhangzhen
 * @date 2023年7月3日
 *
 */
public class NonBlockingHttpServer {

	private static final int PORT = 8080;
	private static final int BUFFER_SIZE = 1024 * 50;
	private static final int THREAD_POOL_SIZE = 9;

	private Selector selector;
	private ExecutorService threadPool;
	ZE ze = ZES.newZE(140);

	public static void main(final String[] args) {
		final NonBlockingHttpServer server = new NonBlockingHttpServer();
		server.start();
	}

	public void start() {
		try {
			this.selector = Selector.open();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

		ServerSocketChannel serverSocketChannel = null;
		try {
			serverSocketChannel = ServerSocketChannel.open();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocketChannel.bind(new InetSocketAddress(PORT));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocketChannel.configureBlocking(false);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		try {
			serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
		} catch (final ClosedChannelException e) {
			e.printStackTrace();
		}

		System.out.println("Server started on port " + PORT);

		while (true) {
			try {
				this.selector.select();
			} catch (final IOException e) {
				e.printStackTrace();
			}

			final Iterator<SelectionKey> keyIterator = this.selector.selectedKeys().iterator();
			while (keyIterator.hasNext()) {
				final SelectionKey key = keyIterator.next();
				keyIterator.remove();
				final boolean valid = key.isValid();

				if (!valid) {
					System.out.println("key is valid");
					continue;
				}

				if (key.isAcceptable()) {
					try {
						this.handleAccept(key);
					} catch (final IOException e) {
						e.printStackTrace();
					}
				} else if (key.isReadable()) {
					this.handleRead(key);
				}
			}
		}
	}

	private void handleAccept(final SelectionKey key) throws IOException {
		final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		final SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		socketChannel.register(this.selector, SelectionKey.OP_READ);
	}

	private  void handleRead(final SelectionKey key) {
		final SocketChannel socketChannel = (SocketChannel) key.channel();
		final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		final boolean open = socketChannel.isOpen();
		if (!open) {
			return;
		}
		int bytesRead = 0;
		try {
			bytesRead = socketChannel.read(buffer);
		} catch (final IOException e) {
//			e.printStackTrace();
			return;
		}

		if (bytesRead == -1) {
			try {
				key.cancel();
				socketChannel.close();
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
//			System.out.println("Received request:\n" + request);

			key.cancel();
			if (socketChannel.isOpen()) {
//				this.handleRequest(socketChannel, request, key);
				this.ze.executeInQueue(() -> this.handleRequest(socketChannel, request, key));
//				this.threadPool.submit(() -> this.handleRequest(socketChannel, request, key));
			}
		}
	}

	private void handleRequest(final SocketChannel socketChannel, final String request, final SelectionKey key) {
		System.out.println(java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
				+ "NonBlockingHttpServer.handleRequest()");
		try {
			Thread.sleep(1);
		} catch (final InterruptedException e1) {
			e1.printStackTrace();
		}

		// 在这里处理请求并生成响应
		final String response =
					  "HTTP/1.1 200 OK\r\n"
					+ "Content-Type: text/plain\r\n"
					+ "Content-Length: 12\r\n"
					+ "\r\n"
					+ "Hello World!";

		try {
			final ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
			socketChannel.write(buffer);
			socketChannel.close();
		} catch (final IOException e) {
//	            e.printStackTrace();
			return;
		}
	}

//	@SuppressWarnings("resource")
//	public static void main(final String[] args) throws IOException {
//		// 创建选择器
//		final Selector selector = Selector.open();
//
//		// 创建服务器套接字通道
//		final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
//		serverSocketChannel.bind(new InetSocketAddress(8080));
//		serverSocketChannel.configureBlocking(false);
//
//		// 将服务器套接字通道注册到选择器上，并监听接收事件
//		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
//
//		System.out.println("Server started on port 8080");
//
//		final ZE ze = ZES.newZE();
//
//		while (true) {
//			// 阻塞等待就绪的通道
//			selector.select();
//
//			// 获取选择器上所有已就绪的事件
//			final Set<SelectionKey> selectedKeys = selector.selectedKeys();
//			final Iterator<SelectionKey> iterator = selectedKeys.iterator();
//
//			while (iterator.hasNext()) {
//				final SelectionKey key = iterator.next();
//
//				if (key.isAcceptable()) {
//					// 接收新的连接
//					final ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
//					final SocketChannel clientChannel = serverChannel.accept();
//					clientChannel.configureBlocking(false);
//					clientChannel.register(selector, SelectionKey.OP_READ);
//				} else if (key.isReadable()) {
//					// 读取客户端请求
////					ze.executeInQueue(() -> {
//
//						final SocketChannel clientChannel = (SocketChannel) key.channel();
//						final int b = 1 * 1024  * 1024 * 1 ;
//						final ByteBuffer buffer = ByteBuffer.allocate(b);
//						try {
//							clientChannel.read(buffer);
//						} catch (final IOException e1) {
//							e1.printStackTrace();
//						}
//						buffer.flip();
//						final String request = new String(buffer.array()).trim();
//						System.out.println(
//								java.time.LocalDateTime.now() + "\t" + Thread.currentThread().getName() + "\t"
//										+ "\n"
//										+
//										"Received request: " + request);
//
//						// 处理请求并返回响应
//						final String response = "HTTP/1.1 200 OK\r\nContent-Length: 12\r\n\r\nHello World!";
//						final ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
//						try {
//							clientChannel.write(responseBuffer);
//							clientChannel.close();
//						} catch (final IOException e2) {
//							e2.printStackTrace();
//						}
////					});
//				}
//
//				iterator.remove();
//			}
//		}
//	}
}
