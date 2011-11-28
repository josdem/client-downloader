package com.all.rest.downloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;
import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.DownloaderListener;
import com.all.downloader.download.ManagedDownloaderListener;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.messengine.MessEngine;
import com.all.rest.config.RestClientConfig;
import com.all.rest.web.RestService;
import com.all.shared.download.TrackProvider;
import com.all.shared.download.TrackSeeders;
import com.all.shared.messages.MessEngineConstants;
import com.all.shared.model.AllMessage;
import com.all.shared.model.Track;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestRestDownloadBehavior {

	private static final String SEPARATOR = "/";
	private static final String INCOMPLETE_DIR = "Incomplete";
	private static final String DOWNLOADS_DIR = "Downloads";
	@UnderTest
	private RestDownloader restDownloader;
	@Mock
	private RestClientConfig config;
	@Mock
	private RestService restProxy;
	@Mock
	private TrackProvider trackProvider;
	@Mock
	private MessEngine messEngine;

	@Mock
	private ManagedDownloaderListener manager;
	@Mock
	private DownloaderListener listener;

	private String trackName = "temp";
	private String extension = "mp3";
	private String prefix = "REST";
	private String completeFileName = trackName + "." + extension;
	private String incompleteFileName = prefix + completeFileName;

	@Mock
	private Track track;

	private String email = "user@all.com";
	private String downloadId = "1234567890123456789012345678901234567890";
	private byte[] chunk = new byte[] { 0xF };

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
	public void setup() {
		when(config.getUserId()).thenReturn(email);

		when(config.getIncompleteDownloadsPath()).thenReturn(INCOMPLETE_DIR);
		when(config.getCompleteDownloadsPath()).thenReturn(DOWNLOADS_DIR);
		when(config.getInitialShareDelay(TimeUnit.SECONDS)).thenReturn(15L);
		when(config.getShareDelay(TimeUnit.SECONDS)).thenReturn(30L);
		when(config.getInitDownloadDelay(TimeUnit.MILLISECONDS)).thenReturn(1L);

		when(trackProvider.getTrack(downloadId)).thenReturn(track);
		when(track.getHashcode()).thenReturn(downloadId);
		when(track.getName()).thenReturn(trackName);
		when(track.getFileFormat()).thenReturn(extension);

		deleteFiles();

		restDownloader.addDownloaderListener(listener);
		when(restProxy.getChunk(eq(downloadId), anyInt())).thenAnswer(new Answer<byte[]>() {
			@Override
			public byte[] answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(10);
				return chunk;
			}
		});
	}

	@After
	public void teardown() {
		deleteFiles();
	}

	@Test
	public void shouldNotifySourcesIfFileIsAlreadyInRestServer() throws Exception {
		ManagedDownloaderListener manager = mock(ManagedDownloaderListener.class);
		ArgumentCaptor<SearchSourcesEvent> eventCaptor = ArgumentCaptor.forClass(SearchSourcesEvent.class);

		restDownloader.addManagedDownloadListener(downloadId, manager);

		restDownloader.findSources(downloadId);

		verify(manager, timeout(1000)).onSearchSources(eventCaptor.capture());
		SearchSourcesEvent searchEvent = eventCaptor.getValue();
		assertEquals(downloadId, searchEvent.getDownloadId());
		assertEquals(restDownloader, searchEvent.getSource());
		assertEquals(restDownloader, searchEvent.getDownloader());
		assertEquals(DownloadState.ReadyToDownload, searchEvent.getDownloadStatus().getState());
		restDownloader.removeManagedDownloadListener(downloadId, manager);
	}

	@Test(timeout = 1000)
	public void shouldNotifySourcesIfSeederFoundByDownloadPeer() throws Exception {
		when(restProxy.getChunk(eq(downloadId), anyInt())).thenReturn(null);
		when(config.getDownloaderSearchTimeout()).thenReturn(1000L);
		ManagedDownloaderListener manager = mock(ManagedDownloaderListener.class);
		ArgumentCaptor<SearchSourcesEvent> eventCaptor = ArgumentCaptor.forClass(SearchSourcesEvent.class);
		restDownloader.addManagedDownloadListener(downloadId, manager);

		restDownloader.findSources(downloadId);
		verify(messEngine, timeout(1000)).send(isA(AllMessage.class));
		TrackSeeders foundSeeders = new TrackSeeders(downloadId, email);
		foundSeeders.setSeeders(Arrays.asList(new String[] { "seederA", "seederB" }));
		AllMessage<TrackSeeders> response = new AllMessage<TrackSeeders>(MessEngineConstants.TRACK_SEEDERS_RESPONSE_TYPE,
				foundSeeders);
		restDownloader.onMessage(response);

		verify(manager, timeout(1000)).onSearchSources(eventCaptor.capture());
		SearchSourcesEvent searchEvent = eventCaptor.getValue();
		assertEquals(downloadId, searchEvent.getDownloadId());
		assertEquals(restDownloader, searchEvent.getSource());
		assertEquals(restDownloader, searchEvent.getDownloader());
		assertEquals(DownloadState.ReadyToDownload, searchEvent.getDownloadStatus().getState());
		restDownloader.removeManagedDownloadListener(downloadId, manager);
	}

	@Test(timeout = 5000)
	public void shouldNotifyMoreSourcesRequiredIfNoSeedersFoundByDownloadPeer() throws Exception {
		when(restProxy.getChunk(eq(downloadId), anyInt())).thenReturn(null);
		when(config.getDownloaderSearchTimeout()).thenReturn(5000L);
		ManagedDownloaderListener manager = mock(ManagedDownloaderListener.class);
		ArgumentCaptor<SearchSourcesEvent> eventCaptor = ArgumentCaptor.forClass(SearchSourcesEvent.class);
		restDownloader.addManagedDownloadListener(downloadId, manager);

		restDownloader.findSources(downloadId);
		verify(messEngine, timeout(1000)).send(isA(AllMessage.class));
		TrackSeeders foundSeeders = new TrackSeeders(downloadId, email);
		ArrayList<String> seedersList = new ArrayList<String>();
		seedersList.add(email);
		foundSeeders.setSeeders(seedersList);

		AllMessage<TrackSeeders> response = new AllMessage<TrackSeeders>(MessEngineConstants.TRACK_SEEDERS_RESPONSE_TYPE,
				foundSeeders);
		restDownloader.onMessage(response);

		verify(manager, timeout(1000)).onSearchSources(eventCaptor.capture());
		SearchSourcesEvent searchEvent = eventCaptor.getValue();
		assertEquals(downloadId, searchEvent.getDownloadId());
		assertEquals(restDownloader, searchEvent.getSource());
		assertEquals(restDownloader, searchEvent.getDownloader());
		assertEquals(DownloadState.MoreSourcesNeeded, searchEvent.getDownloadStatus().getState());
		restDownloader.removeManagedDownloadListener(downloadId, manager);
		assertTrue(seedersList.isEmpty());
	}

	@Test(timeout = 7000)
	public void shouldNotifyMoreSourcesRequiredIfSearchTimeout() throws Exception {
		when(restProxy.getChunk(eq(downloadId), anyInt())).thenReturn(null);
		when(config.getDownloaderSearchTimeout()).thenReturn(1000L);
		ManagedDownloaderListener manager = mock(ManagedDownloaderListener.class);
		ArgumentCaptor<SearchSourcesEvent> eventCaptor = ArgumentCaptor.forClass(SearchSourcesEvent.class);
		restDownloader.addManagedDownloadListener(downloadId, manager);

		restDownloader.findSources(downloadId);
		verify(messEngine, timeout(2000).atLeast(1)).send(isA(AllMessage.class));

		verify(manager, timeout(2000)).onSearchSources(eventCaptor.capture());
		SearchSourcesEvent searchEvent = eventCaptor.getValue();
		assertEquals(downloadId, searchEvent.getDownloadId());
		assertEquals(restDownloader, searchEvent.getSource());
		assertEquals(restDownloader, searchEvent.getDownloader());
		assertEquals(DownloadState.MoreSourcesNeeded, searchEvent.getDownloadStatus().getState());
		restDownloader.removeManagedDownloadListener(downloadId, manager);
	}

	@Test
	public void shouldFindSourcesAndThenDownload() throws Exception {
		restDownloader.addManagedDownloadListener(downloadId, manager);

		restDownloader.findSources(downloadId);
		DownloadStatus status = restDownloader.getStatus(downloadId);

		verify(manager, timeout(1000)).onSearchSources(any(SearchSourcesEvent.class));

		restDownloader.download(downloadId);

		awaitForDownloadCompleted(status);
		verify(listener, atLeast(99)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertDownloadedFileExists();

	}

	@Test(timeout = 20000)
	public void shouldDownloadAnAlreadyCachedFile() throws Exception {
		restDownloader.download(downloadId);

		awaitForDownloadCompleted(restDownloader.getStatus(downloadId));
		verify(listener, atLeast(99)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertDownloadedFileExists();
	}

	@Test(timeout = 20000)
	public void shouldCancelADownload() throws Exception {
		restDownloader.download(downloadId);
		awaitForExpectedProgress(restDownloader.getStatus(downloadId), 30);
		restDownloader.delete(downloadId);

		verify(listener, atLeast(29)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener, never()).onDownloadCompleted(any(DownloadCompleteEvent.class));
		Thread.sleep(500);
		assertFalse(new File(INCOMPLETE_DIR + SEPARATOR + incompleteFileName).exists());
	}

	@Test(timeout = 20000)
	public void shouldPauseAndResumeADownload() throws Exception {
		restDownloader.download(downloadId);
		DownloadStatus status = restDownloader.getStatus(downloadId);
		awaitForExpectedProgress(status, 30);

		restDownloader.pause(downloadId);

		verify(listener, atLeast(30)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		Thread.sleep(100);
		verify(listener, never()).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertEquals(DownloadState.Paused, restDownloader.getStatus(downloadId).getState());

		restDownloader.resume(downloadId);
		awaitForExpectedProgress(status, 60);
		restDownloader.pause(downloadId);

		verify(listener, atLeast(60)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		Thread.sleep(100);
		restDownloader.resume(downloadId);

		awaitForDownloadCompleted(status);
		verify(listener, atLeast(99)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertDownloadedFileExists();
	}

	private void awaitForDownloadCompleted(DownloadStatus status) throws Exception {
		while (status.getState() != DownloadState.Complete) {
			Thread.sleep(100);
		}
		Thread.sleep(100);
	}

	@Test(timeout = 20000)
	public void shouldRemoveDownloaderListener() throws Exception {
		restDownloader.download(downloadId);
		DownloadStatus status = restDownloader.getStatus(downloadId);
		awaitForExpectedProgress(status, 50);
		restDownloader.removeDownloaderListener(listener);
		verify(listener, Mockito.atMost(status.getProgress())).onDownloadUpdated(any(DownloadUpdateEvent.class));
		if (status.getProgress() < 100) {
			verify(listener, never()).onDownloadCompleted(any(DownloadCompleteEvent.class));
		}
		awaitForDownloadCompleted(status);
		assertDownloadedFileExists();
	}

	@Test(timeout = 20000)
	public void shouldHandleDownloaderListenerExceptions() throws Exception {
		RuntimeException listenerException = new RuntimeException("SomeListenerException");
		doThrow(listenerException).when(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		doThrow(listenerException).when(listener).onDownloadUpdated(any(DownloadUpdateEvent.class));

		restDownloader.download(downloadId);

		awaitForDownloadCompleted(restDownloader.getStatus(downloadId));
		verify(listener, atLeast(99)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertDownloadedFileExists();
	}

	@Test(timeout = 10000)
	public void shouldReportAnExceptionDuringDownload() throws Exception {
		when(restProxy.getChunk(eq(downloadId), anyInt())).thenThrow(new RuntimeException("Some server error."));
		restDownloader.download(downloadId);
		DownloadStatus status = restDownloader.getStatus(downloadId);
		while (restDownloader.getStatus(downloadId) != null) {
			Thread.sleep(100);
		}
		assertEquals(DownloadState.Error, status.getState());
		assertFalse(new File(INCOMPLETE_DIR + SEPARATOR + incompleteFileName).exists());
	}

	@Test
	public void shouldRequestTrackUploadToDownloadPeerWhenChunkIsNotCached() throws Exception {
		when(restProxy.getChunk(eq(downloadId), anyInt())).thenReturn(new byte[] {}, chunk);
		when(config.getMinChunkAwaitDelay(TimeUnit.MILLISECONDS)).thenReturn(10L);
		when(config.getChunkAwaitTimeout(TimeUnit.MILLISECONDS)).thenReturn(40L);

		restDownloader.download(downloadId);

		awaitForDownloadCompleted(restDownloader.getStatus(downloadId));
		verify(messEngine, timeout(1000).times(2)).send(isA(AllMessage.class));
		verify(listener, atLeast(99)).onDownloadUpdated(any(DownloadUpdateEvent.class));
		verify(listener).onDownloadCompleted(any(DownloadCompleteEvent.class));
		assertDownloadedFileExists();
	}

	@Test
	public void shouldRetryRequestToDownloadPeerAfterChunkAwaitTimeOut() throws Exception {
		when(restProxy.getChunk(eq(downloadId), anyInt())).thenReturn(new byte[] {});
		when(config.getMinChunkAwaitDelay(TimeUnit.MILLISECONDS)).thenReturn(10L);
		when(config.getChunkAwaitTimeout(TimeUnit.MILLISECONDS)).thenReturn(40L);
		restDownloader.download(downloadId);
		verify(messEngine, timeout(1000)).send(isA(AllMessage.class));

	}

	private void deleteFiles() {
		File incompleteFile = new File(INCOMPLETE_DIR + SEPARATOR + incompleteFileName);
		if (incompleteFile.exists()) {
			incompleteFile.delete();
		}
		assertFalse(incompleteFile.exists());
		File downloadedFile = new File(DOWNLOADS_DIR + SEPARATOR + completeFileName);
		if (downloadedFile.exists()) {
			downloadedFile.delete();
		}
		assertFalse(downloadedFile.exists());
	}

	private void assertDownloadedFileExists() {
		assertTrue(new File(DOWNLOADS_DIR + SEPARATOR + completeFileName).exists());
		assertFalse(new File(INCOMPLETE_DIR + SEPARATOR + incompleteFileName).exists());
	}

	private void awaitForExpectedProgress(DownloadStatus status, int expectedProgress) throws DownloadException,
			InterruptedException {
		while (status.getProgress() < expectedProgress) {
			Thread.sleep(1);
		}
	}

}
