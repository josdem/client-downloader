package com.all.landownloader;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnection;

public class LanNode {
	
	private static final Log log = LogFactory.getLog(LanNode.class);

//	private static final int MAX_CONNECTION_ATTEMPTS = 10;

	private static final int CONNECTION_TIMEOUT = 60 * 60 * 1000;

	private final String address;
	
	private final AtomicInteger failures = new AtomicInteger(0);

	private INonBlockingConnection connection;

	public LanNode(String address) {
		this.address = address;
	}

//	public boolean isBlacklisted() {
//		return failures.get() > MAX_CONNECTION_ATTEMPTS;
//	}

	public INonBlockingConnection getConnection() {
		return isConnected() ? connection : null;
	}

	public void connect() {
		try {
			log.info("Connecting to " + address);
			connection = new NonBlockingConnection(address, LanNetworkingService.LISTENING_PORT);
			connection.setConnectionTimeoutMillis(LanNode.CONNECTION_TIMEOUT);
			failures.set(0);
			log.info("New LAN node connection to " +address);
		} catch (Exception e) {
			failures.incrementAndGet();
			log.error("Could not connect to " + address, e);
		}
	}
	
	public boolean isConnected() {
		return connection != null && connection.isOpen();
	}

}
