package com.all.landownloader;

import static com.all.landownloader.LanDownloadMessageType.TRACK_REQUEST;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;

import org.bouncycastle.util.encoders.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;

import com.all.landownloader.LanNetworkingService.LanNodeFactory;
import com.all.landownloader.discovery.LanDiscoveryService;
import com.all.shared.json.JsonConverter;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestLanNetworkingService {

	@UnderTest
	private LanNetworkingService networkingService;
	@Mock
	private IServer acceptor;
	@Mock
	private LanDownloaderMessageListener listener;
	@Mock
	private LanNodeFactory nodeFactory;
	@Mock
	private LanNode lanNode;
	@Mock
	private InetAddress remoteInetAddress;
	private String remoteAddress = "192.168.1.xxr";
	@Mock
	private InetAddress localInetAddress;
	private String localAddress = "192.168.1.xxl";
	@Mock
	private INonBlockingConnection connection;
	@Mock
	private LanDiscoveryService discoveryService;

	private LanDownloaderMessage message = new LanDownloaderMessage("127.0.0.1", TRACK_REQUEST, "downloadId");
	private String encodedMessage = new String(Base64.encode(JsonConverter.toJson(message).getBytes()));

	@Before
	public void init() throws Exception {
		networkingService.start();

		verify(discoveryService).addLanDiscoveryListener(networkingService);
		when(acceptor.isOpen()).thenReturn(true);
		networkingService.addMessageListener(listener);
		when(nodeFactory.newNode(anyString())).thenReturn(lanNode);
		when(localInetAddress.getHostAddress()).thenReturn(localAddress);
		when(lanNode.getConnection()).thenReturn(connection);
		when(connection.getLocalAddress()).thenReturn(localInetAddress);
		when(connection.getRemoteAddress()).thenReturn(remoteInetAddress);
	}

	@After
	public void tearDown() throws IOException {
		networkingService.removeMessageListener(listener);
		networkingService.stop();
		verify(acceptor).close();
	}

	@Test
	public void shouldAddNode() throws Exception {
		when(lanNode.isConnected()).thenReturn(false, true);

		networkingService.onDiscoveredAddress(remoteAddress);

		verify(nodeFactory).newNode(remoteAddress);
		verify(lanNode).connect();
	}

	@Test
	public void shouldNotConnectMoreThanOnceToSameNode() throws Exception {
		when(lanNode.isConnected()).thenReturn(false, true, true, true);
		networkingService.onDiscoveredAddress(remoteAddress);
		networkingService.onDiscoveredAddress(remoteAddress);
		networkingService.onDiscoveredAddress(remoteAddress);

		verify(nodeFactory).newNode(remoteAddress);
		verify(lanNode).connect();
		verify(lanNode, times(3)).isConnected();
	}

	// @Test
	// public void shouldNotRetryConnectionToBlacklistedNodes() throws Exception {
	// when(lanNode.isConnected()).thenReturn(false);
	// when(lanNode.isBlacklisted()).thenReturn(false, false, true);
	//
	// networkingService.onDiscoveredAddress(remoteAddress);
	// networkingService.onDiscoveredAddress(remoteAddress);
	// networkingService.onDiscoveredAddress(remoteAddress);
	// networkingService.onDiscoveredAddress(remoteAddress);
	//
	// verify(nodeFactory).newNode(remoteAddress);
	// verify(lanNode, times(2)).connect();
	// }

	@Test
	public void shouldSendMessageToAParticularNode() throws Exception {
		when(lanNode.isConnected()).thenReturn(true);

		networkingService.sendTo(message, remoteAddress);

		verify(connection).write(anyString());
	}

	// @Test
	// public void shouldNotSendMessageToABlacklistedNode() throws Exception {
	// when(lanNode.isConnected()).thenReturn(false);
	// networkingService.onDiscoveredAddress(remoteAddress);
	// verify(lanNode).connect();
	//
	// when(lanNode.isBlacklisted()).thenReturn(true);
	//
	// networkingService.sendTo(message, remoteAddress);
	//
	// verify(nodeFactory).newNode(remoteAddress);
	// verify(connection, never()).write(anyString());
	// }

	@Test
	public void shouldSendMessageToAllKnownNodes() throws Exception {
		networkingService.onDiscoveredAddress(remoteAddress);
		String otherAddress = "192.168.1.xxo";
		networkingService.onDiscoveredAddress(otherAddress);
		when(lanNode.isConnected()).thenReturn(true);

		networkingService.send(message);

		verify(connection, times(2)).write(anyString());
	}

	@Test
	public void shouldCallListenersAsynchronouslyOnMessageReceived() throws Exception {
		when(connection.readStringByDelimiter(anyString())).thenReturn(encodedMessage);

		networkingService.onData(connection);

		verify(listener, timeout(2000)).onMessage(any(LanDownloaderMessage.class));
	}

	@Test
	public void shouldKnowWhenNewNodesConnect() throws Exception {

		assertTrue(networkingService.onConnect(connection));

		verify(connection).getRemoteAddress();
	}

}
