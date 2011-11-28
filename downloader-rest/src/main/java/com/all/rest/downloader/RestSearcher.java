package com.all.rest.downloader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.all.rest.beans.RestSearchData;
import com.all.rest.web.RestService;
import com.all.shared.mc.TrackSearchResult;

@Service
public class RestSearcher extends CommonSearcher implements ManagedSearcher {

	private static final int MAX_CONCURRENT_SEARCHES = 5;

	private final Log log = LogFactory.getLog(this.getClass());

	private final Map<String, RestSearch> currentSearches = Collections
			.synchronizedMap(new HashMap<String, RestSearch>());

	private final ExecutorService searcherExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_SEARCHES);

	@Autowired
	private RestService restService;

	@Override
	public void search(String keyword) throws SearchException {
		if (keyword == null || keyword.trim().isEmpty()) {
			throw new IllegalArgumentException("Keyword cannot be null nor empty.");
		}
		if (!currentSearches.containsKey(keyword)) {
			RestSearch search = new RestSearch(keyword);
			currentSearches.put(keyword, search);
			searcherExecutor.execute(search);
		}
	}

	private final class RestSearch implements Runnable {

		private final String keyword;

		public RestSearch(String keyword) {
			this.keyword = keyword;
		}

		@Override
		public void run() {
			try {

				notifyProgress(0);
				for (TrackSearchResult searchResult : restService.findTracksByKeyword(keyword)) {
					notifySearchData(new SearchDataEvent(RestSearcher.this, keyword, new RestSearchData(searchResult)));
				}

				notifyProgress(100);

			} catch (Exception e) {
				log.error(e);
				notifyError(new SearchErrorEvent(RestSearcher.this, keyword));
			} finally {
				currentSearches.remove(keyword);
			}
		}

		private void notifyProgress(int progress) {
			notifySearchProgress(new SearchProgressEvent(RestSearcher.this, keyword, progress));
		}

	}

}
