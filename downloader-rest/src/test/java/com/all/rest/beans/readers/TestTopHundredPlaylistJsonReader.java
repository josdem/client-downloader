package com.all.rest.beans.readers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import com.all.rest.beans.TopHundredPlaylist;
import com.all.shared.json.JsonConverter;
import com.all.shared.model.Playlist;

public class TestTopHundredPlaylistJsonReader {
	private static final String PL_JSON = "{\"creationDate\":1304018091000,\"expired\":false,\"hashcode\":\"adbb1345e50afb7529c59ab63cd7cacc9bc1f8f9\",\"modifiedDate\":1304018091000,\"name\":\"topplaylist1\"}";

	public static final String JSON = "[" + PL_JSON + "]";

	@Test
	public void shouldConvertPlaylist() throws Exception {
		TopHundredPlaylist category = new TopHundredPlaylistJsonReader().read(PL_JSON);
		assertPlaylist(category);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldTestReaderAsItShouldWork() throws Exception {
		JsonConverter.addJsonReader(TopHundredPlaylist.class, new TopHundredPlaylistJsonReader());
		ArrayList collection = JsonConverter.toTypedCollection(JSON, ArrayList.class, TopHundredPlaylist.class);
		assertEquals(1, collection.size());
		assertPlaylist((TopHundredPlaylist) collection.get(0));
	}

	private void assertPlaylist(Playlist playlist) {
		assertEquals(new Date(1304018091000L), playlist.getCreationDate());
		assertEquals("adbb1345e50afb7529c59ab63cd7cacc9bc1f8f9", playlist.getHashcode());
		assertEquals(new Date(1304018091000L), playlist.getLastPlayed());
		assertEquals(new Date(1304018091000L), playlist.getModifiedDate());
		assertEquals("topplaylist1", playlist.getName());
		assertEquals("top", playlist.getOwner());
		assertNull(playlist.getParentFolder());
		assertTrue(playlist.isEmpty());
		assertTrue(playlist.isNewContent());
		assertFalse(playlist.isSmartPlaylist());
	}
}
