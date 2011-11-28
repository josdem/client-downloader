package com.all.landownloader.discovery;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

@Service
public class LanDiscoveryService {

	public static final long MESSAGE_SENT_WINDOW = 1 * 30 * 1000;

	private static final long DELAY = 1;

	private static final int THREAD_POOL_SIZE = 1;

	private final Log log = LogFactory.getLog(this.getClass());

	private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(THREAD_POOL_SIZE);

	private final Announcer announcer = new Announcer();

	private final Thread thread = new SocketListenerThread();

	private final Set<LanDiscoveryListener> listeners = new HashSet<LanDiscoveryListener>();

	private LanDiscoverySocket socketDiscovery;

	private ScheduledFuture<?> scheduleFuture;

	private ExecutorService adderExecutor = Executors.newCachedThreadPool();

	@PostConstruct
	public boolean startListening() throws UnknownHostException {
		thread.start();
		scheduleFuture = scheduler.schedule(announcer, DELAY, TimeUnit.MINUTES);
		return true;
	}

	@PreDestroy
	public void die() {
		thread.interrupt();
		scheduleFuture.cancel(true);
		scheduler.shutdownNow();
		socketDiscovery.announceDeath();
	}

	public void addLanDiscoveryListener(LanDiscoveryListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	public void removeLanDiscoveryListener(LanDiscoveryListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	class Announcer implements Runnable {
		private long previousSentMessage;

		@Override
		public void run() {
			if (isAbleToRunNow()) {
				try {
					socketDiscovery.announceNow();
				} catch (Exception e) {
					log.error(e, e);
				} finally {
					scheduleFuture = scheduler.schedule(announcer, DELAY, TimeUnit.MINUTES);
					previousSentMessage = System.currentTimeMillis();
				}
			}
		}

		boolean isAbleToRunNow() {
			return System.currentTimeMillis() - previousSentMessage > MESSAGE_SENT_WINDOW;
		}
	}

	class SocketListenerThread extends Thread {

		public SocketListenerThread() {
			this.setDaemon(true);
			this.setName(this.getClass().getSimpleName());
		}

		@Override
		public void run() {
			try {
				while (socketDiscovery == null) {
					// TODO: CHECK PORT SINCE IT WAS ORIGINALLY TAKEN FROM DOWNLOADER
					int port = 1025;
					socketDiscovery = new LanDiscoverySocket(15L * 1000L, port);
					while (thread == Thread.currentThread()) {
						socketDiscovery.listen();
						final ArrayList<String> addresses = new ArrayList<String>();
						for (LanDiscoveredPeer peer : socketDiscovery.getAddresses()) {
							addresses.add(peer.getAddress().getHostAddress());
						}
						adderExecutor.execute(new Runnable() {
							@Override
							public void run() {
								try {
									for (String address : addresses) {
										synchronized (listeners) {
											for (LanDiscoveryListener listener : listeners) {
												listener.onDiscoveredAddress(address);
											}
										}
									}
								} catch (Exception e) {
									log.error(e, e);
								}
							}
						});
					}
				}
			} catch (Exception e) {
				log.error(e, e);
			}
		}
	}

}
