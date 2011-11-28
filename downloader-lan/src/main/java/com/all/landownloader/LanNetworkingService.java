package com.all.landownloader;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;

import com.all.landownloader.discovery.LanDiscoveryListener;
import com.all.landownloader.discovery.LanDiscoveryService;
import com.all.shared.json.JsonConverter;

@Service
public class LanNetworkingService implements IDataHandler, IConnectHandler, LanDiscoveryListener {

	private static final String MESSAGE_DELIMITER = "EndOfMessage";

	public static final int LISTENING_PORT = 10011;

	private final Log log = LogFactory.getLog(this.getClass());

	private final BlockingQueue<LanDownloaderMessage> incomingQueue = new LinkedBlockingQueue<LanDownloaderMessage>();

	private final ExecutorService incomingMessagesExecutor = Executors.newSingleThreadExecutor();

	private final LanNodeFactory nodeFactory = new LanNodeFactory();

	private Map<String, LanNode> currentNodes = new HashMap<String, LanNode>();

	private Set<LanDownloaderMessageListener> listeners = new HashSet<LanDownloaderMessageListener>();

	private IServer acceptor;

	private ExecutorService acceptorExecutor = Executors.newSingleThreadExecutor();

	private AtomicBoolean started = new AtomicBoolean(false);
	@Autowired
	private LanDiscoveryService discoveryService;

	@PostConstruct
	public void start() {
		try {
			if (acceptor == null) {
				acceptor = new Server(LISTENING_PORT, this);
				acceptorExecutor.execute(acceptor);
			}
			incomingMessagesExecutor.execute(new DispatchLanDownloaderMessagesTask());
			discoveryService.addLanDiscoveryListener(this);
			started.set(true);
			log.info("Lan downloader is listening on port " + LISTENING_PORT);
		} catch (Exception e) {
			log.error("Could not start LAN downloader succesfully.", e);
		}
	}

	@PreDestroy
	public void stop() {
		if (acceptor.isOpen()) {
			log.info("Disposing LAN Downloader acceptor...");
			try {
				acceptor.close();
				acceptorExecutor.shutdown();
			} catch (IOException e) {
				log.error("Could not close XSocket server.", e);
			}
		}
	}

	public void addMessageListener(LanDownloaderMessageListener listener) {
		listeners.add(listener);
	}

	public void removeMessageListener(LanDownloaderMessageListener listener) {
		listeners.remove(listener);
	}

	private LanNode getNode(String address, boolean retry) {
		LanNode lanNode = currentNodes.get(address);
		boolean firstTry = false;
		if (lanNode == null) {
			lanNode = nodeFactory.newNode(address);
			currentNodes.put(address, lanNode);
			firstTry = true;
		}
		if (!lanNode.isConnected()) {
			// if (lanNode.isBlacklisted()) {
			// log.error("Ignoring " + address + " because it is blacklisted.");
			// } else
			if (firstTry || retry) {
				log.info("Connecting to " + address);
				lanNode.connect();
			}
		}
		return lanNode;
	}

	private INonBlockingConnection getConnection(String destinationAddress) {
		LanNode node = getNode(destinationAddress, false);
		return node != null ? node.getConnection() : null;
	}

	public int send(LanDownloaderMessage message) {
		int succesfulReceivers = 0;
		Collection<String> receivers = new ArrayList<String>();
		synchronized (currentNodes) {
			receivers.addAll(currentNodes.keySet());
		}
		for (String receiver : receivers) {
			if (sendTo(message, getConnection(receiver))) {
				succesfulReceivers++;
			}
		}
		return succesfulReceivers;
	}

	public boolean sendTo(LanDownloaderMessage message, String receiver) {
		log.debug("Sending " + message.getType() + " to " + receiver);
		boolean sendTo = sendTo(message, getConnection(receiver));
		if (!sendTo) {
			log.error("Could not send message " + message.getType() + " to " + receiver);
		}
		return sendTo;
	}

	private boolean sendTo(LanDownloaderMessage message, INonBlockingConnection connection) {
		if (connection != null) {
			message.setSourceAddress(connection.getLocalAddress().getHostAddress());
			byte[] bytes = Base64.encode(JsonConverter.toJson(message).getBytes());
			StringBuilder sb = new StringBuilder();
			sb.append(new String(bytes));
			sb.append(MESSAGE_DELIMITER);
			synchronized (connection) {
				try {
					connection.write(sb.toString());
					return true;
				} catch (IOException e) {
					log.error(e, e);
				}
			}
		}
		return false;
	}

	class DispatchLanDownloaderMessagesTask implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					LanDownloaderMessage message = incomingQueue.take();
					for (LanDownloaderMessageListener listener : listeners) {
						try {
							listener.onMessage(message);
						} catch (Exception e) {
							log.error("Unexpected error in listener " + listener.getClass().getSimpleName());
						}
					}
				} catch (Exception e) {
					log.error(e, e);
				}
			}
		}
	}

	@Override
	public void onDiscoveredAddress(String address) {
		if (started.get()) {
			if (!LanUtils.getLocalAddress().equals(address)) {
				getNode(address, true);
			}
		}
	}

	@Override
	public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException,
			ClosedChannelException, MaxReadSizeExceededException {
		String message = connection.readStringByDelimiter(MESSAGE_DELIMITER);
		String json = new String(Base64.decode(message.getBytes()));
		LanDownloaderMessage downloaderMessage = JsonConverter.toBean(json, LanDownloaderMessage.class);
		downloaderMessage.setSourceAddress(connection.getRemoteAddress().getHostAddress());
		incomingQueue.add(downloaderMessage);
		return true;
	}

	@Override
	public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException,
			MaxReadSizeExceededException {
		log.info("New LAN connection to " + connection.getRemoteAddress());
		return true;
	}

	class LanNodeFactory {
		LanNode newNode(String address) {
			return new LanNode(address);
		}
	}
}
