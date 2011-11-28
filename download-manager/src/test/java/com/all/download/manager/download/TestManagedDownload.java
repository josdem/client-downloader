package com.all.download.manager.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;
import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.DownloaderListener;
import com.all.downloader.download.ManagedDownloader;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.testing.MockInyectRunner;
import com.all.testing.Stub;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestManagedDownload {
	@UnderTest
	private ManagedDownload managedDownload;
	@Mock
	private DownloaderListener downloaderListener; 
	@Mock
	private SearchSourcesEvent searchSourcesEvent;
	@Mock
	DownloadStatus downloadStatus;
	@Mock
	DownloadUpdateEvent downloadUpdateEvent;
	@Mock
	DownloadCompleteEvent downloadCompleteEvent;
	@Stub
	private String downloadId = "downloadId";
	@Stub
	private Collection<ManagedDownloader> managedDownloaderCollection = new ArrayList<ManagedDownloader>();
	@Stub
	private Set<Integer> blockedDownloaders = new HashSet<Integer>();
	@Stub
	private final TreeSet<ManagedDownloader> candidateDownloaders = new TreeSet<ManagedDownloader>(
			new ManagedDownloadPriorityComparator());
	@Captor
	ArgumentCaptor<DownloadUpdateEvent> updateEventCaptor;
	// cannot mock with annotation as it will cause different behavior than
	// expected due to MockInyectRunner
	private ManagedDownloader managedDownloader = mock(ManagedDownloader.class);
	private ManagedDownloader managedDownloader1 = mock(ManagedDownloader.class);
	private ManagedDownloader managedDownloader2 = mock(ManagedDownloader.class);

	@Before
	public void setup() {
		managedDownloaderCollection.add(managedDownloader);
		managedDownloaderCollection.add(managedDownloader1);
		managedDownloaderCollection.add(managedDownloader2);

		when(managedDownloader.getDownloaderPriority()).thenReturn(0);
		when(managedDownloader1.getDownloaderPriority()).thenReturn(1);
		when(managedDownloader2.getDownloaderPriority()).thenReturn(2);
		
		when(managedDownloader.toString()).thenReturn("downloader1");
		when(managedDownloader1.toString()).thenReturn("downloader2");
		when(managedDownloader2.toString()).thenReturn("downloader3");
		
		when(searchSourcesEvent.getDownloadStatus()).thenReturn(downloadStatus);
	}

	@Test
	public void increaseCoverage() throws Exception {
		ManagedDownload managedDownload = new ManagedDownload(downloadId);
		managedDownload.setDownloaderListener(downloaderListener);
		managedDownload.setManagedDownloaders(managedDownloaderCollection);
	}

	@Test
	public void shouldFindSourcesOnDownload() throws Exception {
		assertTrue(blockedDownloaders.isEmpty());

		managedDownload.download();

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);
		
		verify(managedDownloader).addManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader).findSources(downloadId);

		verify(managedDownloader1).addManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader1).findSources(downloadId);

		verify(managedDownloader2).addManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader2).findSources(downloadId);

		assertTrue(blockedDownloaders.contains(1));
		assertTrue(blockedDownloaders.contains(2));
		assertTrue(blockedDownloaders.contains(3));
	}

	@Test
	public void shouldFindSourcesOnDownloadEvenIfADownloaderFails() throws Exception {
		int expectedPriorityBlocked = 1;

		assertTrue(blockedDownloaders.isEmpty());

		managedDownload.download();

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);
		
		verify(managedDownloader).addManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader).findSources(downloadId);
		assertTrue(blockedDownloaders.contains(expectedPriorityBlocked));
	}

	@Test
	public void shouldRemoveAsListenerOnException() throws Exception {
		doThrow(new DownloadException("")).when(managedDownloader).findSources(downloadId);

		assertTrue(blockedDownloaders.isEmpty());

		managedDownload.download();

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);
		verify(managedDownloader).addManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
	}

	@Test
	public void shouldDelete() throws Exception {
		managedDownload.delete();

		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader1).delete(downloadId);
		verify(managedDownloader2).delete(downloadId);
	}

	@Test
	public void shouldPause() throws Exception {
		managedDownload.pause();

		verify(managedDownloader).pause(downloadId);
		verify(managedDownloader1).pause(downloadId);
		verify(managedDownloader2).pause(downloadId);
	}

	@Test
	public void shouldResume() throws Exception {
		managedDownload.resume();

		verify(managedDownloader).resume(downloadId);
		verify(managedDownloader1).resume(downloadId);
		verify(managedDownloader2).resume(downloadId);
	}

	@Test
	public void shouldCallOnlyTheCurrentManagedDownloaderForDeletePauseAndResume() throws Exception {
		// this codes setup a current managed downloader
		arrangeCandidateIsSetAsSelectedDownloader();

		// this test really starts here
		doThrow(new DownloadException("")).when(managedDownloader).pause(downloadId);

		managedDownload.delete();
		managedDownload.pause();
		managedDownload.resume();

		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).pause(downloadId);
		verify(managedDownloader).resume(downloadId);

		verify(managedDownloader1, never()).delete(downloadId);
		verify(managedDownloader1, never()).pause(downloadId);
		verify(managedDownloader1, never()).resume(downloadId);
		verify(managedDownloader2, never()).delete(downloadId);
		verify(managedDownloader2, never()).pause(downloadId);
		verify(managedDownloader2, never()).resume(downloadId);
	}

	@Test
	public void shouldCallAllDownloadersPauseEvenIfOneFails() throws Exception {
		doThrow(new DownloadException("")).when(managedDownloader).pause(downloadId);
		doThrow(new DownloadException("")).when(managedDownloader2).pause(downloadId);

		managedDownload.pause();

		verify(managedDownloader).pause(downloadId);
		verify(managedDownloader1).pause(downloadId);
		verify(managedDownloader2).pause(downloadId);
	}

	private void arrangeCandidateIsSetAsSelectedDownloader() {
		when(searchSourcesEvent.getDownloader()).thenReturn(managedDownloader);
		when(downloadStatus.getState()).thenReturn(DownloadState.ReadyToDownload);

		managedDownload.download();
		managedDownload.onSearchSources(searchSourcesEvent);

		reset(managedDownloader);
		reset(managedDownloader1);
		reset(managedDownloader2);
	}

	@Test
	public void shouldIgnoreEventsAfterADownloaderHasBeenSelected() throws Exception {
		arrangeCandidateIsSetAsSelectedDownloader();

		reset(searchSourcesEvent);
		reset(downloadStatus);

		managedDownload.onSearchSources(searchSourcesEvent);

		verify(searchSourcesEvent, never()).getDownloadStatus();
		verify(downloadStatus, never()).getState();
	}

	@Test
	public void shouldStartDownloadFromHighestPriorityCandidateEvent() throws Exception {
		when(searchSourcesEvent.getDownloader()).thenReturn(managedDownloader);
		when(downloadStatus.getState()).thenReturn(DownloadState.ReadyToDownload);

		managedDownload.download();

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);

		managedDownload.onSearchSources(searchSourcesEvent);

		verify(managedDownloader).download(downloadId);
		verifyEventSentWithStatusAndResetListener(DownloadState.ReadyToDownload);
		verify(managedDownloader1).delete(downloadId);
		verify(managedDownloader1).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader2).delete(downloadId);
		verify(managedDownloader2).removeManagedDownloadListener(downloadId, managedDownload);
	}

	private void verifyEventIsCorrect(DownloadState state) {
		DownloadUpdateEvent searchEvent = updateEventCaptor.getValue();
		DownloadStatus searchStatus = searchEvent.getDownloadStatus();
		assertEquals(downloadId, searchEvent.getDownloadId());
		assertEquals(downloaderListener, searchEvent.getSource());
		assertEquals(state, searchStatus.getState());
		assertEquals(downloadId, searchStatus.getDownloadId());
	}

	@Test
	public void shouldStartDownloadFromSecondPriorityCandidateReceivingEventAfterFirstCandidate() throws Exception {
		managedDownload.download(); // this calls find sources

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);
		
		sendEventWithStateFromDownloader(managedDownloader1, DownloadState.ReadyToDownload);

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);
		
		verifyEventSentWithStatusAndResetListener(DownloadState.ReadyToDownload);
		verify(managedDownloader1).download(downloadId);
		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader2).delete(downloadId);
		verify(managedDownloader2).removeManagedDownloadListener(downloadId, managedDownload);
	}

	private void verifyEventSentWithStatusAndResetListener(DownloadState state) {
		verify(downloaderListener).onDownloadUpdated(updateEventCaptor.capture());
		verifyEventIsCorrect(state);
		reset(downloaderListener);
	}

	@Test
	public void shouldStartDownloadFromSecondPriorityCandidateReceivingEventBeforeFirstCandidate() throws Exception {
		managedDownload.download(); // this calls find sources
		
		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);

		sendEventWithStateFromDownloader(managedDownloader1, DownloadState.ReadyToDownload);

		verify(managedDownloader1).download(downloadId);
		verifyEventSentWithStatusAndResetListener(DownloadState.ReadyToDownload);
		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader2).delete(downloadId);
		verify(managedDownloader2).removeManagedDownloadListener(downloadId, managedDownload);
	}

	private void sendEventWithStateFromDownloader(ManagedDownloader managedDownloader, DownloadState downloadState) {
		when(searchSourcesEvent.getDownloader()).thenReturn(managedDownloader);
		when(downloadStatus.getState()).thenReturn(downloadState);

		managedDownload.onSearchSources(searchSourcesEvent);
	}

	@Test
	public void shouldStartDownloadFromThirdPriorityCandidateReceivingMorSourcesEventFromFirstTwoDownloadersFirst()
			throws Exception {
		managedDownload.download(); // this calls find sources

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);

		sendEventWithStateFromDownloader(managedDownloader1, DownloadState.MoreSourcesNeeded);

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);

		when(searchSourcesEvent.getDownloader()).thenReturn(managedDownloader2);
		when(downloadStatus.getState()).thenReturn(DownloadState.ReadyToDownload);

		managedDownload.onSearchSources(searchSourcesEvent);

		verify(managedDownloader2).download(downloadId);
		verifyEventSentWithStatusAndResetListener(DownloadState.ReadyToDownload);
		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader1).delete(downloadId);
		verify(managedDownloader1).removeManagedDownloadListener(downloadId, managedDownload);
	}

	@Test
	public void shouldStartDownloadFromThirdPriorityCandidateReceivingMorSourcesEventFromFirstTwoDownloadersLast()
			throws Exception {
		managedDownload.download(); // this calls find sources

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);

		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.ReadyToDownload);

		sendEventWithStateFromDownloader(managedDownloader1, DownloadState.MoreSourcesNeeded);

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);

		verify(managedDownloader2).download(downloadId);
		verifyEventSentWithStatusAndResetListener(DownloadState.ReadyToDownload);
		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader1).delete(downloadId);
		verify(managedDownloader1).removeManagedDownloadListener(downloadId, managedDownload);
	}

	@Test
	public void shouldRemoveDownloadFromAllDownloadersIfSelectedDownloaderFailsToDownload() throws Exception {
		managedDownload.download(); // this calls find sources
		doThrow(new DownloadException("")).when(managedDownloader2).download(downloadId);
		doThrow(new DownloadException("")).when(managedDownloader1).delete(downloadId);

		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.ReadyToDownload);

		sendEventWithStateFromDownloader(managedDownloader1, DownloadState.MoreSourcesNeeded);

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);

		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader1).delete(downloadId);
		verify(managedDownloader1).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader2).delete(downloadId);
		verify(managedDownloader2).removeManagedDownloadListener(downloadId, managedDownload);
	}

	@Test
	public void shouldStartDownloadAfterSendingAMoreSourcesNeededInFirstDownloaders() throws Exception {
		managedDownload.download(); // this calls find sources

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);

		verify(downloaderListener, never()).onDownloadUpdated(any(DownloadUpdateEvent.class));
		
		sendEventWithStateFromDownloader(managedDownloader1, DownloadState.MoreSourcesNeeded);

		verify(downloaderListener, never()).onDownloadUpdated(any(DownloadUpdateEvent.class));

		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.ReadyToDownload);

		verifyEventSentWithStatusAndResetListener(DownloadState.ReadyToDownload);
		verify(managedDownloader2).download(downloadId);
		verify(managedDownloader1).delete(downloadId);
		verify(managedDownloader1).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
	}
	
	@Test
	public void shouldStartDownloadAfterSendingAMoreSourcesNeededInFirstAndLastDownloader() throws Exception {
		managedDownload.download(); // this calls find sources

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);
		
		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.MoreSourcesNeeded);

		verify(downloaderListener, never()).onDownloadUpdated(any(DownloadUpdateEvent.class));
		
		sendEventWithStateFromDownloader(managedDownloader1, DownloadState.ReadyToDownload);

		verify(downloaderListener, never()).onDownloadUpdated(any(DownloadUpdateEvent.class));
		
		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);
		
		verifyEventSentWithStatusAndResetListener(DownloadState.ReadyToDownload);
		verify(managedDownloader1).download(downloadId);
		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader2).delete(downloadId);
		verify(managedDownloader2).removeManagedDownloadListener(downloadId, managedDownload);
	}

	@Test
	public void shouldKeepNormalBehaviorIfReceivedTwiceSameEvent() throws Exception {
		managedDownload.download(); // this calls find sources

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);
		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);

		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.MoreSourcesNeeded);
		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.MoreSourcesNeeded);
		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.MoreSourcesNeeded);

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.ReadyToDownload);

		verify(managedDownloader).download(downloadId);
		verify(managedDownloader1).delete(downloadId);
		verify(managedDownloader1).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader2).delete(downloadId);
		verify(managedDownloader2).removeManagedDownloadListener(downloadId, managedDownload);
	}

	@Test
	public void shouldNotifyMoreSourcesNeededIAllDownloadersCouldNotFindSources() throws Exception {
		managedDownload.download(); // this calls find sources
		doThrow(new DownloadException("")).when(managedDownloader1).delete(downloadId);

		verifyEventSentWithStatusAndResetListener(DownloadState.Searching);

		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.MoreSourcesNeeded);

		sendEventWithStateFromDownloader(managedDownloader1, DownloadState.MoreSourcesNeeded);

		sendEventWithStateFromDownloader(managedDownloader, DownloadState.MoreSourcesNeeded);

		verifyEventSentWithStatusAndResetListener(DownloadState.MoreSourcesNeeded);
		verify(managedDownloader).delete(downloadId);
		verify(managedDownloader).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader1).delete(downloadId);
		verify(managedDownloader1).removeManagedDownloadListener(downloadId, managedDownload);
		verify(managedDownloader2).delete(downloadId);
		verify(managedDownloader2).removeManagedDownloadListener(downloadId, managedDownload);
	}

	@Test
	public void shouldIgnoreUnexpectedEvents() throws Exception {
		managedDownload.download(); // this calls find sources

		sendEventWithStateFromDownloader(managedDownloader2, DownloadState.Downloading);

		// no candidate downloaders added
		assertTrue(candidateDownloaders.isEmpty());
		// priorities weren't touch
		assertTrue(blockedDownloaders.contains(1));
		assertTrue(blockedDownloaders.contains(2));
		assertTrue(blockedDownloaders.contains(3));
	}

	@Test
	public void shouldPropagateDownloadUpdateEvent() throws Exception {
		arrangeCandidateIsSetAsSelectedDownloader();

		managedDownload.onDownloadUpdated(downloadUpdateEvent);

		verify(downloaderListener).onDownloadUpdated(downloadUpdateEvent);
	}

	@Test
	public void shouldNotPropagateDownloadUpdateEventIfDownloaderNotSelectedYet() throws Exception {
		managedDownload.onDownloadUpdated(downloadUpdateEvent);

		verify(downloaderListener, never()).onDownloadUpdated(downloadUpdateEvent);
	}

	@Test
	public void shouldPropagateDownloadCompleteEvent() throws Exception {
		arrangeCandidateIsSetAsSelectedDownloader();

		managedDownload.onDownloadCompleted(downloadCompleteEvent);

		verify(downloaderListener).onDownloadCompleted(downloadCompleteEvent);
	}

	@Test
	public void shouldNotPropagateDownloadCompleteEventIfDownloaderNotSelectedYet() throws Exception {
		managedDownload.onDownloadCompleted(downloadCompleteEvent);

		verify(downloaderListener, never()).onDownloadCompleted(downloadCompleteEvent);
	}

	@Test
	public void shouldCompareBasedOnPriority() throws Exception {
		TreeSet<ManagedDownloader> candidateDownloaders = new TreeSet<ManagedDownloader>(
				new ManagedDownloadPriorityComparator());

		candidateDownloaders.add(managedDownloader1);
		candidateDownloaders.add(managedDownloader2);
		candidateDownloaders.add(managedDownloader);

		assertEquals(managedDownloader, candidateDownloaders.pollFirst());
		assertEquals(managedDownloader1, candidateDownloaders.pollFirst());
		assertEquals(managedDownloader2, candidateDownloaders.pollFirst());
	}
}
