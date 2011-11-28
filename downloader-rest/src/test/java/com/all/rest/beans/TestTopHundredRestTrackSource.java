package com.all.rest.beans;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.all.rest.web.RestService;
import com.all.shared.model.Playlist;
import com.all.shared.model.Track;
import com.all.shared.model.TrackContainer;

public class TestTopHundredRestTrackSource {
	@Test
	public void shouldUseRestServiceAsRealSource() throws Exception {
		RestService service = Mockito.mock(RestService.class);
		Playlist mock = Mockito.mock(Playlist.class);
		TrackContainer source = new TopHundredRestTrackSource(service, mock);
		List<Track> list = new ArrayList<Track>();
		Mockito.when(service.getTracksForPlaylist(mock)).thenReturn(list);
		assertEquals(list, source.getTracks());
	}
}
