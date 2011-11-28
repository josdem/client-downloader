package com.all.downloader.download;

public interface ManagedDownloaderConfig extends DownloaderConfig {

	int getDownloaderPriority(String downloaderKey);

	int getDownloaderSearchTimeout(String downloaderKey);

	String getProperty(String propertyKey);
}
