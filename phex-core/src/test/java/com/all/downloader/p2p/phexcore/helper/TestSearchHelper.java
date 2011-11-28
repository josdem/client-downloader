package com.all.downloader.p2p.phexcore.helper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import phex.query.Search;
import phex.query.SearchContainer;
import phex.query.SearchProgress;

import com.all.downloader.p2p.phexcore.PhexCore;
import com.all.downloader.p2p.phexcore.search.SearchInfo;
import com.all.downloader.search.SearcherListener;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestSearchHelper {
	static final String SEARCHTERM = "seachTerm";

	@UnderTest
	private SearchHelper searchHelper;
	@Mock
	private PhexCore phexCore;
	@SuppressWarnings("unused")
	// injected
	@Mock
	private SearchContainer searchContainer;
	@Mock
	private Search search;
	@Mock
	private SearcherListener searcherListener;

	@Test
	public void shouldCreateKeyWordSearch() throws Exception {
		when(phexCore.createSearchTerm(SEARCHTERM)).thenReturn(search);

		SearchInfo searchInfo = searchHelper.createKeywordSearch(SEARCHTERM);

		assertNotNull(searchInfo);
		verify(search).startSearching(isA(SearchProgress.class));
		verify(phexCore).processPhexAnotation(searchInfo);
	}

	@Test
	public void shouldAddAndRemoveSearcherSourcesListener() throws Exception {
		assertTrue(searchHelper.searcherSourcesListeners.isEmpty());
		searchHelper.addSearcherSourcesListener(searcherListener);
		assertFalse(searchHelper.searcherSourcesListeners.isEmpty());
		searchHelper.removeSearcherSourcesLister(searcherListener);
		assertTrue(searchHelper.searcherSourcesListeners.isEmpty());
	}
}
