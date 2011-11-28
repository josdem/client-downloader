package com.all.download.manager.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import com.all.download.manager.download.DownloaderManager.ManagedDownloadFactory;
import com.all.downloader.bean.DownloadState;
import com.all.downloader.bean.DownloadStatus;
import com.all.downloader.download.DownloadCompleteEvent;
import com.all.downloader.download.DownloadException;
import com.all.downloader.download.DownloadUpdateEvent;
import com.all.downloader.download.DownloaderListener;
import com.all.downloader.download.ManagedDownloader;
import com.all.testing.MockInyectRunner;
import com.all.testing.Stub;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestDownloaderManager {
	@UnderTest
	private DownloaderManager downloaderManager;
	@Mock
	private ManagedDownloader managedDownloader0;
	@Mock
	private ManagedDownloader managedDownloader1;
	@Mock
	private ManagedDownloader managedDownloader2;
	@Mock
	private ManagedDownloadFactory managedDownloadFactory;
	@Mock
	private ManagedDownload managedDownload;
	@Mock
	private DownloaderListener downloaderListener;
	@Stub
	private Collection<ManagedDownloader> managedDownloaders = new ArrayList<ManagedDownloader>();
	@Stub
	private final Map<String, ManagedDownload> currentDownloads = new HashMap<String, ManagedDownload>();
	
	String downloadId = "downloadId";

	@Before
	public void setup() {
		when(managedDownloader0.getDownloaderPriority()).thenReturn(0);
		when(managedDownloader1.getDownloaderPriority()).thenReturn(1);
		when(managedDownloader2.getDownloaderPriority()).thenReturn(2);

		managedDownloaders.add(managedDownloader1);
		managedDownloaders.add(managedDownloader0);
		managedDownloaders.add(managedDownloader2);
		
		downloaderManager.addDownloaderListener(downloaderListener);
	}
	
	@Test
	public void shouldValidateDownloaderPriorities() throws Exception {
		downloaderManager.validateDownloaderPriorities();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void shouldFailIfNoManagedDownloaderReceived() throws Exception {
		managedDownloaders.clear();
		
		downloaderManager.validateDownloaderPriorities();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void shouldNotAddManageddownloaderWithSamePriority() throws Exception {
		when(managedDownloader2.getDownloaderPriority()).thenReturn(1);
		
		downloaderManager.validateDownloaderPriorities();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void shouldNotAcceptNotSequentialDownloaderPriorities() throws Exception {
		when(managedDownloader2.getDownloaderPriority()).thenReturn(3);
		
		downloaderManager.validateDownloaderPriorities();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void shouldNotAcceptDownloaderPrioritySartingAtOne() throws Exception {
		when(managedDownloader0.getDownloaderPriority()).thenReturn(1);
		when(managedDownloader1.getDownloaderPriority()).thenReturn(2);
		when(managedDownloader2.getDownloaderPriority()).thenReturn(3);
		
		downloaderManager.validateDownloaderPriorities();
	}
	
	@Test
	public void shouldDownload() throws Exception {
		when(managedDownloadFactory.create(downloadId)).thenReturn(managedDownload);
		
		downloaderManager.download(downloadId);
		
		assertEquals(managedDownload, currentDownloads.get(downloadId));
		verify(managedDownload).download();
	}
	
	@Test(expected=DownloadException.class)
	public void shouldNotDownloadIfAlreadyDownloading() throws Exception {
		currentDownloads.put(downloadId, managedDownload);
		
		downloaderManager.download(downloadId);
	}
	
	@Test
	public void shouldDelete() throws Exception {
		currentDownloads.put(downloadId, managedDownload);
		downloaderManager.delete(downloadId);
		verify(managedDownload).delete();
	}
	
	@Test(expected=DownloadException.class)
	public void shouldNotDeleteBecasueDownloadDoesNotExist() throws Exception {
		downloaderManager.delete(downloadId);
	}
	
	@Test
	public void shouldPause() throws Exception {
		currentDownloads.put(downloadId, managedDownload);
		downloaderManager.pause(downloadId);
		verify(managedDownload).pause();
	}
	
	@Test(expected=DownloadException.class)
	public void shouldNotPauseBecasueDownloadDoesNotExist() throws Exception {
		downloaderManager.pause(downloadId);
	}
	
	@Test
	public void shouldResume() throws Exception {
		currentDownloads.put(downloadId, managedDownload);
		downloaderManager.resume(downloadId);
		verify(managedDownload).resume();
	}
	
	@Test(expected=DownloadException.class)
	public void shouldNotResumeBecasueDownloadDoesNotExist() throws Exception {
		downloaderManager.resume(downloadId);
	}
	
	@Test
	public void shouldProcessEventAndPropagateDownloadUpdateEvent() throws Exception {
		DownloadUpdateEvent downloadUpdateEvent = mock(DownloadUpdateEvent.class);
		DownloadStatus downloadStatus = mock(DownloadStatus.class);
		
		currentDownloads.put(downloadId, managedDownload);
		when(downloadUpdateEvent.getDownloadStatus()).thenReturn(downloadStatus);
		when(downloadStatus.getState()).thenReturn(DownloadState.MoreSourcesNeeded);
		when(downloadUpdateEvent.getDownloadId()).thenReturn(downloadId);
		
		downloaderManager.onDownloadUpdated(downloadUpdateEvent);
		
		assertFalse(currentDownloads.containsKey(downloadId));
		verify(downloaderListener).onDownloadUpdated(downloadUpdateEvent);
	}
	
	@Test
	public void shouldRemoveDownloadenAndPropagateDownloadCompleteEvent() throws Exception {
		DownloadCompleteEvent downloadCompleteEvent = mock(DownloadCompleteEvent.class);
		
		currentDownloads.put(downloadId, managedDownload);
		when(downloadCompleteEvent.getDownloadId()).thenReturn(downloadId);
		
		downloaderManager.onDownloadCompleted(downloadCompleteEvent);
		
		assertFalse(currentDownloads.containsKey(downloadId));
		verify(downloaderListener).onDownloadCompleted(downloadCompleteEvent);
	}
	
	@Test
	public void shouldIncreaseCoverage() throws Exception {
		DownloaderManager.ManagedDownloadFactory managedDownloadFactory = downloaderManager.new ManagedDownloadFactory();
		managedDownloadFactory.create(downloadId);
	}
}
