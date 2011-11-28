package com.all.landownloader.discovery;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.all.landownloader.LanUtils;

public class LanDiscoverySocket {
	private static final Log log = LogFactory.getLog(LanDiscoverySocket.class);
	private static final int BUF_LENGTH = 16;
	private static final int HEADER_LENGTH = 8;
	private static final byte[] ANNOUNCE_MSG = { 0x00, 0x01, 0x12, 0x13, 0x12, 0x13, 0x11, 0x03 };
	private static final byte[] REPLY_MSG = { 0x20, 0x21, 0x22, 0x23, 0x22, 0x23, 0x21, 0x23 };
	private static final byte[] BYE_MSG = { 0x79, 0x79, 0x72, 0x73, 0x72, 0x73, 0x71, 0x73 };

	public static final String GROUP = "239.192.152.143";
	public static final int PORT = 6771;

	private static final String BROADCAST = "255.255.255.255";

	private final Set<LanDiscoveredPeer> addresses = new HashSet<LanDiscoveredPeer>();
	private final Set<RegisteredAddress> leases = new TreeSet<RegisteredAddress>();
	private final Map<LanDiscoveredPeer, RegisteredAddress> quickLeases = new HashMap<LanDiscoveredPeer, RegisteredAddress>();

	private final long leaseTime;

	private final InetAddress addressGroup;
	private final InetAddress broadcast;
	private int port = 1025;

	public LanDiscoverySocket(long leaseTime, int port) throws UnknownHostException, IllegalArgumentException {
		this.leaseTime = leaseTime;
		this.addressGroup = InetAddress.getByName(GROUP);
		this.broadcast = InetAddress.getByName(BROADCAST);
		setPort(port);
	}

	public final void setPort(int port) throws IllegalArgumentException {
		if (port < 1024 || port > 0xFFFF) {
			log.warn("Port out of range downloadManager is: " + port);
			throw new IllegalArgumentException("Invalid port : " + port);
		} else {
			this.port = port;
			announce();
		}
	}

	public Set<LanDiscoveredPeer> getAddresses() {
		long currentTimeMillis = System.currentTimeMillis();
		for (Iterator<RegisteredAddress> iter = leases.iterator(); iter.hasNext();) {
			RegisteredAddress registeredAddress = iter.next();
			if (registeredAddress.getLeaseTime() < currentTimeMillis) {
				iter.remove();
				addresses.remove(registeredAddress.getAddress());
				quickLeases.remove(registeredAddress.getAddress());
			} else {
				break;
			}
		}
		return addresses;
	}

	public LanDiscoveredPeer listen() throws IOException, IllegalArgumentException {
		MulticastSocket socket = new MulticastSocket(PORT);
		socket.joinGroup(addressGroup);
		byte[] buf = new byte[BUF_LENGTH];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		socket.leaveGroup(addressGroup);
		socket.close();

		InetAddress address = packet.getAddress();
		byte[] remotePortBuffer = new byte[4];
		System.arraycopy(buf, HEADER_LENGTH, remotePortBuffer, 0, remotePortBuffer.length);
		LanDiscoveredPeer discoveredPeer = new LanDiscoveredPeer(address, LanUtils.decodeInt(remotePortBuffer));

		if (eq(BYE_MSG, buf, HEADER_LENGTH)) {
			addresses.remove(address);
			RegisteredAddress remove = quickLeases.remove(address);
			if (remove != null) {
				leases.remove(remove);
			}
			return null;
		}

		RegisteredAddress reg = quickLeases.get(discoveredPeer);
		long nextLeaseTime = System.currentTimeMillis() + this.leaseTime;
		if (reg == null) {
			reg = new RegisteredAddress(nextLeaseTime, discoveredPeer);
			quickLeases.put(discoveredPeer, reg);
		} else {
			reg.setLease(nextLeaseTime);
		}
		if (eq(ANNOUNCE_MSG, buf, HEADER_LENGTH)) {
			reply(address);
		}
		leases.add(reg);
		addresses.add(discoveredPeer);
		return discoveredPeer;
	}

	private boolean eq(byte[] msg, byte[] buf, int length) {
		for (int i = 0; i < msg.length && i < length; i++) {
			if (i >= buf.length) {
				return false;
			}
			if (msg[i] != buf[i]) {
				return false;
			}
		}
		return true;
	}

	public void reply(InetAddress address) throws IllegalArgumentException {
		try {
			byte[] buf = createPacket(REPLY_MSG);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, PORT);
			DatagramSocket socket = new DatagramSocket();
			socket.send(packet);
			socket.close();
		} catch (IOException e) {
		}
	}

	public void announceNow() {
		announce();
	}

	private void announce() throws IllegalArgumentException {
		try {
			byte[] buf = createPacket(ANNOUNCE_MSG);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, addressGroup, PORT);
			MulticastSocket socket = new MulticastSocket();
			socket.send(packet);
			packet = new DatagramPacket(buf, buf.length, broadcast, PORT);
			socket.send(packet);
			socket.close();
		} catch (Exception e) {
		}

	}

	public void announceDeath() throws IllegalArgumentException {
		try {
			byte[] buf = createPacket(BYE_MSG);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, addressGroup, PORT);
			MulticastSocket socket = new MulticastSocket();
			socket.send(packet);
			packet = new DatagramPacket(buf, buf.length, broadcast, PORT);
			socket.send(packet);
			socket.close();
		} catch (IOException e) {
		}
	}

	private byte[] createPacket(byte[] byeMsg) throws IllegalArgumentException {
		byte[] buffer = new byte[BUF_LENGTH];
		System.arraycopy(byeMsg, 0, buffer, 0, byeMsg.length);
		byte[] encodeInt = LanUtils.encodeInt(port);
		System.arraycopy(encodeInt, 0, buffer, byeMsg.length, encodeInt.length);
		return buffer;
	}
}

class RegisteredAddress implements Comparable<RegisteredAddress> {
	private long leaseTime;
	private LanDiscoveredPeer address;

	public RegisteredAddress(long leaseTime, LanDiscoveredPeer address) {
		super();
		this.leaseTime = leaseTime;
		this.address = address;
	}

	public void setLease(long leaseTime) {
		this.leaseTime = leaseTime;
	}

	@Override
	public int compareTo(RegisteredAddress o) {
		return (int) (this.leaseTime - o.leaseTime);
	}

	public long getLeaseTime() {
		return leaseTime;
	}

	public LanDiscoveredPeer getAddress() {
		return address;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RegisteredAddress) {
			RegisteredAddress other = (RegisteredAddress) obj;
			return address.equals(other.address);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return address.hashCode();
	}

}
