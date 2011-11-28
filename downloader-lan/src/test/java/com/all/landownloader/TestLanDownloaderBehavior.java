package com.all.landownloader;

import static com.all.landownloader.LanDownloadMessageType.CANCEL_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.CHUNK_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.START_TRANSFER;
import static com.all.landownloader.LanDownloadMessageType.TRACK_REQUEST;
import static com.all.landownloader.LanDownloadMessageType.TRACK_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;
import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.DownloaderListener;
import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.downloader.download.ManagedDownloaderListener;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.messengine.MessEngine;
import com.all.shared.download.TrackProvider;
import com.all.shared.model.Track;
import com.all.testing.MockInyectRunner;
import com.all.testing.Stub;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestLanDownloaderBehavior {
	private static final String SEPARATOR = System.getProperty("file.separator");
	private static final String INCOMPLETE_DIR = "Incomplete";
	private static final String DOWNLOADS_DIR = "Downloads";
	@UnderTest
	private LanDownloader lanDownloader;
	@Mock
	private ManagedDownloaderConfig config;
	@Mock
	private TrackProvider trackProvider;
	@SuppressWarnings("unused")
	@Mock
	private MessEngine messEngine;
	
	private LanNetworkingStub networkingStub = new LanNetworkingStub();
	@SuppressWarnings("unused")
	// injected
	@Stub
	private LanNetworkingService networkingService = networkingStub;

	private String email = "user@all.com";
	@Mock
	private DownloaderListener listener;
	@Mock
	private Properties downloaderProperties;

	private AtomicBoolean requestSent = new AtomicBoolean(false);
	private String downloadId = "1234567890123456789012345678901234567890";
	private String seederAddr = "192.168.1.27";
	private String encodedChunk = "encodedChunk";
	private String trackName = "temp";
	private String extension = "mp3";
	private String prefix = "LAN";
	private String completeFileName = trackName + "." + extension;
	private String incompleteFileName = prefix + completeFileName;
	private Integer searchTimeout = 1; // in seconds
	@Mock
	private Track track;

	@BeforeClass
	public static void createDirs() {
		File incompleteDir = new File(INCOMPLETE_DIR);
		if (!incompleteDir.exists()) {
			incompleteDir.mkdir();
		}
		File downloadsDir = new File(DOWNLOADS_DIR);
		if (!downloadsDir.exists()) {
			downloadsDir.mkdir();
		}
	}

	@Before
	public void setup() throws InterruptedException {
		when(config.getUserId()).thenReturn(email);
		when(config.getIncompleteDownloadsPath()).thenReturn(INCOMPLETE_DIR);
		when(config.getCompleteDownloadsPath()).thenReturn(DOWNLOADS_DIR);

		when(trackProvider.getTrack(downloadId)).thenReturn(track);
		when(track.getHashcode()).thenReturn(downloadId);
		when(track.getName()).thenReturn(trackName);
		when(track.getFileFormat()).thenReturn(extension);
		deleteFiles();

		lanDownloader.addDownloaderListener(listener);
	}

	@After
	public void teardown() throws InterruptedException {
		deleteFiles();
		Thread.sleep(1000);
	}

	@Test
	public void shouldNotifyReadyToDownloadIfSourcesFound() throws Exception {
		ManagedDownloaderListener listener = mock(ManagedDownloaderListener.class);
		ArgumentCaptor<SearchSourcesEvent> eventCaptor = ArgumentCaptor.forClass(SearchSourcesEvent.class);
		lanDownloader.addManagedDownloadListener(downloadId, listener);
		lanDownloader.findSources(downloadId);

		verify(listener, timeout(1000)).onSearchSources(eventCaptor.capture());
		SearchSourcesEvent event = eventCaptor.getValue();
		assertEquals(downloadId, event.getDownloadId());
		assertEquals(lanDownloader, event.getSource());
		assertEquals(lanDownloader, event.getDownloader());
		assertEquals(DownloadState.ReadyToDownload, event.getDownloadStatus().getState());
	}

	@Test(timeout = 5000)
	public void shouldNotifyMoreSourcesNeededIfNoSeeders() throws Exception {
		ManagedDownloaderListener listener = mock(ManagedDownloaderListener.class);
		ArgumentCaptor<SearchSourcesEvent> eventCaptor = ArgumentCaptor.forClass(SearchSourcesEvent.class);
		networkingStub.setAutoResponse(false);
		when(downloaderProperties.getProperty(LanDownloader.TIMEOUT_KEY)).thenReturn(searchTimeout.toString());
		lanDownloader.addManagedDownloadListener(downloadId, listener);

		lanDownloader.findSources(downloadId);
		while (!requestSent.get()) {
			Thread.sleep(100);
		}
		sendTrackResponse(false);
		verify(listener, timeout(1000)).onSearchSources(eventCaptor.capture());
		SearchSourcesEvent event = eventCaptor.getValue();
		assertEquals(downloadId, event.getDownloadId());
		assertEquals(lanDownloader, event.getSource());
		assertEquals(lanDownloader, event.getDownloader());
		assertEquals(DownloadState.MoreSourcesNeeded, event.getDownloadStatus().getState());
	}

	@Test(timeout = 5000)
	public void shouldNotifyMoreSourcesNeededIfNoSourcesAfterSearchTimeout() throws Exception {
		ManagedDownloaderListener listener = mock(ManagedDownloaderListener.class);
		ArgumentCaptor<SearchSourcesEvent> eventCaptor = ArgumentCaptor.forClass(SearchSourcesEvent.class);
		networkingStub.setAutoResponse(false);
		when(downloaderProperties.getProperty(LanDownloader.TIMEOUT_KEY)).thenReturn(searchTimeout.toString());
		lanDownloader.addManagedDownloadListener(downloadId, listener);

		lanDownloader.findSources(downloadId);

		verify(listener, timeout((searchTimeout * 1000) + 1000)).onSearchSources(eventCaptor.capture());
		SearchSourcesEvent event = eventCaptor.getValue();
		assertEquals(downloadId, event.getDownloadId());
		assertEquals(lanDownloader, event.getSource());
		assertEquals(lanDownloader, event.getDownloader());
		assertEquals(DownloadState.MoreSourcesNeeded, event.getDownloadStatus().getState());

	}

	@Test
	public void shouldFindSourcesAndThenDownload() throws Exception {

	}

	@Test(timeout = 20000)
	public void shouldDownloadDirectly() throws Exception {
		lanDownloader.download(downloadId);
		awaitForDownloadCompleted(lanDownloader.getStatus(downloadId));
		assertTrue(networkingStub.isMessageSent(TRACK_REQUEST));
		assertTrue(networkingStub.isMessageSent(START_TRANSFER));
		verify(listener, atLeast(99)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertDownloadedFileExists();
	}

	@Test(timeout = 20000)
	public void shouldCancelADownload() throws Exception {
		int expectedProgress = 30;
		networkingStub.pauseAfter(expectedProgress);
		lanDownloader.download(downloadId);
		verify(listener, timeout(1000).atLeast(expectedProgress)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		lanDownloader.delete(downloadId);
		Thread.sleep(500);
		verify(listener, never()).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertTrue(networkingStub.isMessageSent(TRACK_REQUEST));
		assertTrue(networkingStub.isMessageSent(START_TRANSFER));
		assertTrue(networkingStub.isMessageSent(CANCEL_TRANSFER));
		assertFalse(new File(INCOMPLETE_DIR + SEPARATOR + incompleteFileName).exists());
	}

	@Test(timeout = 20000)
	public void shouldPauseAndResumeADownload() throws Exception {
		networkingStub.pauseAfter(30, 60);
		lanDownloader.download(downloadId);
		DownloadStatus status = lanDownloader.getStatus(downloadId);

		verify(listener, timeout(500).atLeast(30)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		lanDownloader.pause(downloadId);
		assertTrue(status.getDownloadRate() > 0);
		verify(listener, never()).onDownloadCompleted(any(DownloadCompleteEvent.class));
		Thread.sleep(500);

		assertEquals(DownloadState.Paused, lanDownloader.getStatus(downloadId).getState());

		lanDownloader.resume(downloadId);
		networkingStub.resume();
		verify(listener, timeout(1000).atLeast(60)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		lanDownloader.pause(downloadId);
		Thread.sleep(200);
		verify(listener, never()).onDownloadCompleted(any(DownloadCompleteEvent.class));
		lanDownloader.resume(downloadId);
		networkingStub.resume();

		awaitForDownloadCompleted(status);
		verify(listener, atLeast(99)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertDownloadedFileExists();
		assertEquals(100, status.getProgress());
		assertEquals(0, status.getRemainingSeconds());
	}

	private void awaitForDownloadCompleted(DownloadStatus status) throws Exception {
		verify(listener, timeout(5000)).onDownloadCompleted(any(DownloadCompleteEvent.class));
		Thread.sleep(500);
	}

	@Test(timeout = 20000)
	public void shouldRemoveDownloaderListener() throws Exception {
		networkingStub.pauseAfter(50);
		lanDownloader.download(downloadId);
		DownloadStatus status = lanDownloader.getStatus(downloadId);
		verify(listener, timeout(500).atMost(50)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		lanDownloader.removeDownloaderListener(listener);
		networkingStub.resume();
		while (status.getState() != DownloadState.Complete) {
			Thread.sleep(100);
		}
		verify(listener, never()).onDownloadCompleted(any(DownloadCompleteEvent.class));
		Thread.sleep(500);
		assertDownloadedFileExists();
	}

	@Test(timeout = 20000)
	public void shouldHandleDownloaderListenerExceptions() throws Exception {
		RuntimeException listenerException = new RuntimeException("SomeListenerException");
		doThrow(listenerException).when(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		doThrow(listenerException).when(listener).onDownloadUpdated(any(DownloadUpdateEvent.class));

		lanDownloader.download(downloadId);

		awaitForDownloadCompleted(lanDownloader.getStatus(downloadId));
		verify(listener, atLeast(99)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertDownloadedFileExists();
	}

	private void sendTrackResponse(boolean hasTrack) {
		LanDownloaderMessage response = new LanDownloaderMessage(seederAddr, TRACK_RESPONSE, downloadId);
		response.setBody(Boolean.toString(hasTrack));
		lanDownloader.onMessage(response);
	}

	private void deleteFiles() throws InterruptedException {
		File incompleteFile = new File(INCOMPLETE_DIR + SEPARATOR + incompleteFileName);
		while (incompleteFile.exists()) {
			incompleteFile.delete();
			Thread.sleep(100);
		}
		File downloadedFile = new File(DOWNLOADS_DIR + SEPARATOR + completeFileName);
		while (downloadedFile.exists()) {
			downloadedFile.delete();
			Thread.sleep(100);
		}
	}

	private void assertDownloadedFileExists() {
		String pathname = DOWNLOADS_DIR + SEPARATOR + completeFileName;
		assertTrue(new File(pathname).exists());
		assertFalse(new File(INCOMPLETE_DIR + SEPARATOR + incompleteFileName).exists());
	}

	class LanNetworkingStub extends LanNetworkingService {
		private Collection<LanDownloadMessageType> sentMessages = new ArrayList<LanDownloadMessageType>();

		private Collection<Integer> pauses = new ArrayList<Integer>();

		private int chunkCounter = 0;

		private boolean autoResponse = true;

		public void setAutoResponse(boolean autoResponse) {
			this.autoResponse = autoResponse;
		}

		@Override
		public int send(LanDownloaderMessage message) {
			sentMessages.add(message.getType());
			if (TRACK_REQUEST.equals(message.getType())) {
				requestSent.set(true);
				if (autoResponse) {
					sendTrackResponse(true);
				}
			} else {
				throw new IllegalArgumentException("Only track requests should be sent to all users in LAN.");
			}
			return 1;
		}

		public void pauseAfter(Integer... programmedPauses) {
			for (Integer pause : programmedPauses) {
				pauses.add(pause);
			}
		}

		public void resume() {
			new SendChunks().start();
		}

		@Override
		public boolean sendTo(LanDownloaderMessage message, String receiver) {
			sentMessages.add(message.getType());
			switch (message.getType()) {
			case START_TRANSFER:
				new SendChunks().start();
				break;

			default:
				break;
			}
			return true;
		}

		class SendChunks extends Thread {
			@Override
			public void run() {
				while (chunkCounter < 100) {
					LanDownloaderMessage chunkMessage = new LanDownloaderMessage(seederAddr, CHUNK_TRANSFER, downloadId);
					chunkMessage.setBody(encodedChunk);
					lanDownloader.onMessage(chunkMessage);
					chunkCounter++;
					if (pauses.contains(chunkCounter)) {
						break;
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
				}
			}
		}

		public boolean isMessageSent(LanDownloadMessageType type) {
			return sentMessages.contains(type);
		}

	}
}
