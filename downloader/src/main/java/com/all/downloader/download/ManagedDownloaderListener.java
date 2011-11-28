package com.all.downloader.download;

public interface ManagedDownloaderListener extends DownloaderListener {
	
	void onSearchSources(SearchSourcesEvent searchSourcesEvent);
	
}
