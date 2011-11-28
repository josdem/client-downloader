package com.all.downloader.search;

import com.all.shared.model.Track;

public interface SearchData {

	enum Source {
		GNUTELLA, ALL, YOUTUBE;
	}

	String getName();

	String getFileType();

	long getSize();

	int getPeers();

	String getFileHash();

	Track toTrack();

	Source getSource();
	
}
