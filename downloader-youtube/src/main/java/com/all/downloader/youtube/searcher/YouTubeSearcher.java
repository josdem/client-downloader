package com.all.downloader.youtube.searcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.all.commons.IncrementalNamedThreadFactory;
import com.all.downloader.search.CommonSearcher;
import com.all.downloader.search.ManagedSearcher;
import com.all.downloader.search.SearchDataEvent;
import com.all.downloader.search.SearchErrorEvent;
import com.all.downloader.search.SearchException;
import com.all.downloader.search.SearchProgressEvent;
import com.google.gdata.client.youtube.YouTubeQuery;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.util.ServiceException;

@Service
public class YouTubeSearcher extends CommonSearcher implements ManagedSearcher {

	private static final Log LOG = LogFactory.getLog(YouTubeSearcher.class);

	private YouTubeService service = new YouTubeService("ALL.com");

	private YouTubeQuery query;

	private ExecutorService executor = Executors.newCachedThreadPool(new IncrementalNamedThreadFactory(
			"YouTubeSearcherTask"));

	@PreDestroy
	public void destroy() {
		executor.shutdownNow();
	}

	@Override
	public void search(String keyword) throws SearchException {
		try {
			executor.execute(new YouTubeSearchTask(keyword));
		} catch (Exception e) {
			LOG.error("Unexepected error while searching in youtube", e);
			notifyError(new SearchErrorEvent(this, keyword));
		}
	}

	public YouTubeQuery getYouTubeQuery() throws MalformedURLException {
		if (query == null) {
			query = new YouTubeQuery(new URL("http://gdata.youtube.com/feeds/api/videos"));
			query.setOrderBy(YouTubeQuery.OrderBy.RELEVANCE);
			query.setSafeSearch(YouTubeQuery.SafeSearch.NONE);
		}
		return query;
	}

	private final class YouTubeSearchTask implements Runnable {
		private final String keyword;

		private YouTubeSearchTask(String keyword) {
			this.keyword = keyword;
		}

		@Override
		public void run() {
			try {

				getYouTubeQuery().setFullTextQuery(keyword);
				VideoFeed videoFeed = service.query(getYouTubeQuery(), VideoFeed.class);

				float progress = 0;
				float total = videoFeed.getEntries().size();
				for (VideoEntry videoEntry : videoFeed.getEntries()) {
					notifySearchData(new SearchDataEvent(YouTubeSearcher.this, keyword, new YouTubeSearchData(videoEntry)));
					notifySearchProgress(new SearchProgressEvent(YouTubeSearcher.this, keyword, (int) (++progress * 100. / total)));
				}

			} catch (IOException e) {
				LOG.error("Unable to perform search on youtube", e);
				notifyError(new SearchErrorEvent(this, keyword));
			} catch (ServiceException e) {
				LOG.error("Unable to perform search on youtube", e);
				notifyError(new SearchErrorEvent(this, keyword));
			} catch (Exception e) {
				LOG.error("Unexepected error while searching in youtube", e);
				notifyError(new SearchErrorEvent(this, keyword));
			}
		}
	}

}
