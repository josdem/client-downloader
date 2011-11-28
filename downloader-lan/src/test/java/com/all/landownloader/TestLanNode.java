package com.all.landownloader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;


public class TestLanNode {

	private String address = null;
	private LanNode node = new LanNode(address);

	private IServer server;
	
	private ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
		
	public void startServer() throws UnknownHostException, IOException{
		server = new Server(LanNetworkingService.LISTENING_PORT, new IDataHandler(){

			@Override
			public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException,
					ClosedChannelException, MaxReadSizeExceededException {
				return true;
			}});
		serverExecutor.execute(server);
	}
	
	@After
	public void tearDown() throws IOException{
		if(server !=  null && server.isOpen()) {
			server.close();
			serverExecutor.shutdownNow();
		}
	}
	
	@Test
	public void shouldKnowIfNodeIsConnected() throws Exception {
		assertFalse(node.isConnected());
		assertNull(node.getConnection());
		startServer();
		
		node.connect();
		
		assertTrue(node.isConnected());
		assertNotNull(node.getConnection());
	}
	
//	@Test
//	public void shouldMarkSomeNodeAsBlacklistedAfterSomeUnsuccesfulAttempts() throws Exception {
//		assertFalse(node.isConnected());
//		assertNull(node.getConnection());
//
//		while(!node.isBlacklisted()){
//			node.connect();
//		}
//
//		assertTrue(node.isBlacklisted());
//	}
	
}
