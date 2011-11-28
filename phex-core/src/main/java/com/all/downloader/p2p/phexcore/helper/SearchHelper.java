package com.all.downloader.p2p.phexcore.helper;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import phex.query.DefaultSearchProgress;
import phex.query.Search;
import phex.query.SearchProgress;

import com.all.downloader.p2p.phexcore.PhexCore;
import com.all.downloader.p2p.phexcore.search.SearchInfo;
import com.all.downloader.search.SearcherListener;

/**
 * understands a search helper to simplify search code outside this class
 */
@Component
public class SearchHelper {

	protected final Set<SearcherListener> searcherSourcesListeners = new CopyOnWriteArraySet<SearcherListener>();
	@Autowired
	private PhexCore phexCore;

	public SearchInfo createKeywordSearch(String keyword) {
		Search search = phexCore.createSearchTerm(keyword);

		SearchProgress progress = new PhexSearchProgress();
		search.startSearching(progress);

		SearchInfo searchInfo = new SearchInfo(search, keyword, searcherSourcesListeners);
		phexCore.processPhexAnotation(searchInfo);

		return searchInfo;
	}

	public void addSearcherSourcesListener(SearcherListener listener) {
		searcherSourcesListeners.add(listener);
	}

	public void removeSearcherSourcesLister(SearcherListener listener) {
		searcherSourcesListeners.remove(listener);
	}

	class PhexSearchProgress extends DefaultSearchProgress {
		public PhexSearchProgress() {
			super(DEFAULT_QUERY_TIMEOUT, DESIRED_RESULTS);
		}
	}

}
