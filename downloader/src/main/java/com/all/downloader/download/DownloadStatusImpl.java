package com.all.downloader.download;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;

public class DownloadStatusImpl implements DownloadStatus {
	private String downloadId;
	private DownloadState state;
	private long downloadRate;
	private int progress;
	private int remainingSeconds;
	private int freeNodes;
	private int busyNodes;

	public DownloadStatusImpl(String downloadId) {
		this.downloadId = downloadId;
	}

	@Override
	public String getDownloadId() {
		return downloadId;
	}

	@Override
	public long getDownloadRate() {
		return downloadRate;
	}

	public void setDownloadRate(long downloadRate) {
		this.downloadRate = downloadRate;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	@Override
	public int getProgress() {
		return progress;
	}

	public void setRemainingSeconds(int remainingSeconds) {
		this.remainingSeconds = remainingSeconds;
	}

	@Override
	public int getRemainingSeconds() {
		return remainingSeconds;
	}

	public void setState(DownloadState state) {
		this.state = state;
	}

	@Override
	public DownloadState getState() {
		return state;
	}

	public void setFreeNodes(int freeNodes) {
		this.freeNodes = freeNodes;
	}
	
	@Override
	public int getFreeNodes() {
		return freeNodes;
	}

	public void setBusyNodes(int busyNodes) {
		this.busyNodes = busyNodes;
	}
	
	@Override
	public int getBusyNodes() {
		return busyNodes;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.NO_FIELD_NAMES_STYLE);
	}
	
}
