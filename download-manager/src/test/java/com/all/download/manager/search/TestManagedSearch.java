package com.all.download.manager.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.all.download.manager.search.ManagedSearch.UpdateProgressTask;
import com.all.downloader.search.ManagedSearcher;
import com.all.downloader.search.SearchDataEvent;
import com.all.downloader.search.SearchErrorEvent;
import com.all.downloader.search.SearchException;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.SearcherListener;
import com.all.testing.MockInyectRunner;
import com.all.testing.Stub;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestManagedSearch {
	@UnderTest
	private ManagedSearch managedSearch;
	@Stub
	private String keyword = "keyword";
	@Mock
	private ScheduledExecutorService scheduledExecutorService;
	@Mock
	private SearcherListener searcherListener;
	@Mock
	private ManagedSearcher managedSearcherA;
	@Mock
	private ManagedSearcher managedSearcherB;

	private Collection<ManagedSearcher> searcherCollection = new ArrayList<ManagedSearcher>();

	private ArgumentCaptor<SearchErrorEvent> errorCaptor = ArgumentCaptor.forClass(SearchErrorEvent.class);

	@Before
	public void setup() {
		searcherCollection.add(managedSearcherA);
		searcherCollection.add(managedSearcherB);

		managedSearch.setSearcherListener(searcherListener);
		managedSearch.setSearchers(searcherCollection);
	}

	@Test
	public void increasingCoverage() throws Exception {
		new ManagedSearch(keyword);
	}

	@Test
	public void shouldSearch() throws Exception {
		managedSearch.search();

		verify(managedSearcherA).search(keyword);
		verify(managedSearcherA).addSearcherListener(managedSearch);

		verify(managedSearcherB).search(keyword);
		verify(managedSearcherB).addSearcherListener(managedSearch);

		verify(searcherListener, never()).onError(any(SearchErrorEvent.class));
		verify(scheduledExecutorService).schedule(isA(Runnable.class), anyLong(), isA(TimeUnit.class));
	}

	@Test
	public void shouldKeepSearchingInOtherSearchersIfOneFails() throws Exception {
		doThrow(new SearchException("")).when(managedSearcherA).search(keyword);

		managedSearch.search();

		verify(managedSearcherA, never()).addSearcherListener(managedSearch);

		verify(managedSearcherB).search(keyword);
		verify(managedSearcherB).addSearcherListener(managedSearch);

		verify(searcherListener, never()).onError(any(SearchErrorEvent.class));
		verify(scheduledExecutorService).schedule(isA(Runnable.class), anyLong(), isA(TimeUnit.class));
	}

	@Test
	public void shouldNotifyErrorIfAllSearchersFail() throws Exception {
		doThrow(new SearchException("")).when(managedSearcherA).search(keyword);
		doThrow(new SearchException("")).when(managedSearcherB).search(keyword);

		try {
			managedSearch.search();

			fail("Should have thrown excpetion above");
		} catch (SearchException se) {
			// expected
		}

		verify(managedSearcherA, never()).addSearcherListener(managedSearch);
		verify(managedSearcherB, never()).addSearcherListener(managedSearch);

		verify(searcherListener).onError(errorCaptor.capture());
		SearchErrorEvent searchErrorEvent = errorCaptor.getValue();
		assertEquals(searcherListener, searchErrorEvent.getSource());
		assertEquals(keyword, searchErrorEvent.getKeyword());
		verify(scheduledExecutorService, never()).schedule(isA(Runnable.class), anyLong(), isA(TimeUnit.class));
	}

	@Test
	public void shouldUpdateSearchData() throws Exception {
		SearchDataEvent updateSearchEvent = mock(SearchDataEvent.class);
		managedSearch.updateSearchData(updateSearchEvent);
		verify(searcherListener).updateSearchData(updateSearchEvent);
	}

	@Test
	public void shouldOmitUpdateProgressEvents() throws Exception {
		SearchProgressEvent searchProgressEvent = mock(SearchProgressEvent.class);
		managedSearch.updateProgress(searchProgressEvent);
		verify(searcherListener, never()).updateProgress(any(SearchProgressEvent.class));
	}

	@Test
	public void shouldRemoveSearcherWhenSendingError() throws Exception {
		managedSearch.search();

		SearchErrorEvent searchErrorEvent = new SearchErrorEvent(managedSearcherB, keyword);
		managedSearch.onError(searchErrorEvent);

		verify(managedSearcherB).removeSearcherListener(managedSearch);
		verify(scheduledExecutorService, never()).shutdownNow();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldStopSearchingWhenNotWorkingSearchersAvailable() throws Exception {
		ScheduledFuture updateTaskFuture = mock(ScheduledFuture.class);

		when(scheduledExecutorService.schedule(any(UpdateProgressTask.class), eq(1L), eq(TimeUnit.SECONDS)))
				.thenReturn(updateTaskFuture);
		doThrow(new SearchException("")).when(managedSearcherB).search(keyword);

		managedSearch.search();

		SearchErrorEvent searchErrorEvent = new SearchErrorEvent(managedSearcherA, keyword);

		managedSearch.onError(searchErrorEvent);

		verify(managedSearcherA).removeSearcherListener(managedSearch);
		verify(updateTaskFuture).cancel(true);
		verify(scheduledExecutorService).shutdownNow();

		verify(searcherListener).onError(errorCaptor.capture());
		SearchErrorEvent sentSearchErrorEvent = errorCaptor.getValue();
		assertEquals(searcherListener, sentSearchErrorEvent.getSource());
		assertEquals(keyword, sentSearchErrorEvent.getKeyword());
	}

	@Test
	public void shouldIgnoreEventIfSourceUnkown() throws Exception {
		ManagedSearcher managedSearcher = mock(ManagedSearcher.class);
		SearchErrorEvent searchErrorEvent = new SearchErrorEvent(managedSearcher, keyword);

		managedSearch.onError(searchErrorEvent);

		verify(managedSearcher).removeSearcherListener(managedSearch);
		verify(scheduledExecutorService, never()).shutdownNow();
		verify(searcherListener, never()).onError(any(SearchErrorEvent.class));
	}
	
	@Test
	public void shouldUpdateProgress() throws Exception {
		ArgumentCaptor<SearchProgressEvent> progressEventCaptor = ArgumentCaptor.forClass(SearchProgressEvent.class);
		UpdateProgressTask updateProgressTask = managedSearch.new UpdateProgressTask();
		int expectedProgress = 0;
		
		updateProgressTask.run();
		
		verify(searcherListener).updateProgress(progressEventCaptor.capture());
		SearchProgressEvent value = progressEventCaptor.getValue();
		assertEquals(searcherListener, value.getSource());
		assertEquals(keyword, value.getKeywordSearch());
		assertEquals(expectedProgress , value.getProgress());
		verify(scheduledExecutorService).schedule(updateProgressTask, 1, TimeUnit.SECONDS);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void shouldStopThreadAfterLifeTimeReached() throws Exception {
		ScheduledFuture updateTaskFuture = mock(ScheduledFuture.class);

		when(scheduledExecutorService.schedule(any(UpdateProgressTask.class), eq(1L), eq(TimeUnit.SECONDS)))
				.thenReturn(updateTaskFuture);

		managedSearch.search();
		
		UpdateProgressTask updateProgressTask = managedSearch.new UpdateProgressTask();

		//simulate progress advances every second at a time
		for(int i=0; i<=100; i++) {
			updateProgressTask.run();
		}

		verify(updateTaskFuture).cancel(true);
		verify(scheduledExecutorService).shutdownNow();
		verify(managedSearcherA).removeSearcherListener(managedSearch);
		verify(managedSearcherB).removeSearcherListener(managedSearch);
	}
}
