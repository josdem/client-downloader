package com.all.rest.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import com.all.rest.beans.RestTrackStatus;
import com.all.rest.beans.TopHundredCategory;
import com.all.rest.beans.readers.TestTopHundredCategoryJsonReader;
import com.all.rest.beans.readers.TestTopHundredPlaylistJsonReader;
import com.all.rest.beans.readers.TestTopHundredTrackJsonReader;
import com.all.rest.config.RestClientConfig;
import com.all.shared.json.JsonConverter;
import com.all.shared.mc.TrackSearchResult;
import com.all.shared.mc.TrackStatus;
import com.all.shared.mc.TrackStatus.Status;
import com.all.shared.model.Category;
import com.all.shared.model.Playlist;
import com.all.shared.model.RemoteTrack;
import com.all.shared.model.Track;

public class TestRestService {

	@InjectMocks
	private RestService restService = new RestService();
	@Mock
	private RestClientConfig restConfig;
	@Mock
	private RestTemplate restTemplate;

	private String restPeerUrl = "serverUrl";
	private String trackId = "12345678";
	private int chunk = 2;
	private String trackerUrl = "trackerUrl";

	@Before
	public void initialize() {
		initMocks(this);
		when(restConfig.getProperty("restServerUrl")).thenReturn(restPeerUrl);
		when(restConfig.getProperty("trackerUrl")).thenReturn(trackerUrl);
		when(restTemplate.getForObject(startsWith(restPeerUrl), eq(String.class))).thenReturn("OK");
		restService.initialize();
	}

	@Test
	public void shouldGetChunk() throws Exception {
		restService.getChunk(trackId, chunk);

		verify(restTemplate).getForObject(anyString(), eq(byte[].class), eq(trackId), eq(chunk));
	}

	@Test
	public void shouldFindTracksById() throws Exception {
		List<String> ids = new ArrayList<String>();
		ids.add(trackId);
		when(restTemplate.postForObject(anyString(), eq(JsonConverter.toJson(ids)), eq(String.class))).thenReturn("[]");

		List<String> result = restService.findTracksById(ids);
		assertNotNull(result);
		assertTrue(result.isEmpty());
		verify(restTemplate).postForObject(anyString(), eq(JsonConverter.toJson(ids)), eq(String.class));
	}

	@Test
	public void shouldGetUploadRate() throws Exception {
		restService.getUploadRate();
		verify(restTemplate, times(6)).put(anyString(), any(byte[].class));
	}

	@Test
	public void shouldCancelUpload() throws Exception {
		restService.cancelUpload(trackId);
		verify(restTemplate).delete(anyString(), eq(trackId));
	}

	@Test
	public void shouldUploadMetadata() throws Exception {
		RemoteTrack track = new RemoteTrack();
		track.setHashcode(trackId);

		restService.uploadMetadata(track);

		verify(restTemplate).put(anyString(), eq(JsonConverter.toJson(track)), eq(trackId));
	}

	@Test
	public void shouldGetStatus() throws Exception {
		RestTrackStatus status = new RestTrackStatus();
		status.setTrackId(trackId);
		status.setLastChunkNumber(chunk);
		status.setTrackStatus(Status.INCOMPLETE);
		String json = JsonConverter.toJson(status);
		when(restTemplate.getForObject(anyString(), eq(String.class), eq(trackId))).thenReturn(json);
		TrackStatus result = restService.getStatus(trackId);

		assertNotNull(result);
		assertEquals(status.getTrackId(), result.getTrackId());
		assertEquals(status.getLastChunkNumber(), result.getLastChunkNumber());
		assertEquals(status.getTrackStatus(), result.getTrackStatus());
	}

	@Test
	public void shouldUploadChunk() throws Exception {
		byte[] chunkBytes = new byte[] { 1, 2, 3 };
		restService.uploadChunk(trackId, chunk, chunkBytes);

		verify(restTemplate).postForLocation(anyString(), eq(chunkBytes), eq(trackId), eq(chunk));
	}

	@Test
	public void shouldFindTracksByKeyword() throws Exception {
		String keyword = "some keyword";
		when(restTemplate.postForObject(anyString(), eq(keyword), eq(String.class))).thenReturn("[]");

		List<TrackSearchResult> result = restService.findTracksByKeyword(keyword);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	public void shouldSwitchToAnotherRestPeerIfCurrentOneIsDown() throws Exception {
		byte[] chunkBytes = new byte[] { 1, 2, 3 };

		when(restTemplate.postForLocation(startsWith(restPeerUrl), eq(chunkBytes), eq(trackId), eq(chunk)))
				.thenThrow(new RuntimeException("Server Down"));
		when(restTemplate.getForObject(startsWith(restPeerUrl), eq(String.class))).thenThrow(new RuntimeException("Server Down"));
		String otherUrl = "otherUrl";
		List<String> restPeers = new ArrayList<String>();
		restPeers.add(otherUrl);
		when(restTemplate.getForObject(startsWith(trackerUrl), eq(String.class))).thenReturn(JsonConverter.toJson(restPeers));
		try {
			restService.uploadChunk(trackId, chunk, chunkBytes);
		} catch (Exception e) {
			reset(restTemplate);
		}
		restService.uploadChunk(trackId, chunk, chunkBytes);
		verify(restTemplate).postForLocation(startsWith(otherUrl), eq(chunkBytes), eq(trackId), eq(chunk));
	}

	@Test
	public void shouldFindTopCategories() throws Exception {
		when(restTemplate.getForObject(contains("top/categories"), eq(String.class))).thenReturn(TestTopHundredCategoryJsonReader.JSON);

		List<Category> topCategories = restService.findTopCategories();
		assertEquals(1, topCategories.size());

	}

	@Test
	public void shouldFindTopPlaylists() throws Exception {
		long categoryId = 1;
		when(restTemplate.getForObject(contains("top/{categoryId}"), eq(String.class), eq(categoryId))).thenReturn(TestTopHundredPlaylistJsonReader.JSON);

		List<Playlist> topPlaylists = restService.findPlaylistsForTopCategories(new TopHundredCategory(1, "a"));
		assertEquals(1, topPlaylists.size());

	}

	@Test
	public void shouldFindTopTracks() throws Exception {
		String playlistId = "2";
		when(restTemplate.getForObject(contains("top/playlist/{playlistId}"), eq(String.class), eq(playlistId))).thenReturn(
				TestTopHundredTrackJsonReader.JSON);

		Playlist mock = Mockito.mock(Playlist.class);
		when(mock.getHashcode()).thenReturn(playlistId);
		List<Track> topCategories = restService.getTracksForPlaylist(mock);
		assertEquals(2, topCategories.size());

	}
}
