package com.all.downloader.download;


public interface DownloaderListener {
	
	 void onDownloadUpdated(DownloadUpdateEvent downloadUpdateEvent);

	 void onDownloadCompleted(DownloadCompleteEvent downloadCompleteEvent);
	 
}
