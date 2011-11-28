package com.all.downloader.p2p.phexcore.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import phex.common.URN;
import phex.download.RemoteFile;

import com.all.downloader.p2p.phexcore.PhexCoreImpl;
import com.all.downloader.p2p.phexcore.helper.SearchHelper;
import com.all.downloader.p2p.phexcore.search.PhexSearcher.DownloadMonitor;
import com.all.downloader.search.SearchException;
import com.all.downloader.search.SearcherListener;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestPhexSearcher {
	private static final String KEYWORD = "KEYWORD";
	private static final String SEARCHTERM = "SEARCHTERM";
	private static final long INITIAL_DELAY = 1;
	private static final long DELAY = 1;

	@UnderTest
	private PhexSearcher searcher;
	@Mock
	private PhexCoreImpl phexCore;
	@Mock
	private SearchHelper searchHelper;
	@Mock
	private SearchInfo searchInfo;
	@Mock
	private ScheduledExecutorService scheduler;
	@Mock
	private SearcherListener searcherListener;
	@Mock
	private URN urn;

	@Test
	public void shouldVerifyCallScheduller() throws Exception {
		searcher.initialize();
		verify(scheduler).scheduleWithFixedDelay(any(DownloadMonitor.class), eq(INITIAL_DELAY), eq(DELAY),
				eq(TimeUnit.SECONDS));
	}

	@Test
	public void shouldClearSearchData() throws Exception {
		SearchInfo searchInfo = mock(SearchInfo.class);
		searcher.currentSearches.put(KEYWORD, searchInfo);

		searcher.clearSearchData(KEYWORD);

		assertTrue(searcher.currentSearches.isEmpty());
		verify(searchInfo).stopSearch();
	}

	@Test(expected = SearchException.class)
	public void shouldNotSearchIfNoUltrapeers() throws Exception {
		when(phexCore.isConnected()).thenReturn(false);

		searcher.search(KEYWORD);
	}

	@Test
	public void shouldSearch() throws Exception {
		when(phexCore.isConnected()).thenReturn(true);
		when(phexCore.createNaturalSearchTerm(KEYWORD)).thenReturn(SEARCHTERM);
		when(searchHelper.createKeywordSearch(anyString())).thenReturn(searchInfo);
		when(searchInfo.getKeyword()).thenReturn(SEARCHTERM);

		searcher.search(KEYWORD);
		assertEquals(searchInfo, searcher.currentSearches.get(SEARCHTERM));
	}

	@Test
	public void shouldUpdateSearchProgress() throws Exception {
		setSearchInfoExpectations();

		searcher.updateSearchProgress();

		verify(searchInfo).notifyProgress();
	}

	private void setSearchInfoExpectations() {
		assertTrue(searcher.currentSearches.isEmpty());
		searcher.currentSearches.put("mockSearchInfo", searchInfo);

		when(searchInfo.isFinished()).thenReturn(true);
	}

	@Test
	public void shouldNotGetAllSearchResultsIfEmpty() throws Exception {
		assertTrue(searcher.currentSearches.isEmpty());
		List<RemoteFile> result = searcher.getAllSearchResults();
		assertTrue(result.isEmpty());
	}

	@Test
	public void shouldGetAllSearchResults() throws Exception {
		List<RemoteFile> remoteList = setRemoteFileExpectations();
		assertTrue(searcher.currentSearches.isEmpty());
		when(searchInfo.getRemoteFileList()).thenReturn(remoteList);

		searcher.currentSearches.put("keywordSearch", searchInfo);
		List<RemoteFile> filteredResults = searcher.getAllSearchResults();

		assertEquals(1, filteredResults.size());
	}

	@Test
	public void shouldAddSearchSourcesListener() throws Exception {
		searcher.addSearcherListener(searcherListener);
		verify(searchHelper).addSearcherSourcesListener(searcherListener);
	}

	@Test
	public void shouldKeepSearchData() throws Exception {
		String allLinkAsString = "allLink:urnsha1=BDDMWQMX4F7WFTZTL4LVECOF6CMGB5D6";
		List<RemoteFile> remoteList = setRemoteFileExpectations();
		assertTrue(searcher.currentSearches.isEmpty());
		when(searchInfo.getRemoteFileList()).thenReturn(remoteList);
		searcher.initialize();

		searcher.currentSearches.put("keywordSearch", searchInfo);

		searcher.keepSearchData(allLinkAsString);

		assertEquals(1, searcher.keptRemoteFilesSearchInfo.getRemoteFileList().size());
	}

	private List<RemoteFile> setRemoteFileExpectations() {
		RemoteFile remoteFile = mock(RemoteFile.class);
		List<RemoteFile> remoteList = new ArrayList<RemoteFile>();
		when(urn.getAsString()).thenReturn("urn:sha1:BDDMWQMX4F7WFTZTL4LVECOF6CMGB5D6");
		when(remoteFile.getURN()).thenReturn(urn);
		remoteList.add(remoteFile);
		return remoteList;
	}
}
