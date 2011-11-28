package com.all.downloader.youtube.searcher;

import com.all.downloader.alllink.AllLink;
import com.all.downloader.search.SearchData;
import com.all.shared.model.RemoteTrack;
import com.all.shared.model.Track;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.YouTubeMediaGroup;

public class YouTubeSearchData implements SearchData {

	private String name;
	private String filehash;
	private long size;

	public YouTubeSearchData(VideoEntry videoEntry) {
		this.name = videoEntry.getTitle().getPlainText();
		YouTubeMediaGroup mediaGroup = videoEntry.getMediaGroup();
		this.filehash = mediaGroup.getVideoId();
		// all YouTubeContents have the same duration, it's safe to take the first one
		int duration = mediaGroup.getYouTubeContents().get(0).getDuration();
		this.size = (long) (duration * 7284.502994011976); // an aproximation of the filesize
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFileType() {
		return "MP3";
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public int getPeers() {
		return 0;
	}

	@Override
	public String getFileHash() {
		return filehash;
	}

	@Override
	public Track toTrack() {
		RemoteTrack remoteTrack = new RemoteTrack();
		remoteTrack.setHashcode(filehash);
		remoteTrack.setName(name);
		remoteTrack.setDownloadString(new AllLink(null, filehash).toString());
		remoteTrack.setSize(size);
		remoteTrack.setFileFormat(getFileType());
		return remoteTrack;
	}

	@Override
	public Source getSource() {
		return Source.YOUTUBE;
	}

}
