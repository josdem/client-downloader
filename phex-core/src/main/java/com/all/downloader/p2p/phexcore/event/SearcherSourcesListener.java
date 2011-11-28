package com.all.downloader.p2p.phexcore.event;

import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.downloader.p2p.phexcore.download.PhexDownload;
import com.all.downloader.p2p.phexcore.download.PhexDownloader;
import com.all.downloader.search.SearchDataEvent;
import com.all.downloader.search.SearchErrorEvent;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.SearcherListener;

public class SearcherSourcesListener implements SearcherListener {
	private final PhexDownload phexDownload;
	private final String downloadId;
	private final PhexDownloader phexDownloader;
	private ScheduledFuture<?> schedule;
	private Log log = LogFactory.getLog(SearcherSourcesListener.class);
	
	public SearcherSourcesListener(PhexDownloader phexDownloader, String keyword, String downloadId, PhexDownload phexDownload) {
		this.phexDownloader = phexDownloader;
		this.downloadId = downloadId;
		this.phexDownload = phexDownload;
	}
	
	@Override
	public void updateProgress(SearchProgressEvent updateProgressEvent) {
	}

	@Override
	public void updateSearchData(SearchDataEvent updateSearchEvent) {
		String fileHash = updateSearchEvent.getSearchData().getFileHash();
		if(fileHash.contains(phexDownload.getFileHashcode())){
			log.info("Sources found I'm going to notify : " + DownloadState.ReadyToDownload + " to " + updateSearchEvent.getSearchData().getFileHash());
			this.schedule.cancel(true);
			phexDownload.setState(DownloadState.ReadyToDownload);
			SearchSourcesEvent searchSourcesEvent = new SearchSourcesEvent(phexDownloader, downloadId, phexDownload);
			phexDownloader.notifySearchSourcesResult(searchSourcesEvent);
		}
	}
	
	public void setFuture(ScheduledFuture<?> schedule) {
		this.schedule = schedule;
	}

	@Override
	public void onError(SearchErrorEvent searchErrorEvent) {
	}
}
