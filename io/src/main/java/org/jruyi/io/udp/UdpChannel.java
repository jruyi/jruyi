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
package org.jruyi.io.udp;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;

import org.jruyi.io.channel.Channel;
import org.jruyi.io.channel.IChannelService;

public class UdpChannel extends Channel {

	private DatagramChannel m_datagramChannel;
	private SocketAddress m_remoteAddress;

	public UdpChannel(IChannelService channelService) {
		super(channelService);
	}

	public UdpChannel(IChannelService channelService,
			DatagramChannel datagramChannel, SocketAddress remoteAddress) {
		super(channelService);
		m_datagramChannel = datagramChannel;
		m_remoteAddress = remoteAddress;
	}

	@Override
	public final Object localAddress() {
		return m_datagramChannel.socket().getLocalSocketAddress();
	}

	@Override
	public final Object remoteAddress() {
		return m_remoteAddress;
	}

	@Override
	protected final boolean connect() throws Exception {
		DatagramChannel datagramChannel = m_datagramChannel;
		if (datagramChannel == null)
			m_datagramChannel = datagramChannel = DatagramChannel.open();

		final DatagramSocket socket = datagramChannel.socket();

		final UdpChannelConf conf = (UdpChannelConf) channelService()
				.getConfiguration();

		if (conf.reuseAddr())
			socket.setReuseAddress(true);

		if (conf.broadcast())
			socket.setBroadcast(true);

		// IP_TOS
		Integer integer = conf.trafficClass();
		if (integer != null)
			socket.setTrafficClass(integer);

		// SO_RCVBUF
		integer = conf.recvBufSize();
		if (integer != null)
			socket.setReceiveBufferSize(integer);

		datagramChannel.configureBlocking(false);
		SocketAddress remoteAddress = m_remoteAddress;
		if (remoteAddress == null)
			m_remoteAddress = remoteAddress = new InetSocketAddress(conf.ip(),
					conf.port());

		datagramChannel.connect(remoteAddress);

		return true;
	}

	@Override
	protected final void onConnected() throws Exception {
		final DatagramChannel datagramChannel = m_datagramChannel;
		final DatagramSocket socket = datagramChannel.socket();

		final UdpChannelConf conf = (UdpChannelConf) channelService()
				.getConfiguration();

		// IP_TOS
		Integer integer = conf.trafficClass();
		if (integer != null)
			socket.setTrafficClass(integer);

		// SO_SNDBUF
		integer = conf.sendBufSize();
		if (integer != null)
			socket.setSendBufferSize(integer);
	}

	@Override
	protected final void onAccepted() throws Exception {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	protected final void onClose() throws Exception {
		final DatagramChannel datagramChannel = m_datagramChannel;
		if (datagramChannel != null) {
			datagramChannel.disconnect();
			datagramChannel.close();
		}
	}

	@Override
	protected final ScatteringByteChannel scatteringByteChannel() {
		return m_datagramChannel;
	}

	@Override
	protected final GatheringByteChannel gatheringByteChannel() {
		return m_datagramChannel;
	}

	@Override
	protected final SelectableChannel selectableChannel() {
		return m_datagramChannel;
	}
}
