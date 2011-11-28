package com.all.rest.beans.readers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;

import org.junit.Test;

import com.all.shared.json.JsonConverter;
import com.all.shared.model.RemoteTrack;
import com.all.shared.model.Track;

public class TestTopHundredTrackJsonReader {

	private static final String TR1_JSON = "{\"VBR\":false,\"album\":\"Platin Das Album Der Megastars\",\"artist\":\"Sasha\",\"bitRate\":\"190\",\"duration\":198,\"enabled\":false,\"extraKeywords\":[],\"fileFormat\":\"mp3\",\"fileName\":\"Lucky Day.mp3\",\"genre\":\"\",\"hashcode\":\"650b0e286c136ce0c88fc261cd526228ec0e2f47\",\"keywords\":[\"der\",\"das\",\"platin\",\"lucky\",\"album\",\"megastars\",\"sasha\",\"day\"],\"name\":\"Lucky Day\",\"newContent\":false,\"playcount\":0,\"rating\":0,\"sampleRate\":\"44100\",\"size\":4739322,\"skips\":0,\"year\":\"2007\"}";
	private static final String TR2_JSON = "{\"VBR\":false,\"album\":\"Platin Das Album Der Megastars\",\"artist\":\"No Angels\",\"bitRate\":\"209\",\"duration\":210,\"enabled\":false,\"extraKeywords\":[],\"fileFormat\":\"mp3\",\"fileName\":\"Goodbye To Yesterday.mp3\",\"genre\":\"\",\"hashcode\":\"dc9936813d61fbf9472f45f72b566f36efeb79ab\",\"keywords\":[\"to\",\"der\",\"das\",\"platin\",\"no\",\"album\",\"yesterday\",\"goodbye\",\"megastars\",\"angels\"],\"name\":\"Goodbye To Yesterday\",\"newContent\":false,\"playcount\":0,\"rating\":0,\"sampleRate\":\"44100\",\"size\":5504852,\"skips\":0,\"year\":\"2007\"}";
	public static final String JSON = "[" + TR1_JSON + "," + TR2_JSON + "]";

	@SuppressWarnings("unchecked")
	@Test
	public void shouldTestReaderAsItShouldWork() throws Exception {
		ArrayList collection = JsonConverter.toTypedCollection(JSON, ArrayList.class, RemoteTrack.class);
		assertEquals(2, collection.size());
		assertTrack1((Track) collection.get(0));
		assertTrack2((Track) collection.get(1));
	}

	private void assertTrack1(Track track) {
		assertFalse(track.isVBR());
		assertEquals("Platin Das Album Der Megastars", track.getAlbum());
		assertEquals("Sasha", track.getArtist());
		assertEquals("190", track.getBitRate());
		assertEquals(198, track.getDuration());
		assertFalse(track.isEnabled());
		// assertTrue(track.getExtraKeywords().isEmpty());
		assertEquals("mp3", track.getFileFormat());
		assertEquals("Lucky Day.mp3", track.getFileName());
		assertEquals("", track.getGenre());
		assertEquals("650b0e286c136ce0c88fc261cd526228ec0e2f47", track.getHashcode());
		// assertArrayEquals(new Object[] { "der", "das", "platin", "lucky", "album", "megastars", "sasha", "day" },
		// track.getKeywords().toArray());
		assertEquals("Lucky Day", track.getName());
		assertFalse(track.isNewContent());
		assertEquals(0, track.getPlaycount());
		assertEquals(0, track.getRating());
		assertEquals("44100", track.getSampleRate());
		assertEquals(4739322L, track.getSize());
		assertEquals(0, track.getSkips());
		assertEquals("2007", track.getYear());
	}

	private void assertTrack2(Track track) {
		assertFalse(track.isVBR());
		assertEquals("Platin Das Album Der Megastars", track.getAlbum());
		assertEquals("No Angels", track.getArtist());
		assertEquals("209", track.getBitRate());
		assertEquals(210, track.getDuration());
		assertFalse(track.isEnabled());
		// assertTrue(track.getExtraKeywords().isEmpty());
		assertEquals("mp3", track.getFileFormat());
		assertEquals("Goodbye To Yesterday.mp3", track.getFileName());
		assertEquals("", track.getGenre());
		assertEquals("dc9936813d61fbf9472f45f72b566f36efeb79ab", track.getHashcode());
		// assertArrayEquals(new Object[] { "to", "der", "das", "platin", "no", "album", "yesterday", "goodbye",
		// "megastars", "angels" }, track
		// .getKeywords().toArray());
		assertEquals("Goodbye To Yesterday", track.getName());
		assertFalse(track.isNewContent());
		assertEquals(0, track.getPlaycount());
		assertEquals(0, track.getRating());
		assertEquals("44100", track.getSampleRate());
		assertEquals(5504852L, track.getSize());
		assertEquals(0, track.getSkips());
		assertEquals("2007", track.getYear());

	}

}
