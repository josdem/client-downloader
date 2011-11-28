package com.all.downloader.download;

import com.all.downloader.bean.DownloadStatus;

public class SearchSourcesEvent extends DownloadUpdateEvent {

	private static final long serialVersionUID = 1L;
	private final ManagedDownloader downloader;

	public SearchSourcesEvent(ManagedDownloader source, String downloadId, DownloadStatus status) {
		super(source, downloadId, status);
		downloader = source;
	}

	public ManagedDownloader getDownloader() {
		return downloader;
	}

}
