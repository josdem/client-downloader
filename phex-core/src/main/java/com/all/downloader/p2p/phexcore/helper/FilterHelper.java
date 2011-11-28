package com.all.downloader.p2p.phexcore.helper;

import phex.download.RemoteFile;

/**
 * understands a helper to PhexManager in filter process
 */

public class FilterHelper {
	private static final long ONE_MEGA = 1000000;
	private String[] goodResults = { ".mp3", ".flac", ".m4a", ".ogg", ".m4p", ".aiff", ".wav", ".au", ".m4b",
			".mp2" };

	public boolean isValid(RemoteFile remoteFile) {
		if(remoteFile == null){
			return false;
		}
		String displayName = remoteFile.getDisplayName();
		long size = remoteFile.getFileSize();
		
		return isValidName(displayName) && isValidSize(size);
	}

	private boolean isValidSize(long size) {
		return (size>=ONE_MEGA) ? true : false;
	}

	private boolean isValidName(String displayName) {
		displayName = displayName.toLowerCase();

		for (String goodKeyword : goodResults) {
			if (displayName.endsWith(goodKeyword)) {
				return true;
			}
		}
		return false;
	}
}
