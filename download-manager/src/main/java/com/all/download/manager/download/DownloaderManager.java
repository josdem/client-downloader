package com.all.download.manager.download;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;
import com.all.downloader.download.CommonDownloader;
import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.DownloaderListener;
import com.all.downloader.download.ManagedDownloader;

@Service
public class DownloaderManager extends CommonDownloader implements DownloaderListener {

	@Autowired
	private Collection<ManagedDownloader> managedDownloaders;
	private final Map<String, ManagedDownload> currentDownloads = new HashMap<String, ManagedDownload>();
	private final ManagedDownloadFactory managedDownloadFactory = new ManagedDownloadFactory();

	@PostConstruct
	void validateDownloaderPriorities() {
		log.info("Found managed downloaders: " + managedDownloaders);

		if (managedDownloaders.isEmpty()) {
			throw new IllegalArgumentException("No managed downloaders found");
		}

		Set<Integer> priorities = new HashSet<Integer>();
		checkPrioritiesAreNotRepeated(priorities);
		checkPrioritiesAreSequentialSartingAtZero(priorities);

		this.managedDownloaders = Collections.unmodifiableCollection(managedDownloaders);
	}

	private void checkPrioritiesAreNotRepeated(Set<Integer> priorities) {
		for (ManagedDownloader managedDownloader : managedDownloaders) {
			if (!priorities.add(managedDownloader.getDownloaderPriority())) {
				throw new IllegalArgumentException(
						"Cannot have more than one ManagedDownloader with the same priority: "
								+ managedDownloader.getDownloaderPriority());
			}
		}
	}

	private void checkPrioritiesAreSequentialSartingAtZero(Set<Integer> priorities) {
		for (int priority = 0; priority < managedDownloaders.size(); priority++) {
			if (!priorities.contains(priority)) {
				throw new IllegalArgumentException(
						"ManagedDownloader priorities are not sequential, starting at priority cero");
			}
		}
	}

	@Override
	public void download(String downloadId) throws DownloadException {
		if (currentDownloads.containsKey(downloadId)) {
			throw new DownloadException("Already downloading " + downloadId);
		}

		ManagedDownload managedDownload = managedDownloadFactory.create(downloadId);

		currentDownloads.put(downloadId, managedDownload);

		managedDownload.download();
	}

	@Override
	public void delete(String downloadId) throws DownloadException {
		try {
			getDownload(downloadId).delete();
		} finally {
			currentDownloads.remove(downloadId);
		}
	}

	@Override
	public void pause(String downloadId) throws DownloadException {
		getDownload(downloadId).pause();
	}

	@Override
	public void resume(String downloadId) throws DownloadException {
		getDownload(downloadId).resume();
	}

	private ManagedDownload getDownload(String downloadId) throws DownloadException {
		ManagedDownload download = currentDownloads.get(downloadId);
		if (download == null) {
			throw new DownloadException(String.format("Could not find downloadId[%s] in current downloads", downloadId));
		}
		return download;
	}

	@Override
	public void onDownloadUpdated(DownloadUpdateEvent downloadUpdateEvent) {
		DownloadStatus downloadStatus = downloadUpdateEvent.getDownloadStatus();
		DownloadState state = downloadStatus.getState();
		switch (state) {
		case Error:
		case MoreSourcesNeeded:
			try {
				delete(downloadUpdateEvent.getDownloadId());
			} catch (DownloadException e) {
				log.error("Unable to delete download that reported status " + state, e);
			}
		}
		notifyDownloadUpdated(downloadUpdateEvent);
	}

	@Override
	public void onDownloadCompleted(DownloadCompleteEvent downloadCompleteEvent) {
		currentDownloads.remove(downloadCompleteEvent.getDownloadId());
		notifyDownloadCompleted(downloadCompleteEvent);
	}

	class ManagedDownloadFactory {
		public ManagedDownload create(String downloadId) {
			ManagedDownload managedDownload = new ManagedDownload(downloadId);
			managedDownload.setDownloaderListener(DownloaderManager.this);
			managedDownload.setManagedDownloaders(managedDownloaders);
			return managedDownload;
		}
	}

}
