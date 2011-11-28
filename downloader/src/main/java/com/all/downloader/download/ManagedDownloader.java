package com.all.downloader.download;

public interface ManagedDownloader extends Downloader {

	void findSources(String downloadId) throws DownloadException;
	
	int getDownloaderPriority();
	
	void addManagedDownloadListener(String downloadId, ManagedDownloaderListener listener);

	void removeManagedDownloadListener(String downloadId, ManagedDownloaderListener listener);

}
