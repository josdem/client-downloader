package com.all.landownloader.discovery;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

//@Ignore
public class TestDiscoveryListener {
	LanDiscoverySocket discovery;

	@Before
	public void setup() throws Exception {
		discovery = new LanDiscoverySocket(5000, 11223);
	}

	@After
	public void shutdown() throws Exception {
		// discovery.close();
	}

	@Test(timeout = 20000)
	public void shouldRegisterAPeer() throws Exception {
		sendDelayedPacket();
		LanDiscoveredPeer listen = discovery.listen();
		assertEquals(1025, listen.getPort());
		Set<LanDiscoveredPeer> adresses = discovery.getAddresses();
		assertEquals(1, adresses.size());
	}

	@Ignore("Not running on hudson, fix me!")
	@Test(timeout = 20000)
	public void shouldRemoveAddressAfterLeaseTime() throws Exception {
		sendDelayedPacket();
		LanDiscoveredPeer listen = discovery.listen();
		assertEquals(1025, listen.getPort());
		Set<LanDiscoveredPeer> adresses = discovery.getAddresses();
		assertEquals(1, adresses.size());
		Thread.sleep(7000);
		adresses = discovery.getAddresses();
		assertEquals(0, adresses.size());
	}

	@Ignore("Not running on hudson, fix me!")
	@Test(timeout = 30000) 
	public void shouldKeepConnectionAliveIfSocketConnectsWithinLeaseTimeLimit() throws Exception {
		sendDelayedPacket();
		LanDiscoveredPeer listen = discovery.listen();
		assertEquals(1025, listen.getPort());
		Set<LanDiscoveredPeer> adresses = discovery.getAddresses();
		assertEquals(1, adresses.size());
		Thread.sleep(3000);
		sendDelayedPacket();
		listen = discovery.listen();
		assertEquals(1025, listen.getPort());
		Thread.sleep(3000);
		adresses = discovery.getAddresses();
		assertEquals(1, adresses.size());

	}

	private void sendDelayedPacket() {
		Thread thread = new Thread(new MockClient());
		thread.setDaemon(true);
		thread.start();
	}
}

class MockClient implements Runnable {

	@Override
	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		try {
			byte[] buf = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 1, 0, 0, 0, 0 };
			InetAddress group = InetAddress.getByName(LanDiscoverySocket.GROUP);
			DatagramPacket packet = new DatagramPacket(buf, buf.length, group, LanDiscoverySocket.PORT);
			MulticastSocket socket = new MulticastSocket();
			socket.send(packet);
		} catch (IOException e) {
		}
	}

}
