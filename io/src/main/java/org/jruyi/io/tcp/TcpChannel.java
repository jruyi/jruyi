/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jruyi.io.tcp;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.jruyi.io.channel.Channel;
import org.jruyi.io.channel.IChannelService;

public final class TcpChannel extends Channel {

	private int m_readTimerState;
	private SocketChannel m_socketChannel;

	public TcpChannel(IChannelService channelService) {
		super(channelService);
	}

	public TcpChannel(IChannelService channelService, SocketChannel socketChannel) {
		super(channelService);
		m_socketChannel = socketChannel;
	}

	@Override
	public void setReadTimerState(int readTimerState) {
		m_readTimerState = readTimerState;
	}

	@Override
	public int readTimerState() {
		return m_readTimerState;
	}

	@Override
	public Object localAddress() {
		return m_socketChannel.socket().getLocalSocketAddress();
	}

	@Override
	public Object remoteAddress() {
		return m_socketChannel.socket().getRemoteSocketAddress();
	}

	@Override
	protected ReadableByteChannel readableByteChannel() {
		return m_socketChannel;
	}

	@Override
	protected WritableByteChannel writableByteChannel() {
		return m_socketChannel;
	}

	@Override
	protected SelectableChannel selectableChannel() {
		return m_socketChannel;
	}

	@Override
	protected void onAccepted() throws Exception {
		Socket socket = m_socketChannel.socket();

		TcpChannelConf conf = (TcpChannelConf) channelService().getConfiguration();

		// IP_TOS
		Integer integer = conf.trafficClass();
		if (integer != null)
			socket.setTrafficClass(integer);

		// SO_SNDBUF
		integer = conf.sendBufSize();
		if (integer != null)
			socket.setSendBufferSize(integer);

		// SO_LINGER
		integer = conf.soLinger();
		if (integer != null)
			socket.setSoLinger(true, integer);

		// SO_KEEPALIVE
		Boolean bool = conf.keepAlive();
		if (bool != null && bool)
			socket.setKeepAlive(true);

		// TCP_NODELAY
		bool = conf.tcpNoDelay();
		if (bool != null)
			socket.setTcpNoDelay(bool);

		// SO_OOBINLINE
		bool = conf.oobInline();
		if (bool != null)
			socket.setOOBInline(bool);
	}

	@Override
	protected void onConnected() throws Exception {
		SocketChannel socketChannel = m_socketChannel;
		socketChannel.finishConnect();

		Socket socket = socketChannel.socket();

		TcpChannelConf conf = (TcpChannelConf) channelService().getConfiguration();

		// SO_SNDBUF
		Integer integer = conf.sendBufSize();
		if (integer != null)
			socket.setSendBufferSize(integer);

		// SO_LINGER
		integer = conf.soLinger();
		if (integer != null)
			socket.setSoLinger(true, integer);

		// SO_KEEPALIVE
		Boolean bool = conf.keepAlive();
		if (bool != null && bool)
			socket.setKeepAlive(true);

		// TCP_NODELAY
		bool = conf.tcpNoDelay();
		if (bool != null)
			socket.setTcpNoDelay(bool);

		// SO_OOBINLINE
		bool = conf.oobInline();
		if (bool != null)
			socket.setOOBInline(bool);
	}

	@Override
	protected boolean connect() throws Exception {
		SocketChannel socketChannel = SocketChannel.open();
		m_socketChannel = socketChannel;

		Socket socket = socketChannel.socket();

		TcpChannelConf conf = (TcpChannelConf) channelService().getConfiguration();

		if (conf.reuseAddr())
			socket.setReuseAddress(true);

		Integer[] performancePreferences = conf.performancePreferences();
		if (performancePreferences != null) {
			int n = performancePreferences.length;
			int connectionTime = 0;
			int latency = 0;
			int bandWidth = 0;
			if (n > 2) {
				connectionTime = performancePreferences[0];
				latency = performancePreferences[1];
				bandWidth = performancePreferences[2];
			} else if (n > 1) {
				connectionTime = performancePreferences[0];
				latency = performancePreferences[1];
			} else if (n > 0)
				connectionTime = performancePreferences[0];

			socket.setPerformancePreferences(connectionTime, latency, bandWidth);
		}

		// IP_TOS
		Integer integer = conf.trafficClass();
		if (integer != null)
			socket.setTrafficClass(integer);

		// SO_RCVBUF
		integer = conf.recvBufSize();
		if (integer != null)
			socket.setReceiveBufferSize(integer);

		socketChannel.configureBlocking(false);
		return socketChannel.connect(new InetSocketAddress(conf.ip(), conf.port()));
	}

	@Override
	protected void onClose() throws Exception {
		SocketChannel socketChannel = m_socketChannel;
		if (socketChannel != null)
			socketChannel.close();
	}
}
