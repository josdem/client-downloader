package com.all.downloader.download;

public class DownloaderStub implements Downloader {

	@Override
	public void addDownloaderListener(DownloaderListener listener) {
	}

	@Override
	public void delete(String downloadId) throws DownloadException {
	}

	@Override
	public void download(String downloadId) throws DownloadException {
	}

	@Override
	public void pause(String downloadId) throws DownloadException {
	}

	@Override
	public void resume(String downloadId) throws DownloadException {
	}

	@Override
	public void removeDownloaderListener(DownloaderListener listener) {
	}

}
