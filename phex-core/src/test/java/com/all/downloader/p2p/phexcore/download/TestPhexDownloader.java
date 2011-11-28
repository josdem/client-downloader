package com.all.downloader.p2p.phexcore.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import phex.common.URN;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.query.Search;
import phex.query.SearchContainer;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.DownloaderListener;
import com.all.downloader.download.ManagedDownloaderConfig;
import com.all.downloader.p2p.phexcore.PhexCore;
import com.all.downloader.p2p.phexcore.download.PhexDownloader.PhexDownloadFactory;
import com.all.downloader.p2p.phexcore.helper.DownloadHelper;
import com.all.downloader.p2p.phexcore.helper.SearchHelper;
import com.all.downloader.p2p.phexcore.search.PhexSearcher;
import com.all.messengine.MessEngine;
import com.all.shared.download.TrackProvider;
import com.all.shared.model.Track;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestPhexDownloader {
	private static final String ALL_LINK_WITH_CANDIDATE_STRING = "allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b&candidate=josdem@all.com";
	private static final String DOWNLOAD_ID = "00a9ae41a50cfece357f26e786db6fa014af765b";
	private static final String URN_SHA = "J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
	private static final String allLinkAsString = "allLink:urnsha1=TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI";

	@UnderTest
	private PhexDownloader phexDownloader;
	@Mock
	private TrackProvider trackProvider;
	@Mock
	private PhexCore phexCore;
	@SuppressWarnings("unused")
	// injected
	@Mock
	private ManagedDownloaderConfig downloaderConfig;
	@SuppressWarnings("unused")
	// injected
	@Mock
	private SearchHelper searchHelper;
	@SuppressWarnings("unused")
	// injected
	@Mock
	private MessEngine messEngine;	
	@Mock
	private File fileToDownload;
	@Mock
	private ScheduledExecutorService scheduler;
	@Mock
	private PhexDownload phexDownload;
	@Mock
	private URN urn;
	@Mock
	private SWDownloadFile downloadFile;
	@Mock
	private DownloadHelper downloadHelper;
	@Mock
	private PhexSearcher phexSearcher;
	@Mock
	private PhexDownloadFactory phexDownloadFactory;
	@Mock
	private Track track;
	private String trackName = "trackName";
	private String fileFormat = "mp3";

	@Before
	public void setup() throws Exception {
		when(phexDownloadFactory.newDownload()).thenReturn(phexDownload);
		when(trackProvider.getTrack(DOWNLOAD_ID)).thenReturn(track);
		when(track.getHashcode()).thenReturn(DOWNLOAD_ID);
		when(track.getName()).thenReturn(trackName);
		when(track.getFileFormat()).thenReturn(fileFormat);
		when(phexDownload.getFile(anyString())).thenReturn(fileToDownload);
	}

	@Test(expected = DownloadException.class)
	public void shouldNotDownloadIfNoAllLink() throws Exception {
		phexDownloader.download(DOWNLOAD_ID);

		assertTrue(phexDownloader.downloadIdToPhexDownload.isEmpty());
		assertTrue(phexDownloader.phexShaHashToDownloadId.isEmpty());
	}

	@Test(expected = DownloadException.class)
	public void shouldNotDownloadIfNoUrnSha() throws Exception {
		when(track.getDownloadString()).thenReturn("allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b");

		phexDownloader.download(DOWNLOAD_ID);

		assertTrue(phexDownloader.downloadIdToPhexDownload.isEmpty());
		assertTrue(phexDownloader.phexShaHashToDownloadId.isEmpty());
	}

	@Test
	public void shouldDownloadIfNoSeedingGnutellaAndHaveUrnSha() throws Exception {
		String allLinkString = "allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b&urnsha1=J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
		when(track.getDownloadString()).thenReturn(allLinkString);
		when(phexCore.isSeedingGnutella()).thenReturn(false);

		phexDownloader.download(DOWNLOAD_ID);

		assertFalse(phexDownloader.downloadIdToPhexDownload.isEmpty());
		assertFalse(phexDownloader.phexShaHashToDownloadId.isEmpty());
	}

	@Test
	public void shouldDownloadIfSeedingGnutellaAndHaveHashCode() throws Exception {
		String allLinkString = "allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b&urnsha1=J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
		when(track.getDownloadString()).thenReturn(allLinkString);
		when(phexCore.isSeedingGnutella()).thenReturn(true);

		phexDownloader.download(DOWNLOAD_ID);

		verify(track).getDownloadString();
		assertFalse(phexDownloader.downloadIdToPhexDownload.isEmpty());
		assertFalse(phexDownloader.phexShaHashToDownloadId.isEmpty());
	}

	@Test
	public void shouldDownloadATrack() throws Exception {
		Long size = 700L;
		String allLinkString = "allLink:urnsha1=J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
		when(track.getDownloadString()).thenReturn(allLinkString);

		phexDownloader.managePhexDownload(DOWNLOAD_ID, phexDownload);

		when(phexCore.isSeedingGnutella()).thenReturn(true);
		when(track.getSize()).thenReturn(size);

		SearchContainer searchContainer = mock(SearchContainer.class);

		Search search = mock(Search.class);
		when(searchContainer.createSearch(anyString())).thenReturn(search);

		phexDownloader.download(DOWNLOAD_ID);

		assertFalse(phexDownloader.downloadIdToPhexDownload.isEmpty());
		assertFalse(phexDownloader.phexShaHashToDownloadId.isEmpty());
	}

	@Test
	public void shouldNotDownloadIfdownloadIdIsNull() throws Exception {
		String downloadId = null;

		try {
			phexDownloader.download(downloadId);

			fail("Should not reach this code, exception should have been thrown above");
		} catch (DownloadException de) {
			// expected, do nothing
		}

		assertEquals(0, phexDownloader.downloadIdToPhexDownload.size());
		verify(track, never()).getDownloadString();
	}

	@Test
	public void shouldNotUpdateDownloadStatusIfcurrentDownloasIsEmpty() throws Exception {
		Set<DownloaderListener> listeners = new HashSet<DownloaderListener>();
		DownloaderListener listener = mock(DownloaderListener.class);
		listeners.add(listener);

		Map<String, PhexDownload> currentDownloads = new HashMap<String, PhexDownload>();
		phexDownloader.downloadIdToPhexDownload = currentDownloads;

		phexDownloader.updateStatus();

		verify(listener, never()).onDownloadUpdated(isA(DownloadUpdateEvent.class));
	}

	@Test
	public void shouldUpdateDownloadStatus() throws Exception {
		Map<String, PhexDownload> currentDownloads = new HashMap<String, PhexDownload>();

		DownloaderListener listener = mock(DownloaderListener.class);
		SwarmingManager swarmingManager = mock(SwarmingManager.class);
		SWDownloadFile swDownloadFile = mock(SWDownloadFile.class);

		when(phexDownload.getFileHashcode()).thenReturn(URN_SHA);
		when(swarmingManager.getDownloadFileByURN((URN) anyObject())).thenReturn(swDownloadFile);
		when(swDownloadFile.getProgress()).thenReturn(0);
		when(scheduler.isShutdown()).thenReturn(false).thenReturn(true);

		currentDownloads.put(DOWNLOAD_ID, phexDownload);
		phexDownloader.downloadIdToPhexDownload = currentDownloads;
		phexDownloader.addDownloaderListener(listener);

		phexDownloader.updateStatus();

		verify(listener).onDownloadUpdated(isA(DownloadUpdateEvent.class));
	}

	@Test
	public void shouldAddDownload() throws Exception {
		when(phexDownload.getFileHashcode()).thenReturn(URN_SHA);

		assertTrue(phexDownloader.downloadIdToPhexDownload.isEmpty());
		assertTrue(phexDownloader.phexShaHashToDownloadId.isEmpty());

		phexDownloader.managePhexDownload(DOWNLOAD_ID, phexDownload);

		assertEquals(phexDownload, phexDownloader.downloadIdToPhexDownload.get(DOWNLOAD_ID));
		assertEquals(DOWNLOAD_ID, phexDownloader.phexShaHashToDownloadId.get(URN_SHA));
	}

	private SWDownloadFile setPauseExpectations() {
		SWDownloadFile downloadFile = mock(SWDownloadFile.class);
		when(phexDownload.getDownloadFile()).thenReturn(downloadFile);
		Map<String, PhexDownload> currentDownloads = new HashMap<String, PhexDownload>();
		currentDownloads.put(DOWNLOAD_ID, phexDownload);
		phexDownloader.downloadIdToPhexDownload = currentDownloads;
		return downloadFile;
	}

	@Test
	public void shouldNotPauseIfNoDownloadId() throws Exception {
		SWDownloadFile downloadFile = setPauseExpectations();
		phexDownloader.pause("");
		verify(downloadFile, never()).stopDownload();
	}

	@Test
	public void shouldPauseADownload() throws Exception {
		SWDownloadFile downloadFile = setPauseExpectations();
		phexDownloader.pause(DOWNLOAD_ID);
		verify(downloadFile).stopDownload();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldNotFailInANullPointerExceptionWhenPausing() throws Exception {
		Map<String, PhexDownload> currentDownloads = Collections.EMPTY_MAP;
		phexDownloader.downloadIdToPhexDownload = currentDownloads;

		phexDownloader.pause(DOWNLOAD_ID);

		assertNull(currentDownloads.get(DOWNLOAD_ID));
	}

	@Test
	public void shouldResumeADownload() throws Exception {
		SWDownloadFile downloadFile = setPauseExpectations();

		phexDownloader.resume(DOWNLOAD_ID);

		verify(downloadFile).startDownload();
	}

	@Test
	public void shouldNotResumeIfNoDownloadId() throws Exception {
		SWDownloadFile downloadFile = setPauseExpectations();
		phexDownloader.resume("");
		verify(downloadFile, never()).startDownload();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldNotFailInANullPointerExceptionWhenResuming() throws Exception {
		Map<String, PhexDownload> currentDownloads = Collections.EMPTY_MAP;
		phexDownloader.downloadIdToPhexDownload = currentDownloads;

		phexDownloader.resume(DOWNLOAD_ID);

		assertNull(currentDownloads.get(DOWNLOAD_ID));
	}

	@Test
	public void shouldPauseAndThenResume() throws Exception {
		PhexDownload phexDownload = new PhexDownload(ALL_LINK_WITH_CANDIDATE_STRING);
		SWDownloadFile downloadFile = mock(SWDownloadFile.class);

		phexDownload.setDownloadFile(downloadFile);
		Map<String, PhexDownload> currentDownloads = new HashMap<String, PhexDownload>();
		currentDownloads.put(DOWNLOAD_ID, phexDownload);
		phexDownloader.downloadIdToPhexDownload = currentDownloads;

		phexDownloader.pause(DOWNLOAD_ID);

		verify(downloadFile).stopDownload();

		phexDownloader.resume(DOWNLOAD_ID);
		verify(downloadFile).startDownload();
		assertEquals(DownloadState.Downloading, phexDownload.getState());
	}

	@Test
	public void shouldWasnotBeingManagedDelete() throws Exception {
		PhexDownload phexDownload = new PhexDownload(ALL_LINK_WITH_CANDIDATE_STRING);
		phexDownloader.downloadIdToPhexDownload.put(DOWNLOAD_ID, phexDownload);
		phexDownloader.delete(DOWNLOAD_ID);
		verify(phexCore, never()).removeSWDownloadFile(any(SWDownloadFile.class));
	}

	@Test
	public void shouldDelete() throws Exception {
		SWDownloadFile downloadFile = mock(SWDownloadFile.class);

		PhexDownload phexDownload = new PhexDownload(ALL_LINK_WITH_CANDIDATE_STRING);
		phexDownload.setDownloadFile(downloadFile);

		phexDownloader.downloadIdToPhexDownload.put(DOWNLOAD_ID, phexDownload);

		phexDownloader.delete(DOWNLOAD_ID);

		verify(phexCore).removeSWDownloadFile(downloadFile);
		assertTrue(phexDownloader.downloadIdToPhexDownload.isEmpty());
	}

	@Test
	public void shouldConstructWithoutURNPrefix() throws Exception {
		String fileHashCode = "fileHashCode";
		String urnsha = PhexDownloader.URN_PREFIX + fileHashCode;
		assertEquals(fileHashCode, phexDownloader.extractHashcodeFromUrn(urnsha));
	}

	@Test
	public void shouldNotifyCompleteIfFileExist() throws Exception {
		String allLinkString = "allLink:urnsha1=TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI&magnetLink=9d50b64820e7e288146a9ae4e14902a3ba584999&candidate=josdem@all.com";
		when(track.getDownloadString()).thenReturn(allLinkString);
		when(fileToDownload.exists()).thenReturn(true);
		assertTrue(phexDownloader.downloadIdToPhexDownload.isEmpty());
		when(phexCore.isSeedingGnutella()).thenReturn(true);

		phexDownloader.download(DOWNLOAD_ID);

		verify(phexDownload).setProgress(100);
		assertTrue(phexDownloader.downloadIdToPhexDownload.isEmpty());
	}

	@Test
	public void shouldNotNotifyIfNoPhexDownload() throws Exception {
		String topic = setNotifyCompleteExpectations();

		phexDownloader.onDownloadFileCompletedEvent(topic, downloadFile);

		verify(downloadFile, never()).getDestinationFile();
	}

	private String setNotifyCompleteExpectations() {
		String topic = "topic";
		String urnsha = "urnsha";
		when(urn.getAsString()).thenReturn(urnsha);
		when(downloadFile.getFileURN()).thenReturn(urn);
		return topic;
	}

	@Test
	public void shouldNotifyDownloadCompleteByPhex() throws Exception {
		String urnsha = "urnsha";
		File destinationFile = mock(File.class);

		String topic = setNotifyCompleteExpectations();

		when(phexDownload.getFileHashcode()).thenReturn(urnsha);
		when(downloadFile.getDestinationFile()).thenReturn(destinationFile);

		phexDownloader.phexShaHashToDownloadId.put(urnsha, DOWNLOAD_ID);
		phexDownloader.downloadIdToPhexDownload.put(DOWNLOAD_ID, phexDownload);
		phexDownloader.onDownloadFileCompletedEvent(topic, downloadFile);

		verify(downloadFile).getDestinationFile();
		assertTrue(phexDownloader.downloadIdToPhexDownload.isEmpty());
		assertTrue(phexDownloader.phexShaHashToDownloadId.isEmpty());
	}

	@Test
	public void shouldStartGnutellaDownload() throws Exception {
		List<RemoteFile> remoteFileList = new ArrayList<RemoteFile>();
		setStartGnutellaDownlodExpectations();
		when(phexSearcher.getAllSearchResults()).thenReturn(remoteFileList);

		phexDownloader.startGnutellaDownload(DOWNLOAD_ID);

		verify(downloadHelper).lookForCandidatesFromSearchResults(remoteFileList, phexDownload);
	}

	@Test
	public void shouldGetPriority() throws Exception {
		int expectedPriority = 2;
		when(phexCore.getDownloaderPriority()).thenReturn(expectedPriority);
		assertEquals(expectedPriority, phexDownloader.getDownloaderPriority());
	}

	private void setStartGnutellaDownlodExpectations() {
		assertTrue(phexDownloader.downloadIdToPhexDownload.isEmpty());
		phexDownloader.downloadIdToPhexDownload.put(DOWNLOAD_ID, phexDownload);
	}

	public void shouldNotFindSourcesIfDownloadIdEmpty() throws Exception {
		phexDownloader.new FindSourcesTask("").run();
		verify(phexDownload).setState(DownloadState.Error);
	}

	public void shouldNotFindSourcesIfAllLinkEmpty() throws Exception {
		phexDownloader.new FindSourcesTask(DOWNLOAD_ID).run();
		verify(phexDownload).setState(DownloadState.Error);
	}

	public void shouldNotFindSourcesIfUrnshaEmpty() throws Exception {
		String allLinkAsString = "allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b";
		when(track.getDownloadString()).thenReturn(allLinkAsString);
		phexDownloader.new FindSourcesTask(DOWNLOAD_ID).run();
		verify(phexDownload).setState(DownloadState.Error);
	}

	@Test
	public void shouldDoASearchOnFindSourcesCall() throws Exception {
		when(phexCore.getDownloaderTimeout()).thenReturn(10);

		when(track.getDownloadString()).thenReturn(allLinkAsString);

		phexDownloader.new FindSourcesTask(DOWNLOAD_ID).run();

		verify(phexDownload, never()).setState(DownloadState.Error);
		verify(phexSearcher).search(anyString());
		verify(scheduler).schedule(isA(Runnable.class), eq(10L), eq(TimeUnit.SECONDS));
	}

	@Test
	public void shouldDoNotASearchOnFindSourcesCall() throws Exception {
		String keyword = "keyword";
		when(track.getDownloadString()).thenReturn(allLinkAsString);
		when(phexCore.getDownloaderTimeout()).thenReturn(10);
		when(downloadHelper.lookForSources(phexSearcher.getAllSearchResults(), phexDownload.getFileHashcode())).thenReturn(
				true);

		phexDownloader.new FindSourcesTask(DOWNLOAD_ID).run();

		verify(phexDownload).setState(DownloadState.ReadyToDownload);
		verify(phexSearcher, never()).search(keyword);
		verify(scheduler, never()).schedule(isA(Runnable.class), eq(10L), eq(TimeUnit.SECONDS));
	}

	@Test
	public void ShouldNotSendMoreSourcesNeededOnCheckIfDownloading() throws Exception {
		phexDownloader.downloadIdToPhexDownload.put(DOWNLOAD_ID, phexDownload);
		when(phexDownload.getStartTimeStamp()).thenReturn(System.currentTimeMillis());

		phexDownloader.checkIfDownloading();

		assertFalse(phexDownloader.downloadIdToPhexDownload.isEmpty());
	}

	@Test
	public void ShouldSendMoreSourcesNeededOnCheckIfDownloading() throws Exception {
		long twoMinutesInMiliseconds = 2 * 60 * 1000;
		when(phexDownload.getStartTimeStamp()).thenReturn(System.currentTimeMillis() - twoMinutesInMiliseconds);
		when(phexDownload.getFreeNodes()).thenReturn(0);
		phexDownloader.downloadIdToPhexDownload.put(DOWNLOAD_ID, phexDownload);

		phexDownloader.checkIfDownloading();

		verify(phexDownload).getFreeNodes();
		verify(phexDownload).setState(DownloadState.MoreSourcesNeeded);
	}

	@Test
	public void ShouldNotSendMoreSourcesNeededOnCheckIfDownloadingTwice() throws Exception {
		long twoMinutesInMiliseconds = 2 * 60 * 1000;
		when(phexDownload.getStartTimeStamp()).thenReturn(System.currentTimeMillis() - twoMinutesInMiliseconds);
		when(phexDownload.getState()).thenReturn(DownloadState.Downloading).thenReturn(DownloadState.MoreSourcesNeeded);
		when(phexDownload.getFreeNodes()).thenReturn(0);

		phexDownloader.downloadIdToPhexDownload.put(DOWNLOAD_ID, phexDownload);

		phexDownloader.checkIfDownloading();
		phexDownloader.checkIfDownloading();

		verify(phexDownload, times(1)).getFreeNodes();
		verify(phexDownload, times(1)).setState(DownloadState.MoreSourcesNeeded);
	}

}
