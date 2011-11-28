package com.all.downloader.p2p.phexcore.download;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.annotation.EventTopicSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import phex.download.swarming.SWDownloadFile;
import phex.event.PhexEventTopics;

import com.all.core.common.spring.InitializeService;
import com.all.downloader.alllink.AllLink;
import com.all.downloader.bean.DownloadState;
import com.all.downloader.download.CommonManagedDownloader;
import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.downloader.p2p.phexcore.PhexCore;
import com.all.downloader.p2p.phexcore.event.MoreSourcesNeededNotifier;
import com.all.downloader.p2p.phexcore.event.SearcherSourcesListener;
import com.all.downloader.p2p.phexcore.helper.DownloadHelper;
import com.all.downloader.p2p.phexcore.helper.SearchHelper;
import com.all.downloader.p2p.phexcore.search.PhexSearcher;
import com.all.downloader.search.SearchException;
import com.all.downloader.search.SearcherListener;
import com.all.downloader.util.DownloaderUtils;
import com.all.messengine.MessEngine;
import com.all.shared.download.TrackProvider;
import com.all.shared.model.AllMessage;
import com.all.shared.model.Track;
import com.all.shared.stats.usage.UserActions;

@Service
public class PhexDownloader extends CommonManagedDownloader {
	private static final Log log = LogFactory.getLog(PhexDownloader.class);

	public static final String URN_PREFIX = "urn:sha1:";
	public static final String PHEX_DOWNLOAD = "savePath";
	public static final String ADD_CANDIDATES = "addCandidates";
	public static final int PORT = 10012;

	private static final long MAX_WAIT_TO_DOWNLOAD = 50;

	protected Map<String, PhexDownload> downloadIdToPhexDownload = Collections
			.synchronizedMap(new HashMap<String, PhexDownload>());
	protected Map<String, String> phexShaHashToDownloadId = Collections.synchronizedMap(new HashMap<String, String>());
	protected Map<String, SearcherListener> downloadIdToListener = Collections
			.synchronizedMap(new HashMap<String, SearcherListener>());

	@Autowired
	private PhexCore phexCore;
	@Autowired
	private TrackProvider trackProvider;
	@Autowired
	private ManagedDownloaderConfig downloaderConfig;
	@Autowired
	private ScheduledExecutorService scheduler;
	@Autowired
	private DownloadHelper downloadHelper;
	@Autowired
	private PhexSearcher phexSearcher;
	@Autowired
	private SearchHelper searchHelper;
	@Autowired
	private MessEngine messEngine;

	private final PhexDownloadFactory phexDownloadFactory = new PhexDownloadFactory();

	public PhexDownloader() {
	}

	PhexDownloader(PhexCore phexCore) {
		this.phexCore = phexCore;
	}

	@PostConstruct
	public void initialize() {
		scheduler.scheduleWithFixedDelay(new DownloadMonitor(), 1, 1, TimeUnit.SECONDS);
	}

	@InitializeService
	public void initializeService() throws Exception {
		phexCore.processPhexAnotation(this);
	}

	@Override
	public void findSources(final String downloadId) throws DownloadException {
		scheduler.execute(new FindSourcesTask(downloadId));
	}

	private void getLinkAndTrackData(String downloadId, PhexDownload phexDownload) throws DownloadException {
		verifyIfDownloadIdEmpty(downloadId);
		Track track = trackProvider.getTrack(downloadId);
		String allLinkString = track.getDownloadString();
		verifyIfAlllinkEmpty(downloadId, allLinkString);
		phexDownload.setAllLink(allLinkString);
		AllLink allLink = AllLink.parse(allLinkString);
		String fileHashCode = allLink.getUrnSha();
		verifyIfUrnshaEmpty(downloadId, fileHashCode);
	}

	@Override
	public void download(final String downloadId) throws DownloadException {
		PhexDownload phexDownload = createPhexDownload();
		getLinkAndTrackData(downloadId, phexDownload);
		if (!phexCore.isSeedingGnutella()) {
			String hashcode = phexDownload.getHashcode();
			log.debug("fileHashCode: " + phexDownload.getFileHashcode());
			if (hashcode != null && phexDownload.getFileHashcode() == null) {
				log.error("hashCode is not null for the track with id: " + downloadId + " so I cant download it, at the moment");
				return;
			}
		}
		verifyIfthisUrnshaIsManaged(downloadId, phexDownload);

		setValuesToPhexDownload(downloadId);

		String path = downloaderConfig.getProperty(PHEX_DOWNLOAD);
		File fileToDownload = phexDownload.getFile(path);

		verifyIfThisDownloadExist(downloadId, phexDownload, fileToDownload);

		scheduler.submit(new GnutellaDownload(downloadId));
	}

	private PhexDownload createPhexDownload() {
		return phexDownloadFactory.newDownload();
	}

	private void verifyIfThisDownloadExist(final String downloadId, PhexDownload phexDownload, File fileToDownload) {
		if (fileToDownload.exists()) {
			log.debug("File :" + fileToDownload + " exist, so I'm going to notify download complete");
			phexDownload.setProgress(100);
			notifyDownloadUpdated(downloadId, phexDownload);
			notifyDownloadComplete(downloadId, fileToDownload);
			removeDownload(downloadId);
		}
	}

	private void setValuesToPhexDownload(final String downloadId) {
		Track track = trackProvider.getTrack(downloadId);
		String trackName = DownloaderUtils.convertToLocalSystemFilename(track.getName());
		PhexDownload phexDownload = downloadIdToPhexDownload.get(downloadId);
		phexDownload.setFileName(trackName);
		phexDownload.setSize(track.getSize());
		phexDownload.setFileExtension(track.getFileFormat());
	}

	private void verifyIfthisUrnshaIsManaged(final String downloadId, PhexDownload phexDownload) {
		if (!downloadIdToPhexDownload.containsKey(downloadId)) {
			managePhexDownload(downloadId, phexDownload);
		}
	}

	private void verifyIfUrnshaEmpty(final String downloadId, String fileHashCode) throws DownloadException {
		if (StringUtils.isEmpty(fileHashCode)) {
			log.error("urnSha is null or empty for the track with id: " + downloadId + " so I cant download it");
			throw new DownloadException("urnSha is null for the track with id: " + downloadId);
		}
	}

	private void verifyIfAlllinkEmpty(final String downloadId, String allLinkString) throws DownloadException {
		if (StringUtils.isEmpty(allLinkString)) {
			log.error("Alllink is null or empty for the track with id: " + downloadId + " so I cant download it");
			throw new DownloadException("Alllink is null or empty for the track with id: " + downloadId);
		}
	}

	private void verifyIfDownloadIdEmpty(final String downloadId) throws DownloadException {
		if (StringUtils.isEmpty(downloadId)) {
			log.warn("Downloading not started. The download id is empty or null");
			throw new DownloadException("The download id is empty or null");
		}
	}

	String extractHashcodeFromUrn(String urnSha1) {
		return urnSha1.replaceFirst(Pattern.quote(URN_PREFIX), "");
	}

	void removeDownload(String downloadId) {
		PhexDownload phexDownload = downloadIdToPhexDownload.remove(downloadId);
		if (phexDownload != null) {
			phexShaHashToDownloadId.remove(phexDownload.getFileHashcode());
		}
	}

	void startGnutellaDownload(String downloadId) throws SearchException {
		PhexDownload phexDownload = downloadIdToPhexDownload.get(downloadId);
		log.debug("Calling to download to: " + phexDownload.getFileName());
		downloadHelper.lookForCandidatesFromSearchResults(phexSearcher.getAllSearchResults(), phexDownload);
		phexDownload.setStartTimeStamp();
	}

	void addCandidateToDownload(PhexDownload phexDownload, String host) {
		try {
			downloadHelper.addCandidateToDownload(host, phexDownload);
		} catch (Exception e) {
			log.error(e, e);
		}
	}

	@EventTopicSubscriber(topic = PhexEventTopics.Download_File_Completed)
	public void onDownloadFileCompletedEvent(String topic, SWDownloadFile downloadFile) {
		try {

			String urnsha = downloadFile.getFileURN().getAsString();
			String fileHashCode = extractHashcodeFromUrn(urnsha);
			String downloadId = phexShaHashToDownloadId.get(fileHashCode);
			PhexDownload phexDownload = downloadIdToPhexDownload.get(downloadId);

			if (phexDownload != null) {
				File destinationFile = downloadFile.getDestinationFile();
				log.debug("destinationFile: " + destinationFile);

				removeDownload(downloadId);

				notifyDownloadComplete(downloadId, destinationFile);
			}
		} catch (Exception e) {
			log.error("Error while processing download complete event", e);
		}
	}

	@Override
	public void delete(String downloadId) throws DownloadException {
		PhexDownload phexDownload = downloadIdToPhexDownload.get(downloadId);
		if (phexDownload != null) {
			// We need to remove downloadId from Map, otherwise updateStatus
			// will count with inexistent values.
			downloadIdToPhexDownload.remove(downloadId);
			SWDownloadFile swDownloadFile = phexDownload.getDownloadFile();
			if (swDownloadFile != null) {
				phexCore.removeSWDownloadFile(swDownloadFile);
			} else {
				log.warn("Could not completely remove the download becasue the id wasn't being managed: " + downloadId);
			}
		}
	}

	@Override
	public void pause(String downloadId) throws DownloadException {
		log.debug("pausing download with id " + downloadId);
		if (StringUtils.isEmpty(downloadId)) {
			return;
		}

		PhexDownload phexDownload = downloadIdToPhexDownload.get(downloadId);

		if (phexDownload == null) {
			return;
		}

		SWDownloadFile downloadFile = phexDownload.getDownloadFile();
		if (downloadFile != null) {
			downloadFile.stopDownload();
		}
	}

	@Override
	public void resume(String downloadId) throws DownloadException {
		log.debug("resuming download with id " + downloadId);
		if (StringUtils.isEmpty(downloadId)) {
			return;
		}

		PhexDownload phexDownload = downloadIdToPhexDownload.get(downloadId);

		if (phexDownload == null) {
			return;
		}

		SWDownloadFile downloadFile = phexDownload.getDownloadFile();
		if (downloadFile != null) {
			downloadFile.startDownload();
		}

	}

	protected void managePhexDownload(String downloadId, PhexDownload phexDownload) {
		phexDownload.setDownloadId(downloadId);
		downloadIdToPhexDownload.put(downloadId, phexDownload);
		phexShaHashToDownloadId.put(phexDownload.getFileHashcode(), downloadId);
	}

	void notifyDownloadUpdated(String downloadId, PhexDownload phexDownload) {
		DownloadUpdateEvent downloadUpdateEvent = new DownloadUpdateEvent(this, downloadId, phexDownload);
		this.notifyDownloadUpdated(downloadUpdateEvent);
	}

	void notifyDownloadComplete(String downloadId, File destinationFile) {
		log.info("Track " + downloadId + " was succesfully downloaded." + managedListeners);
		DownloadCompleteEvent completeEvent = new DownloadCompleteEvent(this, downloadId, destinationFile);
		this.notifyDownloadCompleted(completeEvent);
		messEngine.send(new AllMessage<Integer>(UserActions.USER_ACTION_MESSAGE_TYPE,
				UserActions.Downloads.GNUTELLA_DOWNLOAD));
	}

	void updateStatus() {
		synchronized (downloadIdToPhexDownload) {
			for (Entry<String, PhexDownload> entry : downloadIdToPhexDownload.entrySet()) {
				String downloadId = entry.getKey();
				PhexDownload phexDownload = entry.getValue();

				phexDownload.updateStatus();

				notifyDownloadUpdated(downloadId, phexDownload);
			}
		}
	}

	void checkIfDownloading() {
		synchronized (downloadIdToPhexDownload) {
			for (Entry<String, PhexDownload> entry : downloadIdToPhexDownload.entrySet()) {
				String downloadId = entry.getKey();
				PhexDownload phexDownload = entry.getValue();

				Long diffTime = System.currentTimeMillis() - phexDownload.getStartTimeStamp();
				if (phexDownload.getState() != DownloadState.MoreSourcesNeeded && diffTime / 1000 > MAX_WAIT_TO_DOWNLOAD) {
					if (phexDownload.getFreeNodes() == 0) {
						log.info("No free peers to: " + downloadId + " and downloading time is: " + diffTime / 1000
								+ " more than : " + MAX_WAIT_TO_DOWNLOAD + " seconds. I'm going to notify Need More Sources");
						SearcherListener listener = downloadIdToListener.get(downloadId);
						new MoreSourcesNeededNotifier(this, listener, phexDownload, this.searchHelper).moreSourcesNeeded();
						removeDownloadId(downloadId);
					}
				}
			}
		}
	}

	void removeDownloadId(String downloadId) {
		try {
			delete(downloadId);
		} catch (DownloadException e) {
			log.error(e, e);
		}
	}

	class FindSourcesTask implements Runnable {
		private final String downloadId;

		public FindSourcesTask(String downloadId) {
			this.downloadId = downloadId;
		}

		@Override
		public void run() {
			final PhexDownload phexDownload = createPhexDownload();
			try {
				getLinkAndTrackData(downloadId, phexDownload);
			} catch (DownloadException doe) {
				phexDownload.setState(DownloadState.Error);
				SearchSourcesEvent searchSourcesEvent = new SearchSourcesEvent(PhexDownloader.this, downloadId, phexDownload);
				PhexDownloader.this.notifySearchSourcesResult(searchSourcesEvent);
			}

			Track track = trackProvider.getTrack(downloadId);

			String keyword = DownloaderUtils.convertToLocalSystemFilename(new StringBuilder(track.getName()).append(" ")
					.append(track.getArtist()).append(" ").append(track.getAlbum()).toString());
			final SearcherSourcesListener listener = new SearcherSourcesListener(PhexDownloader.this, keyword, downloadId,
					phexDownload);
			downloadIdToListener.put(downloadId, listener);

			try {
				boolean foundSources = downloadHelper.lookForSources(phexSearcher.getAllSearchResults(),
						phexDownload.getFileHashcode());
				if (foundSources) {
					log.info("Sources found I'm going to notify : " + DownloadState.ReadyToDownload + " to "
							+ phexDownload.getFileHashcode());
					phexDownload.setState(DownloadState.ReadyToDownload);
					SearchSourcesEvent searchSourcesEvent = new SearchSourcesEvent(PhexDownloader.this, downloadId, phexDownload);
					notifySearchSourcesResult(searchSourcesEvent);
					return;
				}
				phexSearcher.addSearcherListener(listener);
				phexSearcher.search(keyword);
			} catch (SearchException see) {
				log.warn(see, see);
			} catch (DownloadException doe) {
				log.error(doe, doe);
			}
			ScheduledFuture<?> schedule = scheduler.schedule(new MoreSourcesNeededNotifier(PhexDownloader.this, listener,
					phexDownload, searchHelper), phexCore.getDownloaderTimeout(), TimeUnit.SECONDS);
			listener.setFuture(schedule);
		}
	}

	class DownloadMonitor implements Runnable {
		@Override
		public void run() {
			try {
				updateStatus();
				checkIfDownloading();
			} catch (Exception e) {
				log.error("Unexpected exception in downloadsMonitor.", e);
			}
		}
	}

	class GnutellaDownload implements Runnable {
		private final String downloadId;

		public GnutellaDownload(String downloadId) {
			this.downloadId = downloadId;
		}

		@Override
		public void run() {
			try {
				startGnutellaDownload(downloadId);
			} catch (Exception e) {
				log.error(e, e);
			}
		}
	}

	PhexDownload createPhexDownload(String allLinkAsString) {
		return new PhexDownload(allLinkAsString);
	}

	@Override
	public int getDownloaderPriority() {
		return phexCore.getDownloaderPriority();
	}

	@Override
	public void notifySearchSourcesResult(SearchSourcesEvent searchSourcesEvent) {
		super.notifySearchSourcesResult(searchSourcesEvent);
	}

	@Override
	public void notifyDownloadUpdated(DownloadUpdateEvent downloadUpdateEvent) {
		super.notifyDownloadUpdated(downloadUpdateEvent);
	}

	class PhexDownloadFactory {
		PhexDownload newDownload() {
			return new PhexDownload();
		}
	}
}
