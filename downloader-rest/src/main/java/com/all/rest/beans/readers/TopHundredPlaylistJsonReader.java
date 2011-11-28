package com.all.rest.beans.readers;

import java.util.ArrayList;
import java.util.Date;

import net.sf.json.JSONObject;

import com.all.rest.beans.TopHundredPlaylist;
import com.all.shared.json.JsonConverter;
import com.all.shared.json.readers.JsonReader;
import com.all.shared.model.ListTrackContainer;
import com.all.shared.model.RemoteTrack;

public class TopHundredPlaylistJsonReader implements JsonReader<TopHundredPlaylist> {

	@SuppressWarnings("unchecked")
	@Override
	public TopHundredPlaylist read(String json) {
		JSONObject jsonModel = JSONObject.fromObject(json);
		TopHundredPlaylist playlist = new TopHundredPlaylist();
		playlist.setCreationDate(new Date(jsonModel.getLong("creationDate")));
		playlist.setExpired(jsonModel.getBoolean("expired"));
		playlist.setHashcode(jsonModel.getString("hashcode"));
		playlist.setModifiedDate(new Date(jsonModel.getLong("modifiedDate")));
		playlist.setName(jsonModel.getString("name"));
		try {
			String jsonTracks = jsonModel.getJSONArray("tracks").toString();
			ArrayList tracks = JsonConverter.toTypedCollection(jsonTracks, ArrayList.class, RemoteTrack.class);
			ListTrackContainer trackSource = new ListTrackContainer(tracks);
			playlist.setTrackSource(trackSource);
		} catch (Exception e) {
		}
		return playlist;
	}
}
