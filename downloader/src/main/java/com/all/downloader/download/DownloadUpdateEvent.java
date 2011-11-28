package com.all.downloader.download;

import java.util.EventObject;

import com.all.downloader.bean.DownloadStatus;

public class DownloadUpdateEvent extends EventObject {
	private static final long serialVersionUID = 5679925768816404294L;
	private final String downloadId;
	private final DownloadStatus downloadStatus;

	public DownloadUpdateEvent(Object source, String downloadId, DownloadStatus downloadStatus) {
		super(source);
		this.downloadId = downloadId;
		this.downloadStatus = downloadStatus;
	}

	public String getDownloadId() {
		return downloadId;
	}

	public DownloadStatus getDownloadStatus() {
		return downloadStatus;
	}
}
