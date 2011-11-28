package com.all.landownloader.discovery;

import static org.junit.Assert.assertTrue;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.junit.Test;

public class TestDiscoveryAnnounce {

	@Test(timeout = 2000)
	public void shouldAnnounceThroughUDPSocket() throws Exception {
		final Object lock = new Object();
		final BooleanStore val = new BooleanStore();

		Thread t = new Thread(new ServerMock(val, lock));
		t.setDaemon(true);
		t.start();
		synchronized (lock) {
			lock.wait();
		}

		LanDiscoverySocket announcer = new LanDiscoverySocket(1000, 10280);
		announcer.announceNow();
		t.join();
		assertTrue(val.value);
	}
}

class ServerMock implements Runnable {
	private final BooleanStore val;
	private final Object lock;

	public ServerMock(BooleanStore val, Object lock) {
		this.val = val;
		this.lock = lock;
	}

	public void run() {
		try {
			MulticastSocket socket = new MulticastSocket(LanDiscoverySocket.PORT);
			InetAddress address = InetAddress.getByName(LanDiscoverySocket.GROUP);
			socket.joinGroup(address);

			byte[] buf = new byte[32];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			synchronized (lock) {
				lock.notifyAll();
			}
			socket.receive(packet);

			socket.leaveGroup(address);
			socket.close();
			val.value = true;
		} catch (Exception e) {
		}
	}
}

class BooleanStore {
	public boolean value;
}