package com.all.download.manager.search;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.downloader.search.CommonSearcher;
import com.all.downloader.search.ManagedSearcher;
import com.all.downloader.search.SearchDataEvent;
import com.all.downloader.search.SearchErrorEvent;
import com.all.downloader.search.SearchException;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.Searcher;
import com.all.downloader.search.SearcherListener;
import com.all.messengine.MessEngine;
import com.all.shared.model.AllMessage;
import com.all.shared.stats.usage.UserActions;

/**
 * Understands a manager of searchers
 */
@Service
public class SearcherManager extends CommonSearcher implements Searcher, SearcherListener {

	private final Log log = LogFactory.getLog(SearcherManager.class);

	private ManagedSearchFactory managedSearchFactory = new ManagedSearchFactory();
	@Autowired
	private Collection<ManagedSearcher> searcherCollection = Collections.emptyList();
	@Autowired
	private MessEngine messEngine;

	@PostConstruct
	public void initialize() {
		if (!searcherCollection.isEmpty()) {
			this.searcherCollection = Collections.unmodifiableCollection(searcherCollection);
			log.info("Found managed searchers: " + searcherCollection);
		}
	}

	@Override
	public void search(String keyword) throws SearchException {
		managedSearchFactory.create(keyword).search();
		messEngine.send(new AllMessage<Integer>(UserActions.USER_ACTION_MESSAGE_TYPE, UserActions.Downloads.SEARCH_MEDIA));
	}

	@Override
	public void updateSearchData(SearchDataEvent updateSearchEvent) {
		notifySearchData(updateSearchEvent);
	}

	@Override
	public void updateProgress(SearchProgressEvent updateProgressEvent) {
		notifySearchProgress(updateProgressEvent);
	}

	@Override
	public void onError(SearchErrorEvent searchErrorEvent) {
		notifyError(searchErrorEvent);
	}

	class ManagedSearchFactory {
		ManagedSearch create(String keyword) {
			ManagedSearch managedSearch = new ManagedSearch(keyword);
			managedSearch.setSearcherListener(SearcherManager.this);
			managedSearch.setSearchers(searcherCollection);
			return managedSearch;
		}
	}
}
