package com.all.downloader.download;

import java.util.HashMap;
import java.util.Map;

public abstract class CommonManagedDownloader extends CommonDownloader
		implements ManagedDownloader {

	protected static final String TRACK_ID = "trackId";

	protected Map<String, ManagedDownloaderListener> managedListeners = new HashMap<String, ManagedDownloaderListener>();

	@Override
	public void addManagedDownloadListener(String downloadId,
			ManagedDownloaderListener listener) {
		managedListeners.put(downloadId, listener);
	}

	@Override
	public void removeManagedDownloadListener(String downloadId,
			ManagedDownloaderListener listener) {
		managedListeners.remove(downloadId);
	}

	protected void notifySearchSourcesResult(
			SearchSourcesEvent searchSourcesEvent) {
		ManagedDownloaderListener managedDownloaderListener = managedListeners
				.get(searchSourcesEvent.getDownloadId());
		if (managedDownloaderListener != null) {
			managedDownloaderListener.onSearchSources(searchSourcesEvent);
		}
	}

	protected void notifyDownloadUpdated(DownloadUpdateEvent downloadUpdateEvent) {
		super.notifyDownloadUpdated(downloadUpdateEvent);
		ManagedDownloaderListener managedDownloaderListener = managedListeners
				.get(downloadUpdateEvent.getDownloadId());
		if (managedDownloaderListener != null) {
			managedDownloaderListener.onDownloadUpdated(downloadUpdateEvent);
		}
	}

	protected void notifyDownloadCompleted(
			DownloadCompleteEvent downloadCompleteEvent) {
		super.notifyDownloadCompleted(downloadCompleteEvent);
		ManagedDownloaderListener managedDownloaderListener = managedListeners
				.get(downloadCompleteEvent.getDownloadId());
		if (managedDownloaderListener != null) {
			managedDownloaderListener
					.onDownloadCompleted(downloadCompleteEvent);
		}
	}

}
