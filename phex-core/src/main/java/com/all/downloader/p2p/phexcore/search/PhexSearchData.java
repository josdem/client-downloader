package com.all.downloader.p2p.phexcore.search;

import phex.download.RemoteFile;

import com.all.downloader.alllink.AllLink;
import com.all.downloader.search.SearchData;
import com.all.shared.model.RemoteTrack;
import com.all.shared.model.Track;

public class PhexSearchData implements SearchData {
	String fileHash;
	String name;
	int peers;
	long size;
	String type;

	public static SearchData createFrom(RemoteFile remoteFile) {
		if (remoteFile == null) {
			return null;
		}
		
		final PhexSearchData phexSearchData = new PhexSearchData();
		phexSearchData.fileHash = remoteFile.getSHA1();
		phexSearchData.type = remoteFile.getFileExt();
		phexSearchData.name = remoteFile.getDisplayName();
		phexSearchData.peers = 1;
		phexSearchData.size = remoteFile.getFileSize();

		return phexSearchData;
	}

	@Override
	public String getFileHash() {
		return fileHash;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getPeers() {
		return peers;
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public String getFileType() {
		return type;
	}

	@Override
	public Track toTrack() {
		RemoteTrack remoteTrack = new RemoteTrack();
		remoteTrack.setHashcode(fileHash);
		remoteTrack.setName(name);
		remoteTrack.setDownloadString(new AllLink(null, fileHash).toString());
		remoteTrack.setSize(size);
		remoteTrack.setFileFormat(type);
		return remoteTrack;
	}
	
	@Override
	public Source getSource() {
		return Source.GNUTELLA;
	}
	
}
