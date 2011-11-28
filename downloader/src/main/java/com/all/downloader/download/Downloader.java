package com.all.downloader.download;

public interface Downloader {

	void download(String downloadId) throws DownloadException;

	void delete(String downloadId) throws DownloadException;

	void pause(String downloadId) throws DownloadException;

	void resume(String downloadId) throws DownloadException;

	void addDownloaderListener(DownloaderListener listener);

	void removeDownloaderListener(DownloaderListener listener);

}
