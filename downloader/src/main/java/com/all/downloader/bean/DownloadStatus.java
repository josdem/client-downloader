package com.all.downloader.bean;

public interface DownloadStatus {

	String getDownloadId();
	
	DownloadState getState();

	/**
	 * progress of the download from 0 to 100
	 * 
	 * @return progress as in value
	 */
	int getProgress();

	/**
	 *  @return bytes per second 
	 */
	long getDownloadRate();

	int getFreeNodes();

	int getBusyNodes();
	
	int getRemainingSeconds();

}
