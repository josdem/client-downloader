package com.all.downloader.p2p.phexcore;

import phex.common.URN;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.query.Search;

public interface PhexCore {
	int PORT = 10012;
	
	void processPhexAnotation(Object object);
	
	Search createSearchTerm(String serchTerm);

	SWDownloadFile getDownloadFileByURN(URN urn);
	
	SWDownloadFile addFileToDownload(RemoteFile remoteFile, String filename, String searchTerm);
	
	void removeSWDownloadFile(SWDownloadFile swDownloadFile);
	
	boolean isConnected();
	
	boolean isSeedingGnutella();
	
	boolean isAnyUltraPeerConnection();

	int getDownloaderPriority();
	
	int getDownloaderTimeout();
}
