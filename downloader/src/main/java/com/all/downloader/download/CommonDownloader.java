package com.all.downloader.download;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class CommonDownloader implements Downloader {
	protected Log log = LogFactory.getLog(this.getClass());
	protected Set<DownloaderListener> listeners = new CopyOnWriteArraySet<DownloaderListener>();

	@Override
	public void addDownloaderListener(DownloaderListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeDownloaderListener(DownloaderListener listener) {
		listeners.remove(listener);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	protected void notifyDownloadUpdated(DownloadUpdateEvent downloadUpdateEvent) {
		for (DownloaderListener listener : listeners) {
			try {
				listener.onDownloadUpdated(downloadUpdateEvent);
			} catch (Exception e) {
				log.error("Unexpected error in downloader listener", e);
			}
		}
	}

	protected void notifyDownloadCompleted(DownloadCompleteEvent downloadCompleteEvent) {
		for (DownloaderListener listener : listeners) {
			try {
				listener.onDownloadCompleted(downloadCompleteEvent);
			} catch(Exception e) {
				log.error("Unexpected error in downloader listener", e);
			}
		}
	}

}
