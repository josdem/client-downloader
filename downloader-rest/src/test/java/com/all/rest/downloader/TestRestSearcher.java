package com.all.rest.downloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.all.downloader.search.SearchData;
import com.all.downloader.search.SearchDataEvent;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.SearcherListener;
import com.all.rest.web.RestService;
import com.all.shared.mc.TrackSearchResult;
import com.all.shared.model.RemoteTrack;

public class TestRestSearcher {

	@InjectMocks
	private RestSearcher searcher = new RestSearcher();
	@Mock
	private RestService restService;
	@Mock
	private SearcherListener listener;
	@Captor
	private ArgumentCaptor<SearchDataEvent> searchDataCaptor;

	private String keyword = "Some keyword";

	@Before
	public void setup() {
		initMocks(this);

		searcher.addSearcherListener(listener);
	}

	@Test
	public void shouldSearchByKeyword() throws Exception {
		RemoteTrack track = new RemoteTrack();
		track.setHashcode("12345678");
		track.setName("trackName");
		track.setFileFormat("mp3");
		track.setSize(1024 * 1024 * 4);
		@SuppressWarnings("deprecation")
		TrackSearchResult result = new TrackSearchResult(track, 4, 5);
		List<TrackSearchResult> results = Arrays.asList(new TrackSearchResult[] { result });
		when(restService.findTracksByKeyword(keyword)).thenReturn(results);

		searcher.search(keyword);

		verify(listener, timeout(1000).times(2)).updateProgress(any(SearchProgressEvent.class));
		verify(listener, timeout(1000)).updateSearchData(searchDataCaptor.capture());
		SearchDataEvent event = searchDataCaptor.getValue();
		assertNotNull(event);

		assertEquals(keyword, event.getKeywordSearch());
		assertEquals(searcher, event.getSource());
		SearchData searchData = event.getSearchData();
		assertNotNull(searchData);
		assertEquals(track.getHashcode(), searchData.getFileHash());
		assertEquals(track.getName(), searchData.getName());
		assertEquals(track.getFileFormat(), searchData.getFileType());
		assertEquals(track.getSize(), searchData.getSize());
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailWithInvalidKeyword() throws Exception {
		searcher.search(" ");
	}

}