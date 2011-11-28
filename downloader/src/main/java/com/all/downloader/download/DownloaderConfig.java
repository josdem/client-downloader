package com.all.downloader.download;

public interface DownloaderConfig {

	String getUserId();

	String getUserConfigPath();

	String getIncompleteDownloadsPath();

	String getCompleteDownloadsPath();

}
