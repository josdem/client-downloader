package com.all.download.manager.download;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;
import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadStatusImpl;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.DownloaderListener;
import com.all.downloader.download.ManagedDownloader;
import com.all.downloader.download.ManagedDownloaderListener;
import com.all.downloader.download.SearchSourcesEvent;

public class ManagedDownload implements ManagedDownloaderListener {
	private final Log log = LogFactory.getLog(ManagedDownload.class);
	private String downloadId;
	private final Set<Integer> blockedDownloaders = new HashSet<Integer>();
	private final TreeSet<ManagedDownloader> candidateDownloaders = new TreeSet<ManagedDownloader>(
			new ManagedDownloadPriorityComparator());
	private DownloaderListener listener;
	private Collection<ManagedDownloader> managedDownloaderCollection;
	private ManagedDownloader selectedDownloader;

	@Deprecated
	public ManagedDownload() {
	}

	public ManagedDownload(String downloadId) {
		this.downloadId = downloadId;
	}

	public void setDownloaderListener(DownloaderListener listener) {
		this.listener = listener;
	}

	public void setManagedDownloaders(Collection<ManagedDownloader> managedDownloaders) {
		this.managedDownloaderCollection = managedDownloaders;
	}

	public void download() {
		findSources();
	}

	private synchronized void findSources() {
		log.info("Searching download sources for " + downloadId);
		sendDownloadUpdateEventWithState(DownloadState.Searching);

		for (ManagedDownloader managedDownloader : managedDownloaderCollection) {
			managedDownloader.addManagedDownloadListener(downloadId, this);
			try {
				managedDownloader.findSources(downloadId);
				// block next downloader in priority
				blockedDownloaders.add(managedDownloader.getDownloaderPriority() + 1);
			} catch (DownloadException e) {
				managedDownloader.removeManagedDownloadListener(downloadId, this);
				log.warn(String.format("Error while finding sources for downloadId[%s] in managedDownloader[%s] with message: %s",
						downloadId, managedDownloader, e.getMessage()));
			} catch(Exception e) {
				managedDownloader.removeManagedDownloadListener(downloadId, this);
				log.error(String.format("Unexpected error while finding sources for downloadId[%s] in managedDownloader[%s] ",
						downloadId, managedDownloader), e);
			}
		}
	}

	public void delete() throws DownloadException {
		accept(new ManagedDownloaderVisitor() {
			@Override
			public void visit(ManagedDownloader managedDownloader) throws DownloadException {
				managedDownloader.delete(downloadId);
			}
		});
	}

	public void pause() throws DownloadException {
		accept(new ManagedDownloaderVisitor() {
			@Override
			public void visit(ManagedDownloader managedDownloader) throws DownloadException {
				managedDownloader.pause(downloadId);
			}
		});
	}

	public void resume() throws DownloadException {
		accept(new ManagedDownloaderVisitor() {
			@Override
			public void visit(ManagedDownloader managedDownloader) throws DownloadException {
				managedDownloader.resume(downloadId);
			}
		});
	}

	@Override
	public synchronized void onSearchSources(SearchSourcesEvent searchSourcesEvent) {
		ManagedDownloader downloader = searchSourcesEvent.getDownloader();
		if (selectedDownloader == null) {

			log.debug("searchedSourcesResult received for "
					+ ToStringBuilder.reflectionToString(searchSourcesEvent, ToStringStyle.NO_FIELD_NAMES_STYLE)
					+ ToStringBuilder.reflectionToString(searchSourcesEvent.getDownloadStatus(),
							ToStringStyle.NO_FIELD_NAMES_STYLE));

			DownloadStatus downloadStatus = searchSourcesEvent.getDownloadStatus();

			boolean validEvent = processSearchSourcesEvent(downloadStatus, downloader);
			if (!validEvent) {
				return;
			}

			findDownloaderCandidate(downloadStatus);

		} else {
			log.warn(String.format("Ignoring event from managedDownloader[%s] since download is already in progress",
					downloader));
		}
	}

	private boolean processSearchSourcesEvent(DownloadStatus downloadStatus, ManagedDownloader downloader) {
		switch (downloadStatus.getState()) {
		case ReadyToDownload:
			candidateDownloaders.add(downloader);
			log.info(String.format("Possible candidate downloader[%s] for downloadId[%s]", downloader, downloadId));
			break;
		case Error:
		case MoreSourcesNeeded:
			// unblock next downloader
			blockedDownloaders.remove(downloader.getDownloaderPriority() + 1);
			log.info(String.format("Downloader[%s] did not find sources for downloadId[%s]", downloader, downloadId));
			break;
		default:
			log.error(String.format("An unexpected downloadState[%s] was received for downloadId[%s]",
					downloadStatus.getState(), downloadId));
			return false;
		}
		return true;
	}

	private void findDownloaderCandidate(DownloadStatus downloadStatus) {
		if (!candidateDownloaders.isEmpty()) {
			
			ManagedDownloader firstCandidateDownloader = candidateDownloaders.first();
			selectedDownloader = isBlocked(firstCandidateDownloader) ? null : firstCandidateDownloader;

			if (selectedDownloader != null) {
				startDownloadOnSelectedDownloaderAndCancelOthers();
			}
			
		} else {
			if (blockedDownloaders.isEmpty()) {
				notifyAllDownloadersFailedFindingSourcesAndDeleteTheDownload();
			}
			
		}
	}
	
	private boolean isBlocked(ManagedDownloader managedDownloader) {
		int downloaderPriority = managedDownloader.getDownloaderPriority();
		for (int i = 0; i <= downloaderPriority; i++) {
			if (blockedDownloaders.contains(i)) {
				return true;
			}
		}
		return false;
	}
	
	private void startDownloadOnSelectedDownloaderAndCancelOthers() {
		log.info(String.format("Starting downloadId[%s] with managedDownloader[%s]", downloadId, selectedDownloader));
		sendDownloadUpdateEventWithState(DownloadState.ReadyToDownload);

		try {
			
			selectedDownloader.download(downloadId);
		} catch (Exception e) {
			log.error("Error while starting download " + downloadId, e);
			
			sendDownloadUpdateEventWithState(DownloadState.Error);
			
			selectedDownloader = null;
		} finally {
			for (ManagedDownloader managedDownloader : managedDownloaderCollection) {
				if (managedDownloader == selectedDownloader) {
					continue;
				}
				try {
					managedDownloader.removeManagedDownloadListener(downloadId, this);
					managedDownloader.delete(downloadId);
				} catch (Exception e) {
					log.error("Error while deleting download " + downloadId, e);
				}
			}
		}
	}

	private void notifyAllDownloadersFailedFindingSourcesAndDeleteTheDownload() {
		// TODO wait a configurable time to execute the code in this
		// block
		log.info("Notifying more sources needed becasue all downloaders did not faind srouces or failed, for download " + downloadId);
		sendDownloadUpdateEventWithState(DownloadState.MoreSourcesNeeded);

		for (ManagedDownloader managedDownloader : managedDownloaderCollection) {
			try {
				managedDownloader.removeManagedDownloadListener(downloadId, this);
				managedDownloader.delete(downloadId);
			} catch (DownloadException e) {
				log.error("Error while deleting download " + downloadId);
			}
		}
	}
	
	private void sendDownloadUpdateEventWithState(DownloadState downloadState) {
		DownloadStatus downloadStatus = createDownloadStatus(downloadState);
		DownloadUpdateEvent downloadUpdateEvent = new DownloadUpdateEvent(listener, downloadId, downloadStatus);
		listener.onDownloadUpdated(downloadUpdateEvent);
	}
	
	private DownloadStatus createDownloadStatus(DownloadState downloadState) {
		DownloadStatusImpl downloadStatus = new DownloadStatusImpl(downloadId);
		downloadStatus.setState(downloadState);
		return downloadStatus;
	}

	@Override
	public void onDownloadUpdated(DownloadUpdateEvent downloadUpdateEvent) {
		if (selectedDownloader != null) {
			listener.onDownloadUpdated(downloadUpdateEvent);
		}
	}

	@Override
	public void onDownloadCompleted(DownloadCompleteEvent completeEvent) {
		if (selectedDownloader != null) {
			listener.onDownloadCompleted(completeEvent);
		}
	}

	private void accept(ManagedDownloaderVisitor visitor) {
		if (selectedDownloader != null) {
			try {
				visitor.visit(selectedDownloader);
			} catch (DownloadException de) {
				log.error("Unexpected error in " + downloadId, de);
			}
		} else {
			for (ManagedDownloader managedDownloader : managedDownloaderCollection) {
				try {
					visitor.visit(managedDownloader);
				} catch (DownloadException de) {
					log.error("Unexpected error in " + downloadId, de);
				}
			}
		}
	}
}

interface ManagedDownloaderVisitor {
	public void visit(ManagedDownloader managedDownloader) throws DownloadException;
}

class ManagedDownloadPriorityComparator implements Comparator<ManagedDownloader> {
	public int compare(ManagedDownloader managedDownloader1, ManagedDownloader managedDownloader2) {
		return managedDownloader1.getDownloaderPriority() - managedDownloader2.getDownloaderPriority();
	}
}
