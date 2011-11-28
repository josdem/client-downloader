package com.all.downloader.p2p.phexcore.event;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.download.SearchSourcesEvent;
import com.all.downloader.p2p.phexcore.BasePhexTestCase;
import com.all.downloader.p2p.phexcore.download.PhexDownload;
import com.all.downloader.p2p.phexcore.download.PhexDownloader;
import com.all.downloader.p2p.phexcore.event.SearcherSourcesListener;
import com.all.downloader.search.SearchData;
import com.all.downloader.search.SearchDataEvent;


public class TestSearcherSourcesListener extends BasePhexTestCase{
	@Mock
	private PhexDownloader phexDownloader;
	@Mock
	private PhexDownload phexDownload;
	@Mock
	private SearchDataEvent updateSearchEvent;
	@Mock
	private SearchData searchData;
	@Mock
	private ScheduledFuture<?> schedule;

	private String keyword = "keyword";
	private String downloadId = "downloadId";
	private String fileHash = "fileHash";
	
	private SearcherSourcesListener listener;

	@Before
	public void setup() throws Exception {
		listener = new SearcherSourcesListener(phexDownloader, keyword, downloadId, phexDownload);
		listener.setFuture(schedule);
		when(updateSearchEvent.getSearchData()).thenReturn(searchData);
	}
	
	@Test
	public void shouldSendReadyToDownloadEvent() throws Exception {
		when(searchData.getFileHash()).thenReturn(fileHash);
		when(phexDownload.getFileHashcode()).thenReturn(fileHash);
		
		listener.updateSearchData(updateSearchEvent);
		
		verify(schedule).cancel(true);
		verify(phexDownload).setState(DownloadState.ReadyToDownload);
		verify(phexDownloader).notifySearchSourcesResult(isA(SearchSourcesEvent.class));
	}
	
	@Test
	public void shouldNotSendReadyToDownloadEvent() throws Exception {
		when(searchData.getFileHash()).thenReturn(fileHash);
		when(phexDownload.getFileHashcode()).thenReturn("another");
		
		listener.updateSearchData(updateSearchEvent);
		
		verify(schedule, never()).cancel(true);
		verify(phexDownload, never()).setState(DownloadState.ReadyToDownload);
		verify(phexDownloader, never()).notifySearchSourcesResult(isA(SearchSourcesEvent.class));
	}
}
