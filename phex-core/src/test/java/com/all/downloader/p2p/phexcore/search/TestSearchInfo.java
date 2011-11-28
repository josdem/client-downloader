package com.all.downloader.p2p.phexcore.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import phex.download.RemoteFile;
import phex.query.Search;
import phex.query.SearchDataEvent;

import com.all.downloader.p2p.phexcore.BasePhexTestCase;
import com.all.downloader.p2p.phexcore.helper.FilterHelper;
import com.all.downloader.search.SearchData;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.SearcherListener;

public class TestSearchInfo extends BasePhexTestCase {

	SearchInfo searchInfo;
	@Mock
	Search search;
	@Mock
	SearchDataEvent event;
	@Mock
	FilterHelper filterHelper;
	@Mock
	RemoteFile remoteFile;
	@Mock
	SearchData searchData;
	@Mock
	SearcherListener searcherListener;

	Set<SearcherListener> searcherSourcesListeners = new HashSet<SearcherListener>();

	String topic = "topic";
	private String keywordSearch = "keywordSearch";

	@Before
	public void setup() {
		
		searchInfo = createStubbedSearchInfo();

		when(filterHelper.isValid(any(RemoteFile.class))).thenReturn(true);
		
		searcherSourcesListeners.add(searcherListener);
	}

	@Test
	public void shouldReturnIfIsNotOurSearch() throws Exception {
		Search otherSearch = mock(Search.class);
		when(event.getSource()).thenReturn(otherSearch);

		searchInfo.onSearchDataEvent(topic, event);

		verify(event, never()).getSearchData();
	}

	@Test
	public void shouldReturnIfNoSearchData() throws Exception {
		when(event.getSource()).thenReturn(search);

		searchInfo.onSearchDataEvent(topic, event);

		verify(event, times(1)).getSearchData();
	}

	@Test
	public void shouldCallListenerOnce() throws Exception {
		RemoteFile[] remoteFileArray = { remoteFile };

		when(event.getSource()).thenReturn(search);
		when(event.getSearchData()).thenReturn(remoteFileArray);

		searchInfo.onSearchDataEvent(topic, event);

		assertEquals(1, searchInfo.remoteFileList.size());
		verify(searcherListener).updateSearchData(isA(com.all.downloader.search.SearchDataEvent.class));
	}

	@Test
	public void shouldStopASearch() throws Exception {
		searchInfo.stopSearch();
		verify(search).stopSearching();
	}

	@Test
	public void shouldCallThreeTimesTheListener() throws Exception {
		RemoteFile[] remoteFileArray = { remoteFile, remoteFile, remoteFile };

		when(event.getSource()).thenReturn(search);
		when(event.getSearchData()).thenReturn(remoteFileArray);

		searchInfo.onSearchDataEvent(topic, event);

		assertEquals(3, searchInfo.remoteFileList.size());
		verify(searcherListener, times(3)).updateSearchData(isA(com.all.downloader.search.SearchDataEvent.class));
	}
	
	@Test
	public void shouldAddRemoteFile() throws Exception {
		assertTrue(searchInfo.remoteFileList.isEmpty());
		searchInfo.addRemoteFile(remoteFile);
		assertFalse(searchInfo.remoteFileList.isEmpty());
	}
	
	@Test
	public void shouldNotifyProgress() throws Exception {
		searchInfo.notifyProgress();
		verify(searcherListener).updateProgress(isA(SearchProgressEvent.class));
	}

	private SearchInfo createStubbedSearchInfo() {
		return new SearchInfo(search, keywordSearch, searcherSourcesListeners) {
			SearchData createSearchData(RemoteFile remoteFile) {
				return searchData;
			};

			@Override
			FilterHelper createFilterHelper() {
				return TestSearchInfo.this.filterHelper;
			}
		};
	}
}
