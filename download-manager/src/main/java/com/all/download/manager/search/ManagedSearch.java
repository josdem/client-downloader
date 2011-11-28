package com.all.download.manager.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.all.commons.IncrementalNamedThreadFactory;
import com.all.downloader.search.ManagedSearcher;
import com.all.downloader.search.SearchDataEvent;
import com.all.downloader.search.SearchErrorEvent;
import com.all.downloader.search.SearchException;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.SearcherListener;

public class ManagedSearch implements SearcherListener {

	private final static Log log = LogFactory.getLog(SearcherManager.class);

	private final static IncrementalNamedThreadFactory threadFactory = new IncrementalNamedThreadFactory(
			"ManagedSearch");

	private SearcherListener searcherListener;

	private Collection<ManagedSearcher> searcherCollection;

	private Collection<ManagedSearcher> workingSearchers = new ArrayList<ManagedSearcher>();

	private String keyword;

	private ScheduledExecutorService scheduledExecutorService = Executors
			.newSingleThreadScheduledExecutor(threadFactory);

	private ScheduledFuture<?> updateTaskFuture;

	@Deprecated
	public ManagedSearch() {
	}

	public ManagedSearch(String keyword) {
		this.keyword = keyword;
	}

	public void setSearchers(Collection<ManagedSearcher> searcherCollection) {
		this.searcherCollection = searcherCollection;
	}

	public void setSearcherListener(SearcherListener searcherListener) {
		this.searcherListener = searcherListener;
	}

	public void search() throws SearchException {
		for (ManagedSearcher searcher : searcherCollection) {
			try {
				searcher.search(keyword);
				searcher.addSearcherListener(this);
				workingSearchers.add(searcher);
			} catch (Exception e) {
				log.error(String.format("Unable to perform search data, searcher[%s]", searcher), e);
			}
		}

		if (workingSearchers.isEmpty()) {
			notifyError();
			throw new SearchException("Searchers are unable to perform search now.");
		} else {
			updateTaskFuture = scheduledExecutorService.schedule(new UpdateProgressTask(), 1, TimeUnit.SECONDS);
		}
	}

	private void notifyError() {
		log.warn(String.format("Search failed for keyword[%s]", keyword));
		searcherListener.onError(new SearchErrorEvent(searcherListener, keyword));
	}

	@Override
	public void updateSearchData(SearchDataEvent searchDataEvent) {
		searcherListener.updateSearchData(searchDataEvent);
	}

	@Override
	public void updateProgress(SearchProgressEvent updateProgressEvent) {
		// omit these events as we'll be generating them here.
	}

	@Override
	public void onError(SearchErrorEvent searchErrorEvent) {
		Object source = searchErrorEvent.getSource();
		if (source instanceof ManagedSearcher) {
			ManagedSearcher searcher = (ManagedSearcher) source;

			searcher.removeSearcherListener(this);

			if (workingSearchers.remove(searcher)) {
				if (workingSearchers.isEmpty()) {
					stopUpdateProgressTask();
					notifyError();
				}
			} else {
				log.warn(String.format("Searcher[%s] for keyword[%s] not found in working searchers[%s] ", searcher,
						keyword, workingSearchers.toString()));
			}
		}
	}

	private void stopUpdateProgressTask() {
		updateTaskFuture.cancel(true);
		scheduledExecutorService.shutdownNow();

		for (ManagedSearcher managedSearcher : workingSearchers) {
			managedSearcher.removeSearcherListener(this);
		}
	}

	class UpdateProgressTask implements Runnable {
		private static final int TOTAL_SEARCH_PROGRESS_TIME = 100;
		private int progress = 0;

		@Override
		public void run() {
			searcherListener.updateProgress(new SearchProgressEvent(searcherListener, keyword, progress++));

			if (progress <= TOTAL_SEARCH_PROGRESS_TIME) {
				updateTaskFuture = scheduledExecutorService.schedule(this, 1, TimeUnit.SECONDS);
			} else {
				log.debug("Stopping search update task becasue reach it's time limit");
				stopUpdateProgressTask();
			}
		} 
	}

}
