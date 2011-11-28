package com.all.rest.beans;

import com.all.rest.web.RestService;
import com.all.shared.model.Playlist;
import com.all.shared.model.Track;
import com.all.shared.model.TrackContainer;

public class TopHundredRestTrackSource implements TrackContainer {
	private final RestService service;
	private final Playlist playlist;

	public TopHundredRestTrackSource(RestService service, Playlist playlist) {
		this.service = service;
		this.playlist = playlist;
	}

	@Override
	public Iterable<Track> getTracks() {
		return service.getTracksForPlaylist(playlist);
	}

}
