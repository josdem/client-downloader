package com.all.landownloader.discovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class LanDiscoveredPeer {
	private InetAddress address;
	private InetSocketAddress inetSocketAddress;
	private int port;

	public LanDiscoveredPeer(InetAddress address, int port) throws IllegalArgumentException {
		if (port < 1024 || port > 0xFFFF) {
			throw new IllegalArgumentException("Invalid port: " + port);
		}
		this.address = address;
		this.port = port;
		inetSocketAddress = new InetSocketAddress(address, port);
	}

	public InetAddress getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null){
			return false;
		}
		if (getClass() != obj.getClass()){
			return false;
		}
		LanDiscoveredPeer other = (LanDiscoveredPeer) obj;
		if (address == null) {
			if (other.address != null){
				return false;
			}
		} else if (!address.equals(other.address)){
			return false;
		}
		if (port != other.port){
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return address + ":" + port;
	}

	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}
}
