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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.jruyi.common.StrUtil;
import org.jruyi.io.AbstractCodec;
import org.jruyi.io.IBuffer;
import org.jruyi.io.IUnit;
import org.jruyi.io.IUnitChain;
import org.jruyi.io.buffer.Util;
import org.jruyi.io.channel.IChannel;
import org.jruyi.io.channel.IIoWorker;
import org.jruyi.io.channel.ISelectableChannel;
import org.jruyi.io.channel.ISelector;
import org.jruyi.io.udp.UdpChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UdpServerChannel extends AbstractCodec<SocketAddress> implements ISelectableChannel, Runnable {

	private static final Logger c_logger = LoggerFactory.getLogger(UdpServerChannel.class);

	// Preserved (-100, 0] for UdpServerChannel
	private static long s_sequence = -100L;

	private final Long m_id;
	private final UdpServer<Object, Object> m_udpServer;
	private final DatagramChannel m_datagramChannel;
	private final SocketAddress m_localAddr;
	private SelectionKey m_selectionKey;
	private final IIoWorker m_ioWorker;

	public UdpServerChannel(UdpServer<Object, Object> udpServer, DatagramChannel datagramChannel,
			SocketAddress localAddr) {
		m_id = ++s_sequence;
		m_udpServer = udpServer;
		m_datagramChannel = datagramChannel;
		m_localAddr = localAddr;
		m_ioWorker = udpServer.getChannelAdmin().designateIoWorker(this);
	}

	@Override
	public Long id() {
		return m_id;
	}

	@Override
	public SocketAddress read(IUnitChain unitChain) {
		try {
			final IUnit unit = Util.lastUnit(unitChain);
			final ByteBuffer bb = unit.getByteBufferForWrite();
			final int n = bb.position();
			final SocketAddress address = m_datagramChannel.receive(bb);
			unit.size(unit.size() + bb.position() - n);
			return address;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	// runs on read
	@Override
	public void run() {
		final UdpServer<Object, Object> server = m_udpServer;
		try {
			final IBuffer in = server.getBufferFactory().create();
			final SocketAddress remoteAddr = in.read(this);

			IChannel channel = server.getChannel(remoteAddr);
			if (channel == null) {
				DatagramChannel datagramChannel = DatagramChannel.open();
				DatagramSocket socket = datagramChannel.socket();
				socket.setReuseAddress(true);
				socket.bind(m_localAddr);
				channel = new UdpChannel(server, datagramChannel, remoteAddr);
				channel.connect(-1);
			}

			channel.receive(in);
		} catch (Throwable t) {
			c_logger.error(StrUtil.join(server, " failed to receive message"), t);
			close();
		}
	}

	@Override
	public void close() {
		m_udpServer.stop();
	}

	@Override
	public void onConnect() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onAccept() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onRead() {
		m_ioWorker.execute(this);
	}

	@Override
	public void onWrite() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onException(Throwable t) {
		c_logger.error(StrUtil.join(m_udpServer, " got an error"), t);
		close();
	}

	@Override
	public void register(ISelector selector, int ops) throws Throwable {
		m_selectionKey = m_datagramChannel.register(selector.selector(), ops, this);
	}

	@Override
	public void interestOps(int ops) {
		final SelectionKey selectionKey = m_selectionKey;
		selectionKey.interestOps(selectionKey.interestOps() | ops);
	}
}
