package com.all.downloader.p2p.phexcore.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import phex.download.RemoteFile;

import com.all.downloader.alllink.AllLink;
import com.all.downloader.p2p.phexcore.PhexCoreImpl;
import com.all.downloader.p2p.phexcore.helper.SearchHelper;
import com.all.downloader.search.CommonSearcher;
import com.all.downloader.search.ManagedSearcher;
import com.all.downloader.search.SearchErrorEvent;
import com.all.downloader.search.SearchException;
import com.all.downloader.search.SearcherListener;

@Service
public class PhexSearcher extends CommonSearcher implements ManagedSearcher {
	public static final int THREAD_POOL_SIZE = 1;
	public static final long INITIAL_DELAY = 1;
	public static final long DELAY = 1;
	private final Log log = LogFactory.getLog(PhexSearcher.class);

	@Autowired
	private PhexCoreImpl phexCore;
	@Autowired
	private SearchHelper searchHelper;
	@Autowired
	private ScheduledExecutorService scheduledExecutorService;

	private ReadWriteLock lock = new ReentrantReadWriteLock();
	protected Map<String, SearchInfo> currentSearches = new HashMap<String, SearchInfo>();
	protected SearchInfo keptRemoteFilesSearchInfo;

	@PostConstruct
	public void initialize() {
		keptRemoteFilesSearchInfo = new SearchInfo() {
			@Override
			public void notifyProgress() {
			}

			@Override
			public void stopSearch() {
			};
		};
		// we use the strangest keyword possible so it's not overriden
		currentSearches.put(" @ # $ % ^ & * ! ", keptRemoteFilesSearchInfo);
		manageDownloadMonitor();
	}

	public void addSearcherListener(SearcherListener listener) {
		lock.writeLock().lock();
		try {

			searchHelper.addSearcherSourcesListener(listener);

		} finally {
			lock.writeLock().unlock();
		}
	}

	public void keepSearchData(String allLinkAsString) {
		log.info("keeping allLink from search: " + allLinkAsString);
		AllLink allLink = AllLink.parse(allLinkAsString);
		for (RemoteFile remoteFile : getAllSearchResults()) {
			String urnsha = remoteFile.getURN().getAsString();
			if (urnsha.contains(allLink.getUrnSha())) {
				keptRemoteFilesSearchInfo.addRemoteFile(remoteFile);
				log
						.debug("Remote File Added: " + remoteFile.getDisplayName() + " urnsha: "
								+ remoteFile.getURN().getAsString());
			}
		}
	}

	public void clearSearchData(String keyword) {
		lock.writeLock().lock();
		try {

			SearchInfo searchInfo = currentSearches.remove(keyword);
			if (searchInfo != null) {
				searchInfo.stopSearch();
			}

		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void search(String keyword) throws SearchException {
		lock.writeLock().lock();
		try {
			if (!phexCore.isConnected()) {
				throw new SearchException("You're not connected to any ultrapeer now");
			}

			SearchInfo searchInfo = searchHelper.createKeywordSearch(keyword);

			currentSearches.put(searchInfo.getKeyword(), searchInfo);
			
		} catch(SearchException se) {
			notifyError(new SearchErrorEvent(this, keyword));
			throw se;
		} catch(Exception e) {
			log.error(e);
			notifyError(new SearchErrorEvent(this, keyword));
		} finally {
			lock.writeLock().unlock();
		}
	}

	public List<RemoteFile> getAllSearchResults() {
		List<RemoteFile> allSearchResults = new ArrayList<RemoteFile>();
		log.error("currentSearches.values() " + currentSearches.values());
		if (currentSearches.values().isEmpty()) {
			log.info("Could not find the peer(s) for");
			return allSearchResults;
		}
		for (SearchInfo searchInfo : currentSearches.values()) {
			allSearchResults.addAll(searchInfo.getRemoteFileList());
		}
		return allSearchResults;
	}

	void manageDownloadMonitor() {
		scheduledExecutorService.scheduleWithFixedDelay(createDownloadMonitor(), INITIAL_DELAY, DELAY, TimeUnit.SECONDS);
	}

	DownloadMonitor createDownloadMonitor() {
		return new DownloadMonitor();
	}

	void updateSearchProgress() {
		for (SearchInfo searchInfo : currentSearches.values()) {
			searchInfo.notifyProgress();
		}
	}

	class DownloadMonitor implements Runnable {
		@Override
		public void run() {
			lock.readLock().lock();
			try {
				updateSearchProgress();
			} catch (Exception e) {
				log.error("Unexpected exception in downloadsMonitor.", e);
			} finally {
				lock.readLock().unlock();
			}
		}
	}

}
