package com.all.rest.mc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.all.mc.manager.McManager;
import com.all.mc.manager.uploads.UploadStatus;
import com.all.mc.manager.uploads.UploadStatus.UploadState;
import com.all.mc.manager.uploads.UploaderListener;
import com.all.messengine.MessEngine;
import com.all.messengine.MessageListener;
import com.all.rest.beans.RestUploadStatus;
import com.all.rest.config.RestClientConfig;
import com.all.rest.web.RestService;
import com.all.shared.download.RestUploadRequest;
import com.all.shared.download.TrackProvider;
import com.all.shared.mc.TrackStatus;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.AllMessage;
import com.all.shared.model.Track;
import com.all.shared.stats.usage.UserActions;

@Service
public class RestMediaManager implements McManager {

	private static final int MAX_CONCURRENT_UPLOADS = 5;

	private final Log log = LogFactory.getLog(this.getClass());

	private final ExecutorService uploadsExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_UPLOADS);

	private final Notifier notifier = new Notifier();

	private final Map<String, RestUpload> currentUploads = new HashMap<String, RestUpload>();

	private AtomicInteger uploadRate;
	@Autowired
	private RestService restService;
	@Autowired
	private MessEngine messEngine;
	@Autowired
	private TrackProvider trackProvider;
	@Autowired
	private RestClientConfig restConfig;

	private final Collection<UploaderListener> listeners = Collections.synchronizedSet(new HashSet<UploaderListener>());

	@PostConstruct
	public void init() {
		messEngine.addMessageListener(MessEngineConstants.REST_UPLOAD_TRACK_REQUEST_TYPE,
				new MessageListener<AllMessage<RestUploadRequest>>() {
					@Override
					public void onMessage(AllMessage<RestUploadRequest> message) {
						String currentUser = restConfig.getUserId();
						RestUploadRequest request = message.getBody();
						if (currentUser == null || currentUser.equals(request.getRequester())) {
							// ignoring auto requests.
							return;
						}
						upload(request.getTrackId());

					}
				});
		Executors.newSingleThreadExecutor().execute(notifier);
	}

	@PreDestroy
	public void shutdown() {
		Collection<RestUpload> uploads = currentUploads.values();
		for (RestUpload restUpload : uploads) {
			restUpload.cancel();
		}
	}

	@Override
	public void addUploaderListener(UploaderListener listener) {
		listeners.add(listener);
	}

	@Override
	public void cancelUpload(String trackId) {
		if (isUploading(trackId)) {
			currentUploads.get(trackId).cancel();
		}
	}

	@Override
	public void upload(String trackId) {
		if (!isUploading(trackId)) {
			RestUpload upload = new RestUpload(trackProvider.getTrack(trackId), trackProvider.getFile(trackId));
			currentUploads.put(trackId, upload);
			uploadsExecutor.execute(upload);
		}
	}

	@Override
	public void removeUploaderListener(UploaderListener listener) {
		listeners.remove(listener);
	}

	@Override
	public synchronized int getUploadRate() {
		if (uploadRate == null) {
			uploadRate = new AtomicInteger(0);
			try {
				uploadRate.set(restService.getUploadRate());
			} catch (Exception e) {
				log.error("Could not determine upload rate.", e);
			}
		}
		return uploadRate.get();
	}

	private void updateUploaderRate(int currentRate) {
		if (uploadRate == null) {
			uploadRate = new AtomicInteger(currentRate);
		} else {
			uploadRate.set(currentRate);
		}
	}

	public boolean isUploading(String trackId) {
		return currentUploads.containsKey(trackId);
	}

	class RestUpload implements Runnable {

		private static final int TOTAL_CHUNKS = 100;

		private final RestUploadStatus uploadStatus;

		private final AtomicBoolean canceled = new AtomicBoolean(false);

		private final String trackId;

		private TrackStatus trackStatus;

		private int fromChunk;

		private FileInputStream fileReader = null;

		private int fileSize = 0;

		private final Track track;

		private final File file;

		public RestUpload(Track track, File file) {
			this.track = track;
			this.trackId = track.getHashcode();
			this.uploadStatus = new RestUploadStatus(trackId);
			this.file = file;
		}

		public void cancel() {
			canceled.set(true);
		}

		@Override
		public void run() {
			try {
				upload();
			} catch (Exception e) {
				notifyError(e);
			} finally {
				dispose();
			}
		}

		private void dispose() {
			currentUploads.remove(trackId);
			if (canceled.get()) {
				try {
					restService.cancelUpload(trackId);
				} catch (Exception e) {
					log.error("Unexpected error cancelling upload.", e);
				}
			}
			closeFileReader();
		}

		private void notifyError(Exception e) {
			log.error("Unexpected error uploading " + trackId, e);
			uploadStatus.setState(UploadStatus.UploadState.ERROR);
			notifier.notifyStatus(uploadStatus);
		}

		private void upload() throws IOException {
			if (!file.exists()) {
				throw new IllegalArgumentException("Could not find file for " + trackId);
			}
			upload(file);
			log.info("Track " + trackId + " was succesfully uploaded.");
		}

		private void upload(File uploadedFile) throws IOException {
			initTrackStatus();
			verifyTrackStatus();
			initFileReader(uploadedFile);
			for (int currentChunk = 0; currentChunk < TOTAL_CHUNKS; currentChunk++) {
				if (canceled.get()) {
					uploadStatus.setState(UploadState.CANCELED);
					notifier.notifyStatus(uploadStatus);
					log.info("Upload for " + trackId + " was cancelled at chunk " + currentChunk);
					break;
				}
				uploadChunk(currentChunk, readChunk(currentChunk));
			}
			uploadStatus.setState(UploadState.COMPLETED);
			notifier.notifyStatus(uploadStatus);
			if (checkIfUploadWasComplete()) {
				uploadTrackMetadata();
			}
		}

		private boolean checkIfUploadWasComplete() {
			return !canceled.get() && fromChunk != TOTAL_CHUNKS;
		}

		private void initTrackStatus() {
			trackStatus = restService.getStatus(trackId);
			log.info("Found track " + trackStatus.getTrackId() + " with status " + trackStatus.getTrackStatus()
					+ ". Will start upload from chunk " + trackStatus.getLastChunkNumber());
		}

		private void uploadTrackMetadata() {
			if (track == null) {
				throw new IllegalStateException("Cannot upload null track.");
			}
			restService.uploadMetadata(track);
		}

		private void verifyTrackStatus() {
			if (trackStatus == null) {
				throw new IllegalStateException("Track status cannot be null.");
			}
			switch (trackStatus.getTrackStatus()) {
			case NOT_AVAILABLE:
				fromChunk = 0;
				break;
			case INCOMPLETE:
				fromChunk = trackStatus.getLastChunkNumber();
				break;
			case UPLOADED:
				fromChunk = TOTAL_CHUNKS;
				break;
			case UPLOADING:
				// TODO: Check with BAs what to do if someone else is uploading this
				// file.
				fromChunk = TOTAL_CHUNKS;
			}
		}

		private void closeFileReader() {
			if (fileReader != null) {
				try {
					fileReader.close();
				} catch (IOException e) {
					log.error("Could not close file reader during upload " + trackId, e);
				}
			}
			fileReader = null;
		}

		private void initFileReader(File uploadedFile) throws IOException {
			fileReader = new FileInputStream(uploadedFile);
			fileSize = fileReader.available();
		}

		private void uploadChunk(int currentChunk, byte[] chunkToUpload) {
			if (currentChunk >= fromChunk) {
				long startTime = System.currentTimeMillis();
				restService.uploadChunk(trackId, currentChunk, chunkToUpload);
				long transferTime = System.currentTimeMillis() - startTime;
				int progress = currentChunk + 1;
				uploadStatus.setProgress(progress);
				uploadStatus.setState(UploadState.UPLOADING);
				int currentRate = (int) (chunkToUpload.length * 1000.0 / transferTime);
				uploadStatus.setUploadRate(currentRate);
				updateUploaderRate(currentRate);
				notifier.notifyStatus(uploadStatus);
				log.debug("Chunk " + currentChunk + " succesfully uploaded");
			}
		}

		private int calculateChunkSize(int chunkNumber) {
			int chunkSize = (fileSize / TOTAL_CHUNKS) + (fileSize % TOTAL_CHUNKS == 0 ? 0 : 1);
			int lastChunk = TOTAL_CHUNKS - 1;
			if (chunkNumber == lastChunk) {
				chunkSize = fileSize - chunkSize * lastChunk;
			}
			return chunkSize;
		}

		private byte[] readChunk(int chunkNumber) throws IOException {
			int chunkSize = calculateChunkSize(chunkNumber);
			byte[] chunk = new byte[chunkSize];
			fileReader.read(chunk);
			return chunk;
		}
	}

	private final class Notifier implements Runnable {

		private final BlockingQueue<UploadStatus> eventQueue = new LinkedBlockingQueue<UploadStatus>();

		@Override
		public void run() {
			try {
				while (true) {
					UploadStatus uploadStatus = eventQueue.take();
					for (UploaderListener listener : listeners) {
						try {
							listener.onUploadUpdated(uploadStatus);
						} catch (Exception e) {
							log.error("Unexpected error during Uploader Listener execution.", e);
						}
					}
					if (uploadStatus.getState() == UploadState.COMPLETED) {
						messEngine.send(new AllMessage<Integer>(UserActions.USER_ACTION_MESSAGE_TYPE,
								UserActions.Downloads.MC_UPLOAD));
					}
				}
			} catch (Exception e) {
				log.error("Unexpected error on Uploader notifier.", e);
			}
		}

		public void notifyStatus(UploadStatus uploadStatus) {
			eventQueue.offer(uploadStatus);
		}
	}

	@Override
	public List<String> getAvailableTracks(List<String> trackIds) {
		return restService.findTracksById(trackIds);
	}

}
