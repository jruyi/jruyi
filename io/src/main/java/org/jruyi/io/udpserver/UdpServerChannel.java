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
package org.jruyi.io.udpserver;

import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.jruyi.common.StrUtil;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.ISelectableChannel;
import org.jruyi.io.channel.ISelector;
import org.jruyi.io.udp.UdpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UdpServerChannel implements ISelectableChannel, Runnable {

	private static final Logger c_logger = LoggerFactory.getLogger(UdpServerChannel.class);
	private static final Long ID = 0L;
	private final UdpServer m_udpServer;
	private final DatagramChannel m_datagramChannel;
	private final SocketAddress m_localAddr;
	private SelectionKey m_selectionKey;

	@Override
	public Long id() {
		return ID;
	}

	// runs on read
	@Override
	public void run() {
		UdpServer server = m_udpServer;
		try {
			final ByteBuffer bb = server.getChannelAdmin().recvDirectBuffer();
			SocketAddress remoteAddr = m_datagramChannel.receive(bb);

			IChannel channel = server.getChannel(remoteAddr);
			if (channel == null) {
				DatagramChannel datagramChannel = DatagramChannel.open();
				DatagramSocket socket = datagramChannel.socket();
				socket.setReuseAddress(true);
				socket.bind(m_localAddr);
				channel = new UdpChannel(server, datagramChannel, remoteAddr);
				channel.connect(-1);
			}

			bb.flip();
			channel.receive(bb);
			channel.onReadRequired();
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(server, " failed to receive message"), t);
			close();
		}
	}

	public UdpServerChannel(UdpServer udpServer, DatagramChannel datagramChannel, SocketAddress localAddr) {
		m_udpServer = udpServer;
		m_datagramChannel = datagramChannel;
		m_localAddr = localAddr;
	}

	@Override
	public void close() {
		m_udpServer.stop();
	}

	@Override
	public void onConnect() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Runnable onRead() {
		return this;
	}

	@Override
	public Runnable onWrite() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onException(Throwable t) {
		c_logger.error(StrUtil.join(m_udpServer, " got an error"), t);
		close();
	}

	@Override
	public void register(ISelector selector, int ops) {
		try {
			m_selectionKey = m_datagramChannel.register(selector.selector(), ops, this);
		} catch (Throwable t) {
			// Ignore
		}
	}

	@Override
	public void interestOps(int ops) {
		SelectionKey selectionKey = m_selectionKey;
		try {
			selectionKey.interestOps(selectionKey.interestOps() | ops);
		} catch (Throwable t) {
			// Ignore
		}
	}
}
