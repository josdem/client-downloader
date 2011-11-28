package com.all.landownloader;

import static com.all.landownloader.LanDownloadMessageType.CANCEL_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.CHUNK_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.PAUSE_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.RESUME_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.START_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.TRACK_REQUEST;
import static com.all.landownloader.LanDownloadMessageType.TRACK_RESPONSE;
import static com.all.landownloader.LanUtils.getLocalAddress;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;
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
import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.downloader.util.DownloaderUtils;
import com.all.messengine.MessEngine;
import com.all.shared.download.TrackProvider;
import com.all.shared.model.AllMessage;
import com.all.shared.model.Track;
import com.all.shared.stats.usage.UserActions;

@Service
public class LanDownloader extends CommonManagedDownloader implements
		LanDownloaderMessageListener {

	public static final String TIMEOUT_KEY = "timeout.lan";

	public static final String PRIORITY_KEY = "priority.lan";

	private static final int MAX_CONCURRENT_LAN_TRANSFER = 10;

	private static final int TOTAL_CHUNKS = 100;

	private final Log log = LogFactory.getLog(this.getClass());

	private Map<String, LanDownload> currentDownloads = Collections
			.synchronizedMap(new HashMap<String, LanDownload>());

	private Map<String, LanTransfer> currentTransfers = Collections
			.synchronizedMap(new HashMap<String, LanTransfer>());

	private final ExecutorService downloadsExecutor = Executors
			.newCachedThreadPool(new IncrementalNamedThreadFactory(
					"LanDownload"));

	private final ExecutorService transfersExecutor = Executors
			.newFixedThreadPool(MAX_CONCURRENT_LAN_TRANSFER,
					new IncrementalNamedThreadFactory("LanTransfer"));

	private final LanDownloadFactory downloadFactory = new LanDownloadFactory();

	private final LanTransferFactory transferFactory = new LanTransferFactory();

	@Autowired
	private LanNetworkingService networkingService;
	@Autowired
	private ManagedDownloaderConfig downloaderConfig;
	@Autowired
	private TrackProvider trackProvider;
	@Autowired
	private MessEngine messEngine;

	@PostConstruct
	public void init() {
		networkingService.addMessageListener(this);
	}

	@PreDestroy
	public void shutdown() {
		downloadsExecutor.shutdownNow();
		transfersExecutor.shutdownNow();
	}

	@Override
	public int getDownloaderPriority() {
		return downloaderConfig.getDownloaderPriority(PRIORITY_KEY);
	}

	private int getSearchTimeout() {
		return downloaderConfig.getDownloaderSearchTimeout(TIMEOUT_KEY);
	}

	@Override
	public void delete(String downloadId) throws DownloadException {
		LanDownload download = currentDownloads.get(downloadId);
		if (download != null) {
			download.cancel();
		}
	}

	@Override
	public void findSources(String downloadId) {
		if (currentDownloads.containsKey(downloadId)) {
			throw new IllegalStateException(
					"This download is already in progress.");
		}

		LanDownload download = downloadFactory.createDownload(
				trackProvider.getTrack(downloadId), false);
		currentDownloads.put(downloadId, download);
		downloadsExecutor.execute(download);
	}

	@Override
	public void download(String downloadId) throws DownloadException {
		LanDownload lanDownload = currentDownloads.get(downloadId);
		if (lanDownload == null) {
			log.info("Starting LAN download for + " + downloadId);
			LanDownload download = downloadFactory.createDownload(
					trackProvider.getTrack(downloadId), true);
			currentDownloads.put(downloadId, download);
			downloadsExecutor.execute(download);
		} else {
			lanDownload.start();
		}
	}

	public DownloadStatus getStatus(String downloadId) throws DownloadException {
		LanDownload download = currentDownloads.get(downloadId);
		return download != null ? download.getStatus() : null;
	}

	@Override
	public void pause(String downloadId) throws DownloadException {
		LanDownload download = currentDownloads.get(downloadId);
		if (download != null) {
			download.pause();
		}
	}

	@Override
	public void resume(String downloadId) throws DownloadException {
		LanDownload download = currentDownloads.get(downloadId);
		if (download != null) {
			download.resume();
		}
	}

	@Override
	public void onMessage(LanDownloaderMessage message) {
		log.debug("Received " + message.getType() + " from "
				+ message.getSourceAddress());
		switch (message.getType()) {
		case TRACK_REQUEST:
			respondTrackRequest(message.getDownloadId(),
					message.getSourceAddress());
			break;
		case TRACK_RESPONSE:
			addSeeder(message.getDownloadId(), message.getSourceAddress(),
					Boolean.valueOf(message.getBody()));
			break;
		case CHUNK_TRANSFER:
			addChunk(message.getDownloadId(), message.getBody());
			break;
		case START_TRANSFER:
			startTransfer(message.getDownloadId(), message.getSourceAddress());
			break;
		case PAUSE_TRANSFER:
			pauseTransfer(message.getDownloadId(), message.getSourceAddress());
			break;
		case RESUME_TRANSFER:
			resumeTransfer(message.getDownloadId(), message.getSourceAddress());
			break;
		case CANCEL_TRANSFER:
			cancelTransfer(message.getDownloadId(), message.getSourceAddress());
			break;
		}
	}

	private void cancelTransfer(String downloadId, String destination) {
		LanTransfer transfer = currentTransfers.get(createTransferKey(
				downloadId, destination));
		if (transfer != null) {
			transfer.cancel();
		}
	}

	private void resumeTransfer(String downloadId, String destination) {
		LanTransfer transfer = currentTransfers.get(createTransferKey(
				downloadId, destination));
		if (transfer != null) {
			transfer.resume();
		}
	}

	private void pauseTransfer(String downloadId, String destination) {
		LanTransfer transfer = currentTransfers.get(createTransferKey(
				downloadId, destination));
		if (transfer != null) {
			transfer.pause();
		}
	}

	private void startTransfer(String downloadId, String destination) {
		LanTransfer transfer = transferFactory.createTransfer(downloadId,
				trackProvider.getFile(downloadId), destination);
		currentTransfers.put(createTransferKey(downloadId, destination),
				transfer);
		transfersExecutor.execute(transfer);
	}

	private void addChunk(String downloadId, String encodedChunk) {
		LanDownload download = currentDownloads.get(downloadId);
		if (download != null) {
			download.addChunk(encodedChunk);
		}
	}

	private void addSeeder(String downloadId, String seederAddr,
			boolean hasTrack) {
		LanDownload download = currentDownloads.get(downloadId);
		if (download != null) {
			download.addSeederResponse(seederAddr, hasTrack);
		}
	}

	private void respondTrackRequest(String downloadId, String destination) {
		LanDownloaderMessage responseMessage = new LanDownloaderMessage(
				getLocalAddress(), TRACK_RESPONSE, downloadId);
		boolean response = trackProvider.getFile(downloadId) != null;
		responseMessage.setBody(Boolean.toString(response));
		log.debug("Responding " + responseMessage.getBody() + " to "
				+ destination + " about " + downloadId);
		networkingService.sendTo(responseMessage, destination);
	}

	private String createTransferKey(String downloadId, String sourceAddress) {
		return downloadId + ":" + sourceAddress;
	}

	class LanTransfer implements Runnable {

		private static final long PAUSE_DELAY = 1000;

		private final String downloadId;

		private FileInputStream fileReader;

		private int fileSize;

		private final String destination;

		private byte[] currentChunk;

		private int chunkSize;

		private int lastChunkSize;

		private int readChunks;

		private AtomicBoolean paused = new AtomicBoolean(false);

		private AtomicBoolean canceled = new AtomicBoolean(false);

		private final File file;

		public LanTransfer(String trackId, File file, String destination) {
			this.downloadId = trackId;
			this.file = file;
			this.destination = destination;
		}

		public void resume() {
			paused.set(false);
		}

		public void cancel() {
			canceled.set(true);
		}

		public void pause() {
			paused.set(true);
		}

		@Override
		public void run() {
			try {
				transfer(file);
			} finally {
				currentTransfers.remove(createTransferKey(downloadId,
						destination));
			}
		}

		private void transfer(File transferredFile) {
			try {
				initFileReader(transferredFile);
				while (readChunk()) {
					if (canceled.get()) {
						return;
					}
					while (paused.get()) {
						Thread.sleep(PAUSE_DELAY);
					}
					transferChunk();
				}
				log.info("Track " + downloadId
						+ " was succesfully transferred.");
			} catch (Exception e) {
				log.error("Unexpected exception during file transfer of "
						+ downloadId, e);
			} finally {
				closeFileReader();
			}
		}

		private void closeFileReader() {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					log.error("Could not close file reader during transfer of "
							+ downloadId, e);
				}
			}
			fileReader = null;
		}

		private void initFileReader(File transferredFile)
				throws FileNotFoundException, IOException {
			fileReader = new FileInputStream(transferredFile);
			fileSize = fileReader.available();
			chunkSize = (fileSize / TOTAL_CHUNKS)
					+ (fileSize % TOTAL_CHUNKS == 0 ? 0 : 1);
			lastChunkSize = fileSize - chunkSize * (TOTAL_CHUNKS - 1);
		}

		private boolean readChunk() throws IOException {
			if (readChunks <= TOTAL_CHUNKS) {
				currentChunk = new byte[readChunks < (TOTAL_CHUNKS - 1) ? chunkSize
						: lastChunkSize];
				fileReader.read(currentChunk);
				readChunks++;
			}
			return readChunks <= TOTAL_CHUNKS;
		}

		private void transferChunk() {
			LanDownloaderMessage chunkMessage = new LanDownloaderMessage(
					getLocalAddress(), CHUNK_TRANSFER, downloadId);
			chunkMessage.setBody(new String(Base64.encode(currentChunk)));
			log.debug("Sending chunk " + readChunks + "/" + TOTAL_CHUNKS
					+ " to " + destination);
			networkingService.sendTo(chunkMessage, destination);
		}

	}

	class LanDownload implements Runnable {

		private static final long PAUSE_DELAY = 1000;

		private static final long CHUNK_AWAIT_TIMEOUT = 30 * 1000;

		private AtomicBoolean paused = new AtomicBoolean(false);

		private AtomicBoolean canceled = new AtomicBoolean(false);

		private AtomicBoolean downloading = new AtomicBoolean(false);

		private AtomicBoolean started = new AtomicBoolean(false);

		private final String downloadId;

		private final Track track;

		private final DownloadStatusImpl status;

		private final BlockingQueue<String> seeders = new LinkedBlockingQueue<String>();

		private final BlockingQueue<byte[]> chunks = new LinkedBlockingQueue<byte[]>();

		private FileOutputStream fileWriter;

		private long startTime;

		private int currentChunk;

		private String currentSeeder;

		private AtomicInteger seederResponses = new AtomicInteger(0);

		private AtomicBoolean delayNextRequest = new AtomicBoolean(false);

		private String incompletePath = null;

		private String completePath = null;

		private final int DOWNLOAD_TIME_PRECISION = 1024;

		public LanDownload(Track track, boolean autoStart) {
			this.track = track;
			this.downloadId = track.getHashcode();
			this.status = new DownloadStatusImpl(downloadId);
			this.started.set(autoStart);
		}

		public void resume() {
			paused.set(false);
			if (isDownloading()) {
				status.setState(DownloadState.Downloading);
				LanDownloaderMessage message = new LanDownloaderMessage(
						getLocalAddress(), RESUME_TRANSFER, downloadId);
				networkingService.sendTo(message, currentSeeder);
			} else {
				status.setState(currentSeeder == null ? DownloadState.Searching
						: DownloadState.ReadyToDownload);
			}
			notifyDownloadUpdated(new DownloadUpdateEvent(LanDownloader.this,
					downloadId, status));
			log.info("LAN Download " + downloadId + " has been resumed.");
		}

		public void pause() {
			log.info("Pausing LAN download for " + downloadId);
			paused.set(true);
		}

		public DownloadStatus getStatus() {
			return status;
		}

		public void findSources() throws InterruptedException {
			log.info("Finding LAN sources for " + downloadId);
			status.setState(DownloadState.Searching);
			while (currentSeeder == null && !canceled.get()) {
				verifyCurrentState();
				awaitForSeeder(sendTrackRequest());
				updateSearchStatus();
			}
			log.info("Found LAN seeder " + currentSeeder + " for " + downloadId);
		}

		private int sendTrackRequest() throws InterruptedException {
			if (delayNextRequest.get()) {
				Thread.sleep(5000);
				delayNextRequest.set(false);
			}
			LanDownloaderMessage message = new LanDownloaderMessage(
					getLocalAddress(), TRACK_REQUEST, downloadId);
			// THIS BODY IS USED AS BACKWARD COMPATIBILITY TO AVOID NEGATIVE
			// RESPONSES
			// TO NODES WITH PREVIOUS IMPL
			message.setBody(Boolean.toString(true));
			return networkingService.send(message);
		}

		private void updateSearchStatus() {
			DownloadState previousState = status.getState();
			DownloadState currentState = currentSeeder == null ? DownloadState.MoreSourcesNeeded
					: DownloadState.ReadyToDownload;
			if (currentState != previousState) {
				log.info("Notifying state " + currentState + " for "
						+ downloadId);
				status.setState(currentState);
				notifySearchSourcesResult(new SearchSourcesEvent(
						LanDownloader.this, downloadId, status));
			}
		}

		private void awaitForSeeder(int expectedResponses)
				throws InterruptedException {
			long awaitTime = 0;
			int sleepTime = 100;
			log.debug("Waiting for " + expectedResponses + " nodes to respond.");
			while (seeders.peek() == null
					&& seederResponses.get() < expectedResponses
					&& awaitTime < getSearchTimeout() && !canceled.get()) {
				awaitTime += sleepTime;
				Thread.sleep(sleepTime);
			}
			log.debug(seederResponses.get() + "  responded to request.");
			currentSeeder = seeders.poll();
			if (seederResponses.get() >= expectedResponses) {
				delayNextRequest.set(true);
			}
			seederResponses.set(0);
		}

		private void verifyCurrentState() throws InterruptedException {
			cancelIfNeccessary();
			pauseIfNeccessary();
		}

		private void pauseIfNeccessary() throws InterruptedException {
			if (paused.get()) {
				log.info("Download " + downloadId + " has been paused.");
				status.setState(DownloadState.Paused);
				if (isDownloading()) {
					log.debug("Sending pause transfer message to "
							+ currentSeeder);
					LanDownloaderMessage message = new LanDownloaderMessage(
							getLocalAddress(), PAUSE_TRANSFER, downloadId);
					networkingService.sendTo(message, currentSeeder);
				}
				notifyDownloadUpdated(new DownloadUpdateEvent(
						LanDownloader.this, downloadId, status));
				while (paused.get() && !canceled.get()) {
					Thread.sleep(PAUSE_DELAY);
				}
			}
		}

		private void cancelIfNeccessary() throws InterruptedException {
			if (canceled.get()) {
				log.info("Download " + downloadId + " has been canceled.");
				status.setState(DownloadState.Canceled);
				if (isDownloading()) {
					log.debug("Sending cancel transfer message to "
							+ currentSeeder);
					LanDownloaderMessage message = new LanDownloaderMessage(
							getLocalAddress(), CANCEL_TRANSFER, downloadId);
					networkingService.sendTo(message, currentSeeder);
				}
				notifyDownloadUpdated(new DownloadUpdateEvent(
						LanDownloader.this, downloadId, status));
				throw new InterruptedException("LAN Download for " + downloadId
						+ " was canceled.");
			}
		}

		@Override
		public void run() {
			try {
				findSources();
				awaitToStart();
				download();
			} catch (InterruptedException e) {
				status.setState(DownloadState.Canceled);
				notifyDownloadUpdated(new DownloadUpdateEvent(
						LanDownloader.this, downloadId, status));
			} catch (Exception e) {
				log.error("Unexpected exception during download.", e);
				status.setState(DownloadState.Error);
				notifyDownloadUpdated(new DownloadUpdateEvent(
						LanDownloader.this, downloadId, status));
			} finally {
				currentDownloads.remove(downloadId);
				try {
					removeIncompleteFile();
				} catch (IOException e) {
					log.error(e, e);
				}
			}
		}

		private void awaitToStart() throws InterruptedException {
			while (!started.get()) {
				verifyCurrentState();
				Thread.sleep(100);
			}
			status.setState(DownloadState.Downloading);
		}

		public void start() {
			started.set(true);
		}

		private void download() throws InterruptedException, IOException,
				TimeoutException {
			status.setState(DownloadState.Downloading);
			sendStartRequest();
			log.debug("Waiting for " + currentSeeder + " to start transfer.");
			startTime = System.currentTimeMillis();
			while (status.getProgress() < 100) {
				long downloadStartTime = System.nanoTime();
				byte[] chunk = nextChunk();
				double downloadTotalTime = (System.nanoTime() - downloadStartTime) / 1000000.0;
				currentChunk++;
				writeChunk(chunk);
				updateStatus(chunk, downloadTotalTime);
				notifyDownloadUpdated(new DownloadUpdateEvent(
						LanDownloader.this, downloadId, status));
			}
			notifyDownloadCompleted();
		}

		private void sendStartRequest() {
			LanDownloaderMessage message = new LanDownloaderMessage(
					getLocalAddress(), START_TRANSFER, downloadId);
			networkingService.sendTo(message, currentSeeder);
			downloading.set(true);
		}

		private byte[] nextChunk() throws InterruptedException,
				TimeoutException {
			byte[] chunk = null;
			long awaitTime = 0;
			while (chunk == null) {
				awaitTime += 100;
				chunk = chunks.poll(100, TimeUnit.MILLISECONDS);
				verifyCurrentState();
				if (awaitTime >= CHUNK_AWAIT_TIMEOUT) {
					throw new TimeoutException(
							"Exceed timeout waiting for chunk from "
									+ currentSeeder);
				}
			}

			return chunk;
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

		private void updateStatus(byte[] chunk, double downloadTotalTime) {
			status.setProgress(currentChunk);
			status.setFreeNodes(1);
			long totalMillis = System.currentTimeMillis() - startTime;
			if (totalMillis > 0) {
				long remainingSeconds = (TOTAL_CHUNKS - currentChunk)
						* totalMillis / currentChunk;
				status.setRemainingSeconds((int) (remainingSeconds / 1000.0));
				status.setDownloadRate((long) ((chunk.length * DOWNLOAD_TIME_PRECISION) / downloadTotalTime));
			}
		}

		private void notifyDownloadCompleted() throws IOException {
			log.info("Track " + downloadId + " was succesfully downloaded.");
			File completedFile = moveDownloadedFile();
			if (completedFile != null) {
				status.setState(DownloadState.Complete);
				DownloadCompleteEvent completeEvent = new DownloadCompleteEvent(
						LanDownloader.this, downloadId, completedFile);
				LanDownloader.this.notifyDownloadCompleted(completeEvent);
				AllMessage<Integer> userActionMessage = new AllMessage<Integer>(
						UserActions.USER_ACTION_MESSAGE_TYPE,
						UserActions.Downloads.LAN_DOWNLOAD);
				userActionMessage.putProperty(CommonManagedDownloader.TRACK_ID,
						downloadId);
				messEngine.send(userActionMessage);
			}
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
					log.error(
							"Could not move incomplete file to final destination.",
							e);
				}
			}
			return null;
		}

		private String getIncompleteFilePath() throws IOException {
			if (incompletePath == null) {
				incompletePath = DownloaderUtils
						.getValidFilePath(
								downloaderConfig.getIncompleteDownloadsPath(),
								DownloaderUtils.getValidFileName(
										"LAN" + track.getName(),
										track.getFileFormat()));
			}
			return incompletePath;
		}

		private String getCompleteFilePath() throws IOException {
			if (completePath == null) {
				completePath = DownloaderUtils.getValidFilePath(
						downloaderConfig.getCompleteDownloadsPath(),
						DownloaderUtils.getValidFileName(track.getName(),
								track.getFileFormat()));
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

		private boolean isDownloading() {
			return downloading.get();
		}

		public void addSeederResponse(String seeder, boolean hasTrack) {
			seederResponses.incrementAndGet();
			if (hasTrack) {
				seeders.offer(seeder);
			}
		}

		public void addChunk(String encodedChunk) {
			chunks.offer(Base64.decode(encodedChunk.getBytes()));
		}

		public void cancel() {
			canceled.set(true);
		}
	}

	public boolean isTransferring() {
		return !currentTransfers.isEmpty();
	}

	class LanDownloadFactory {
		public LanDownload createDownload(Track track, boolean autoStart) {
			return new LanDownload(track, autoStart);
		}
	}

	class LanTransferFactory {
		public LanTransfer createTransfer(String trackId, File file,
				String destination) {
			return new LanTransfer(trackId, file, destination);
		}
	}

}
