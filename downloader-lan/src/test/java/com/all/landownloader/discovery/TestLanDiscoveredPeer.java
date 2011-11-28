package com.all.landownloader.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.Test;


public class TestLanDiscoveredPeer {
	
	@Test
	public void shouldGetInetSocketAddress() throws Exception {
		int port = 6881;
		InetAddress address = InetAddress.getLocalHost();
		LanDiscoveredPeer discoveredPeer = new LanDiscoveredPeer(address , port);
		InetSocketAddress expected = new InetSocketAddress(address, port);
		
		InetSocketAddress result = discoveredPeer.getInetSocketAddress();

		assertNotNull(result);
		assertEquals(expected, result);
	}

	@Test(expected=IllegalArgumentException.class)
	public void shouldFailIfIllegalPort() throws Exception {
		new LanDiscoveredPeer(InetAddress.getLocalHost(), 1000);
	}
	
	@Test
	public void shouldKnowDiscoveredPeersAreEqual() throws Exception {
		LanDiscoveredPeer peerA = new LanDiscoveredPeer(InetAddress.getLocalHost(), 1025);
		LanDiscoveredPeer peerB = new LanDiscoveredPeer(InetAddress.getLocalHost(), 1025);
		LanDiscoveredPeer peerC = new LanDiscoveredPeer(InetAddress.getLocalHost(), 1026);
		LanDiscoveredPeer peerD = new LanDiscoveredPeer(null, 1025);
		
		assertEquals(peerA, peerB);
		assertEquals(peerA, peerA);
		assertEquals(peerB, peerA);
		assertEquals(peerA.hashCode(), peerB.hashCode());
		assertFalse(peerA.equals(peerC));
		assertFalse(peerA.equals(peerD));
		assertFalse(peerD.equals(peerA));
		assertFalse(peerA.equals(new Object()));
		assertFalse(peerA.equals(null));
	}
	
	@Test
	public void shouldOverrideToString() throws Exception {
		LanDiscoveredPeer peer = new LanDiscoveredPeer(InetAddress.getLocalHost(), 1025);
		assertTrue(peer.toString().contains("1025"));
		assertTrue(peer.toString().contains(InetAddress.getLocalHost().getHostAddress()));
	}
	
}
