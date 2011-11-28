package com.all.download.manager.share;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.downloader.alllink.AllLink;
import com.all.downloader.share.CommonSharer;
import com.all.downloader.share.FileSharedEvent;
import com.all.downloader.share.ManagedSharer;
import com.all.downloader.share.ShareException;
import com.all.downloader.share.Sharer;
import com.all.downloader.share.SharerListener;
import com.all.messengine.MessageMethod;
import com.all.shared.messages.MessEngineConstants;

@Service
public class SharerManager extends CommonSharer implements Sharer, SharerListener {
	private static final Log log = LogFactory.getLog(SharerManager.class);
	// private static final int CONCURRENT_SHARE_THREADS = 5;
	private static final int CONCURRENT_SHARE_THREADS = Runtime.getRuntime().availableProcessors();
	private static long CURRENT_SHARE_THREADS = 0;
	@Autowired
	private Collection<ManagedSharer> sharerCollection;
	private ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_SHARE_THREADS,
			new ShareManagerThreadFactory());
	private Set<AllLink> allLinkShared = new HashSet<AllLink>();

	@PostConstruct
	@SuppressWarnings("unchecked")
	public void initialize() {
		if (!sharerCollection.isEmpty()) {

			log.info("Using a thread pool of " + CONCURRENT_SHARE_THREADS);
			log.info("Found managed sharers: " + sharerCollection);

			this.sharerCollection = Collections.unmodifiableCollection(sharerCollection);
			for (ManagedSharer managedSharer : sharerCollection) {
				managedSharer.addSharerListener(this);
			}
		} else {
			this.sharerCollection = Collections.EMPTY_LIST;
		}
	}

	@PreDestroy
	@MessageMethod(MessEngineConstants.USER_SESSION_CLOSED_TYPE)
	public void shutdown() {
		for (ManagedSharer managedSharer : sharerCollection) {
			managedSharer.removeSharerListener(this);
		}
	}

	@Override
	public void share(final AllLink currentAllLink) throws ShareException {
		if (allLinkShared.add(currentAllLink)) {
			executorService.execute(new ShareRunnable(currentAllLink));
		}
	}

	@Override
	public void onFileShared(FileSharedEvent fileSharedEvent) {
		for (SharerListener sharerListener : listeners) {
			try {
				sharerListener.onFileShared(fileSharedEvent);
			} catch (Exception e) {
				log.error("Error while propagating FileSharedEvent", e);
			}
		}
	}

	class ShareRunnable implements Runnable {
		private final Log log = LogFactory.getLog(ShareRunnable.class);
		private final AllLink currentAllLink;

		private ShareRunnable(AllLink currentAllLink) {
			this.currentAllLink = currentAllLink;
		}

		@Override
		public void run() {
			try {
				long start = System.currentTimeMillis();
				log.debug("Starting sharing process for " + currentAllLink);
				for (ManagedSharer managedSharer : sharerCollection) {
					try {
						managedSharer.share(currentAllLink);
					} catch (Exception e) {
						log.error("Unable to share allLink " + currentAllLink, e);
					}
				}
				log.debug(String.format("Finished sharing process for %s which took %d ms", currentAllLink,
						System.currentTimeMillis() - start));
			} catch (Exception e) {
				log.error("Unexpected error while sharing content", e);
			}
		}
	}

	class ShareManagerThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable);
			thread.setName("Share-Thread-" + CURRENT_SHARE_THREADS++);
			thread.setDaemon(true);
			return thread;
		}
	}

}
