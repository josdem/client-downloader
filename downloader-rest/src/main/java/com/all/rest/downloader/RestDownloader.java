package com.all.rest.downloader;

import static com.all.shared.messages.MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE;
import static com.all.shared.messages.MessEngineConstants.TRACK_SEEDERS_REQUEST_TYPE;
import static com.all.shared.messages.MessEngineConstants.TRACK_SEEDERS_RESPONSE_TYPE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.commons.IncrementalNamedThreadFactory;
import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;
import com.all.downloader.download.CommonManagedDownloader;
import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadStatusImpl;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.downloader.util.DownloaderUtils;
import com.all.messengine.MessEngine;
import com.all.messengine.MessageListener;
import com.all.rest.config.RestClientConfig;
import com.all.rest.web.RestService;
import com.all.shared.download.RestUploadRequest;
import com.all.shared.download.TrackProvider;
import com.all.shared.download.TrackSeeders;
import com.all.shared.model.AllMessage;
import com.all.shared.model.Track;
import com.all.shared.stats.usage.UserActions;

@Service
public class RestDownloader extends CommonManagedDownloader implements MessageListener<AllMessage<TrackSeeders>> {

	private final Log log = LogFactory.getLog(this.getClass());

	private final RestDownloadFactory downloadFactory = new RestDownloadFactory();

	private Map<String, RestDownload> currentDownloads = new HashMap<String, RestDownload>();

	private ExecutorService downloadsExecutor = Executors
			.newCachedThreadPool(new IncrementalNamedThreadFactory("RestDownload"));
	@Autowired
	private RestClientConfig restConfig;
	@Autowired
	private RestService restService;
	@Autowired
	private MessEngine messEngine;
	@Autowired
	private TrackProvider trackProvider;

	@PostConstruct
	public void init() {
		messEngine.addMessageListener(TRACK_SEEDERS_RESPONSE_TYPE, this);
	}

	@Override
	public void onMessage(AllMessage<TrackSeeders> message) {
		TrackSeeders response = message.getBody();
		String downloadId = response.getTrackId();
		log.debug("Receiving seeders from Downloadpeer for " + downloadId + " : " + response.getSeeders());
		RestDownload restDownload = currentDownloads.get(downloadId);
		if (restDownload != null) {
			restDownload.addSeeders(response.getSeeders());
		}
	}

	@Override
	public int getDownloaderPriority() {
		return restConfig.getDownloaderPriority();
	}

	@Override
	public void delete(String downloadId) throws DownloadException {
		RestDownload download = currentDownloads.remove(downloadId);
		if (download != null) {
			download.cancel();
		}
	}

	@Override
	public void findSources(String downloadId) {
		if (currentDownloads.containsKey(downloadId)) {
			throw new IllegalStateException("Download " + downloadId + " is already in progress.");
		}
		RestDownload download = downloadFactory.createDownload(trackProvider.getTrack(downloadId), false);
		currentDownloads.put(downloadId, download);
		downloadsExecutor.execute(download);
	}

	@Override
	public void download(String downloadId) throws DownloadException {
		RestDownload download = currentDownloads.get(downloadId);
		if (download == null) {
			download = downloadFactory.createDownload(trackProvider.getTrack(downloadId), true);
			currentDownloads.put(downloadId, download);
			downloadsExecutor.execute(download);
		} else {
			download.start();
		}
	}

	public DownloadStatus getStatus(String downloadId) throws DownloadException {
		RestDownload restDownload = currentDownloads.get(downloadId);
		return restDownload != null ? restDownload.getStatus() : null;
	}

	@Override
	public void pause(String downloadId) throws DownloadException {
		RestDownload download = currentDownloads.get(downloadId);
		if (download != null) {
			download.pause();
		}
	}

	@Override
	public void resume(String downloadId) throws DownloadException {
		RestDownload download = currentDownloads.get(downloadId);
		if (download != null) {
			download.resume();
		}
	}

	class RestDownload implements Runnable {

		private static final int TOTAL_CHUNKS = 100;

		private static final long PAUSE_SLEEP_TIME = 1000;

		private final String downloadId;

		private final AtomicBoolean paused = new AtomicBoolean(false);

		private final AtomicBoolean canceled = new AtomicBoolean(false);

		private final DownloadStatusImpl status;

		private final Track track;

		private final Collection<String> seeders = new ArrayList<String>();

		private FileOutputStream fileWriter = null;

		private long startTime;

		private boolean uploadRequested = false;

		private int awaitTime = 0;

		private int failedAttempts = 0;

		private int currentChunk = 0;

		private AtomicBoolean waiting = new AtomicBoolean(false);

		private final AtomicBoolean started;

		private AtomicBoolean delayNextRequest = new AtomicBoolean(false);

		private String incompletePath;

		private String completePath;
		
		private final int DOWNLOAD_TIME_PRECISION = 1024;

		private RestDownload(Track track, boolean startNow) {
			this.track = track;
			this.downloadId = track.getHashcode();
			this.status = new DownloadStatusImpl(downloadId);
			this.started = new AtomicBoolean(startNow);
		}

		public void start() {
			started.set(true);
		}

		public void addSeeders(List<String> foundSeeders) {
			foundSeeders.remove(restConfig.getUserId());
			this.seeders.addAll(foundSeeders);
			this.waiting.set(false);
		}

		public void resume() {
			paused.set(false);
			startTime = System.currentTimeMillis();
		}

		public void pause() {
			paused.set(true);
			log.info("Pausing download " + downloadId);
		}

		public DownloadStatus getStatus() {
			return status;
		}

		public void cancel() {
			log.info("Cancelling download " + downloadId);
			canceled.set(true);
		}

		@Override
		public void run() {
			log.info("Starting rest download for " + downloadId);
			download();
		}

		private void findSources() throws InterruptedException {
			if (!started.get()) {
				log.info("Finding REST sources for " + downloadId);
				status.setState(DownloadState.Searching);
				while (seeders.isEmpty()) {
					if (canceled.get()) {
						return;
					}
					if (isCached()) {
						seeders.add("RestDownloadServer");
					} else {
						requestSeedersToDownloadPeer();
					}
					awaitForSeeders();
					updateSearchStatus();
				}
			}
		}

		private void updateSearchStatus() {
			DownloadState currentState = seeders.isEmpty() ? DownloadState.MoreSourcesNeeded : DownloadState.ReadyToDownload;
			if (status.getState() != currentState) {
				log.info("Notifying state " + currentState + " for " + downloadId);
				status.setState(currentState);
				notifySearchSourcesResult(new SearchSourcesEvent(RestDownloader.this, downloadId, status));
			}

		}

		private void requestSeedersToDownloadPeer() throws InterruptedException {
			if (delayNextRequest.get()) {
				Thread.sleep(5000);
				delayNextRequest.set(false);
			}
			messEngine.send(new AllMessage<TrackSeeders>(TRACK_SEEDERS_REQUEST_TYPE, new TrackSeeders(downloadId, restConfig
					.getUserId())));
			waiting.set(true);
		}

		private void awaitForSeeders() throws InterruptedException {
			int seederAwaitTime = 0;
			while (!canceled.get() && waiting.get() && seederAwaitTime < restConfig.getDownloaderSearchTimeout()
					&& seeders.isEmpty()) {
				seederAwaitTime += 100;
				Thread.sleep(100);
			}
			if (seeders.isEmpty()) {
				delayNextRequest.set(true);
			}
		}

		private boolean isCached() {
			byte[] chunk = restService.getChunk(downloadId, 0);
			if (chunk != null) {
				return chunk.length > 0;
			}
			return false;
		}

		private void download() {
			try {
				findSources();
				while (!started.get() && !canceled.get()) {
					Thread.sleep(100);
				}
				startTime = System.currentTimeMillis();
				downloadByChunks();
			} catch (Exception e) {
				log.error("Unexepected error in download.", e);
				notifyState(DownloadState.Error);
			} finally {
				try {
					removeIncompleteFile();
				} catch (IOException e) {
					log.error(e, e);
				}
				currentDownloads.remove(downloadId);
			}
		}

		private void downloadByChunks() throws InterruptedException, IOException {
			while (currentChunk < 100) {
				if (canceled.get()) {
					log.info("Download has been canceled " + downloadId);
					notifyState(DownloadState.Canceled);
					return;
				}
				if (paused.get()) {
					notifyState(DownloadState.Paused);
					Thread.sleep(PAUSE_SLEEP_TIME);
					continue;
				}
				downloadChunk();
			}
			notifyDownloadCompleted();
		}

		private void downloadChunk() throws InterruptedException, IOException {
			status.setState(DownloadState.Downloading);
			log.debug("Requesting chunk " + currentChunk + " to RestServer...");
			long downloadStartTime = System.nanoTime();
			byte[] chunk = restService.getChunk(downloadId, currentChunk);
			double downloadTotalTime = (System.nanoTime() - downloadStartTime)/1000000.0;

			if (chunk == null || chunk.length == 0) {
				requestUploadIfRequired();
			} else {
				log.debug("Chunk " + currentChunk + " received...");
				writeChunk(chunk);
				currentChunk++;
				updateStatus(currentChunk, chunk,downloadTotalTime);
				notifyDownloadUpdated(new DownloadUpdateEvent(RestDownloader.this, downloadId, status));
			}
		}

		private void requestUploadIfRequired() throws InterruptedException {
			if (awaitTime > restConfig.getChunkAwaitTimeout(TimeUnit.MILLISECONDS)) {
				uploadRequested = false;
			}
			if (!uploadRequested) {
				RestUploadRequest request = new RestUploadRequest(restConfig.getUserId(), downloadId);
				request.setFromChunk(currentChunk);
				messEngine.send(new AllMessage<RestUploadRequest>(REST_UPLOAD_TRACK_REQUEST_TYPE, request));
				log.info("Will request upload to " + seeders + " for " + downloadId);
				failedAttempts = 0;
				awaitTime = 0;
				uploadRequested = true;
				Thread.sleep(restConfig.getMinChunkAwaitDelay(TimeUnit.MILLISECONDS));
			}
			failedAttempts++;
			awaitTime += restConfig.getMinChunkAwaitDelay(TimeUnit.MILLISECONDS) * failedAttempts;
			Thread.sleep(restConfig.getMinChunkAwaitDelay(TimeUnit.MILLISECONDS) * failedAttempts);
		}

		private void notifyState(DownloadState state) {
			status.setState(state);
			notifyDownloadUpdated(new DownloadUpdateEvent(RestDownloader.this, downloadId, status));
		}

		private void updateStatus(int currentChunk, byte[] chunk, double downloadTotalTime) {
			status.setProgress(currentChunk);
			status.setFreeNodes(1);
			long totalMillis = System.currentTimeMillis() - startTime;
			if (totalMillis > 0) {
				long remainingSeconds = (TOTAL_CHUNKS - currentChunk) * totalMillis / currentChunk;
				status.setRemainingSeconds((int) (remainingSeconds / 1000.0));
				status.setDownloadRate((long)((chunk.length*DOWNLOAD_TIME_PRECISION)/downloadTotalTime));
			}
		}

		private void notifyDownloadCompleted() throws IOException {
			log.info("Track " + downloadId + " was succesfully downloaded.");
			File completedFile = moveDownloadedFile();
			if (completedFile != null) {
				status.setState(DownloadState.Complete);
				DownloadCompleteEvent completeEvent = new DownloadCompleteEvent(RestDownloader.this, downloadId, completedFile);
				RestDownloader.this.notifyDownloadCompleted(completeEvent);
				AllMessage<Integer> userActionMessage = new AllMessage<Integer>(UserActions.USER_ACTION_MESSAGE_TYPE, UserActions.Downloads.MC_DOWNLOAD);
				userActionMessage.putProperty(CommonManagedDownloader.TRACK_ID, downloadId);
				messEngine.send(userActionMessage);
			}
		}

		private void removeIncompleteFile() throws IOException {
			if (fileWriter != null) {
				try {
					fileWriter.close();
					fileWriter = null;
				} catch (IOException e) {
					log.error(e, e);
				}
			}
			File track = new File(getIncompleteFilePath());
			if (track.exists()) {
				track.delete();
			}
		}

		private String getIncompleteFilePath() throws IOException {
			if (incompletePath == null) {
				incompletePath = DownloaderUtils.getValidFilePath(restConfig.getIncompleteDownloadsPath(), DownloaderUtils
						.getValidFileName("REST" + track.getName(), track.getFileFormat()));
			}
			return incompletePath;
		}

		private String getCompleteFilePath() throws IOException {
			if (completePath == null) {
				completePath = DownloaderUtils.getValidFilePath(restConfig.getCompleteDownloadsPath(), DownloaderUtils
						.getValidFileName(track.getName(), track.getFileFormat()));
			}
			return completePath;
		}

		private void writeChunk(byte[] chunk) throws IOException {
			if (fileWriter == null) {
				File downloadedTrack = new File(getIncompleteFilePath());
				log.debug("Creating file for " + downloadedTrack);
				if (!downloadedTrack.exists()) {
					downloadedTrack.createNewFile();
				}
				fileWriter = new FileOutputStream(downloadedTrack, false);
			}
			fileWriter.write(chunk);
		}

		private synchronized File moveDownloadedFile() throws IOException {
			if (fileWriter != null) {
				fileWriter.close();
				fileWriter = null;
			}
			File file = new File(getIncompleteFilePath());
			if (file.exists()) {
				try {
					File completedFile = new File(getCompleteFilePath());
					boolean success = file.renameTo(completedFile);
					if (success) {
						return completedFile;
					} else {
						log.error("Could not move incomplete file to final destination.");
					}
				} catch (Exception e) {
					log.error("Could not move incomplete file to final destination.", e);
				}
			}
			return null;
		}

	}

	class RestDownloadFactory {
		public RestDownload createDownload(Track track, boolean startNow) {
			return new RestDownload(track, startNow);
		}
	}

}
