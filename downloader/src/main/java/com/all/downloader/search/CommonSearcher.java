package com.all.downloader.search;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class CommonSearcher implements Searcher {

	protected Log log = LogFactory.getLog(this.getClass());
	protected Set<SearcherListener> listeners = new CopyOnWriteArraySet<SearcherListener>();

	@Override
	public void addSearcherListener(SearcherListener listener) {
		listeners.add(listener);
	}
	
	@Override
	public void removeSearcherListener(SearcherListener listener) {
		listeners.remove(listener);
	}

	protected void notifySearchProgress(SearchProgressEvent searchProgressEvent) {
		for (SearcherListener searcherListener : listeners) {
			try {
				searcherListener.updateProgress(searchProgressEvent);
			} catch (Exception e) {
				log.error("Unexpected error in searcher listener", e);
			}
		}
	}

	protected void notifySearchData(SearchDataEvent searchDataEvent) {
		for (SearcherListener searcherListener : listeners) {
			try {
				searcherListener.updateSearchData(searchDataEvent);
			} catch (Exception e) {
				log.error("Unexpected error in searcher listener", e);
			}
		}
	}
	
	protected void notifyError(SearchErrorEvent searchErrorEvent) {
		for (SearcherListener searcherListener : listeners) {
			try {
				searcherListener.onError(searchErrorEvent);
			} catch (Exception e) {
				log.error("Unexpected error in searcher listener", e);
			}
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
