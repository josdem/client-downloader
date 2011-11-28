package com.all.downloader.p2p.phexcore.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.downloader.p2p.phexcore.download.PhexDownload;
import com.all.downloader.p2p.phexcore.download.PhexDownloader;
import com.all.downloader.p2p.phexcore.helper.SearchHelper;
import com.all.downloader.search.SearcherListener;

public class MoreSourcesNeededNotifier implements Runnable {
	private final SearcherListener listener;
	private final PhexDownload phexDownload;
	private final SearchHelper searchHelper;
	private final PhexDownloader phexDownloader;
	private Log log = LogFactory.getLog(SearcherSourcesListener.class);
	
	public MoreSourcesNeededNotifier(PhexDownloader phexDownloader, SearcherListener listener, PhexDownload phexDownload, SearchHelper searchHelper) {
		this.phexDownloader = phexDownloader;
		this.listener = listener;
		this.phexDownload = phexDownload;
		this.searchHelper = searchHelper;
	}
	
	@Override
	public void run() {
		SearchSourcesEvent searchSourcesEvent = prepareStateEvent();
		phexDownloader.notifySearchSourcesResult(searchSourcesEvent);
	}
	
	public void moreSourcesNeeded() {
		SearchSourcesEvent searchSourcesEvent = prepareStateEvent();
		phexDownloader.notifyDownloadUpdated(searchSourcesEvent);
	}

	private SearchSourcesEvent prepareStateEvent() {
		log.info("No sources found I'm going to notify : " + DownloadState.MoreSourcesNeeded + " to " + phexDownload.getFileHashcode());
		searchHelper.removeSearcherSourcesLister(listener);
		phexDownload.setState(DownloadState.MoreSourcesNeeded);
		SearchSourcesEvent searchSourcesEvent = new SearchSourcesEvent(phexDownloader, phexDownload.getHashcode(),
				phexDownload);
		return searchSourcesEvent;
	}

}
