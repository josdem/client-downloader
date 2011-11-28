package com.all.download.manager.search;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.isA;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.all.download.manager.UnitTestCase;
import com.all.download.manager.search.SearcherManager.ManagedSearchFactory;
import com.all.downloader.search.ManagedSearcher;
import com.all.downloader.search.SearchDataEvent;
import com.all.downloader.search.SearchErrorEvent;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.SearcherListener;
import com.all.messengine.MessEngine;
import com.all.shared.model.AllMessage;

public class TestSearcherManager extends UnitTestCase {

	@InjectMocks
	private SearcherManager searcherManager = new SearcherManager();
	@Mock
	private ManagedSearchFactory managedSearchFactory;
	@Mock
	private SearcherListener listenerA;
	@Mock
	private SearcherListener listenerB;
	@Mock
	private Collection<ManagedSearcher> searcherCollection = new ArrayList<ManagedSearcher>();
	@Mock
	private MessEngine messEngine;

	private String keyword = "keyword";

	@Before
	public void setup() {
		searcherManager.addSearcherListener(listenerA);
		searcherManager.addSearcherListener(listenerB);
	}

	@Test
	public void shouldInitializeWithNoSearchers() throws Exception {
		searcherManager.initialize();
	}
	
	@Test
	public void shouldinitializeWithSearchers() throws Exception {
		searcherCollection.add(mock(ManagedSearcher.class));
		
		searcherManager.initialize();
	}

	@Test
	public void shouldCreateAndStartManagedSearch() throws Exception {
		ManagedSearch managedSearch = mock(ManagedSearch.class);

		when(managedSearchFactory.create(keyword)).thenReturn(managedSearch);

		searcherManager.search(keyword);

		verify(managedSearch).search();
		verify(messEngine).send(isA(AllMessage.class));
	}

	@Test
	public void shouldNotifySearchData() throws Exception {
		SearchDataEvent searchDataEvent = mock(SearchDataEvent.class);

		searcherManager.updateSearchData(searchDataEvent);

		verify(listenerA).updateSearchData(searchDataEvent);
		verify(listenerB).updateSearchData(searchDataEvent);
	}

	@Test
	public void shouldNotifyProgress() throws Exception {
		SearchProgressEvent searchProgressEvent = mock(SearchProgressEvent.class);

		searcherManager.updateProgress(searchProgressEvent);

		verify(listenerA).updateProgress(searchProgressEvent);
		verify(listenerB).updateProgress(searchProgressEvent);
	}

	@Test
	public void shouldNotifyError() throws Exception {
		SearchErrorEvent searchErrorEvent = mock(SearchErrorEvent.class);

		searcherManager.onError(searchErrorEvent);

		verify(listenerA).onError(searchErrorEvent);
		verify(listenerB).onError(searchErrorEvent);
	}

	@Test
	public void shouldCreateManagedSearch() throws Exception {
		SearcherManager.ManagedSearchFactory managedSearchFactory = searcherManager.new ManagedSearchFactory();
		ManagedSearch managedSearch = managedSearchFactory.create(keyword);
		assertNotNull(managedSearch);
	}
}
