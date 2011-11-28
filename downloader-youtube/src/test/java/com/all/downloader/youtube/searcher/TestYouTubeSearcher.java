package com.all.downloader.youtube.searcher;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.all.downloader.search.SearchData;
import com.all.downloader.search.SearchDataEvent;
import com.all.downloader.search.SearchErrorEvent;
import com.all.downloader.search.SearchProgressEvent;
import com.all.downloader.search.SearcherListener;
import com.google.gdata.client.youtube.YouTubeQuery;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.TextConstruct;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.data.youtube.YouTubeMediaContent;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.util.ServiceException;


public class TestYouTubeSearcher {
	
	@InjectMocks
	private YouTubeSearcher youTubeSearcher = new YouTubeSearcher();
	@Mock
	private YouTubeService service;
	@Mock
	private YouTubeQuery query;
	@Mock
	private VideoFeed videoFeed;
	@Mock
	private VideoEntry videoEntry;
	@Mock
	private SearcherListener searcherListener;
	@Captor
	private ArgumentCaptor<SearchDataEvent> searchDataEventCaptor;
	@Captor
	private ArgumentCaptor<SearchProgressEvent> searchProgressEventCaptor;
	
	private String keyword = "keyword";

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		
		youTubeSearcher.addSearcherListener(searcherListener);
	}
	
	@After
	public void tearDown() {
		youTubeSearcher.removeSearcherListener(searcherListener);
		youTubeSearcher.destroy();
	}
	
	@Test
	public void shouldSearch() throws Exception {
		String title = "title";
		String videoId = "videoId";
		TextConstruct textConstruct = mock(TextConstruct.class);
		YouTubeMediaGroup mediaGroup = mock(YouTubeMediaGroup.class);
		YouTubeMediaContent mediaContent = mock(YouTubeMediaContent.class);

		when(service.query(query, VideoFeed.class)).thenReturn(videoFeed);
		when(videoFeed.getEntries()).thenReturn(Arrays.asList(videoEntry));
		when(videoEntry.getTitle()).thenReturn(textConstruct);
		when(textConstruct.getPlainText()).thenReturn(title);
		when(videoEntry.getMediaGroup()).thenReturn(mediaGroup);
		when(mediaGroup.getVideoId()).thenReturn(videoId);
		when(mediaGroup.getYouTubeContents()).thenReturn(Arrays.asList(mediaContent));
		when(mediaContent.getDuration()).thenReturn(400);
		
		youTubeSearcher.search(keyword);
		
		verify(query, timeout(100000)).setFullTextQuery(keyword);
		verify(searcherListener, timeout(100000)).updateSearchData(searchDataEventCaptor.capture());
		verify(searcherListener, timeout(100000)).updateProgress(searchProgressEventCaptor.capture());
		
		SearchDataEvent searchDataEvent = searchDataEventCaptor.getValue();
		SearchProgressEvent searchProgressEvent = searchProgressEventCaptor.getValue();
		
		assertEquals(keyword, searchDataEvent.getKeywordSearch());
		assertEquals(youTubeSearcher, searchDataEvent.getSource());
		SearchData searchData = searchDataEvent.getSearchData();
		assertEquals(videoId, searchData.getFileHash());
		assertEquals("MP3", searchData.getFileType());
		assertEquals(title, searchData.getName());
		assertEquals(10, searchData.getPeers());
		assertEquals(2913801, searchData.getSize());
		assertEquals(SearchData.Source.YOUTUBE, searchData.getSource());
		assertEquals(keyword, searchProgressEvent.getKeywordSearch());
		assertEquals(youTubeSearcher, searchProgressEvent.getSource());
		assertEquals(100, searchProgressEvent.getProgress());
	}

	@Test
	public void shouldNotifyErrorOnIOException() throws Exception {
		when(service.query(query, VideoFeed.class)).thenThrow(new IOException());
		
		youTubeSearcher.search(keyword);
		
		verify(searcherListener, timeout(100000)).onError(any(SearchErrorEvent.class));
	}
	
	@Test
	public void shouldNotifyErrorOnServiceException() throws Exception {
		when(service.query(query, VideoFeed.class)).thenThrow(new ServiceException(""));
		
		youTubeSearcher.search(keyword);
		
		verify(searcherListener, timeout(100000)).onError(any(SearchErrorEvent.class));
	}
	
	@Test
	public void shouldNotifyErrorOnAnyExcpetion() throws Exception {
		when(service.query(query, VideoFeed.class)).thenThrow(new RuntimeException());
		
		youTubeSearcher.search(keyword);
		
		verify(searcherListener, timeout(100000)).onError(any(SearchErrorEvent.class));
	}
	
	
}
