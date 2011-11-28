package com.all.downloader.download;

import java.io.File;
import java.util.EventObject;

public class DownloadCompleteEvent extends EventObject {
	private static final long serialVersionUID = -3259889825200492316L;
	
	String downloadId;
	File destinationFile;


	public DownloadCompleteEvent(Object source, String downloadId, File destinationFile) {
		super(source);
		this.downloadId = downloadId;
		this.destinationFile = destinationFile;
	}

	public String getDownloadId() {
		return downloadId;
	}
	
	public File getDestinationFile() {
		return destinationFile;
	}

}
