package com.all.downloader.p2p.phexcore.helper;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import phex.common.URN;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.msg.GUID;
import phex.query.QueryHitHost;
import phex.utils.FileUtils;

import com.all.downloader.download.DownloadException;
import com.all.downloader.p2p.phexcore.PhexCore;
import com.all.downloader.p2p.phexcore.download.PhexDownload;
import com.all.downloader.search.SearchException;

/**
 * understands a download helper to simplify/abstract download process
 */
@Component
public class DownloadHelper {
	private final Log log = LogFactory.getLog(this.getClass());

	@Autowired
	private PhexCore phexCore;

	// Phex will set the correct value when we receive a message from the host
	private GUID hostGuid = null;
	private int hostSpeed = -1;
	// Safe to use any value, as long as the URN is provided
	// see javadoc in phex.query.FilteredQueryResponseMonitor.findQueryHit()
	// or see javadoc in phex.query.SearchResultHolder.findQueryHit()
	private int fileIndex = -1;
	// At the moment we are not supporting possible file path information.
	private String pathInfo = "";
	// At the moment we are not using any fileMetadata.
	private String fileMetadata = "";
	// For GUI purposes, how well a search term matches the result from 0 to 100
	private short score = 100;

	public void addCandidateToDownload(String host, PhexDownload phexDownload) {
		if (StringUtils.isEmpty(phexDownload.getFileHashcode())) {
			throw new IllegalArgumentException();
		}
		URN urn = new URN("urn:sha1:" + phexDownload.getFileHashcode());

		DefaultDestAddress defaultDestAddress = convertInetToDefaultDest(host);
		QueryHitHost basicQueryHitHost = new QueryHitHost(hostGuid, defaultDestAddress, hostSpeed);
		basicQueryHitHost.setPushProxyAddresses(new DestAddress[] {});

		// remove special characters
		String fileNameAndExtension = phexDownload.getFileNameAndExtension();
		fileNameAndExtension = FileUtils.convertToLocalSystemFilename(fileNameAndExtension);

		RemoteFile remoteFile = new RemoteFile(basicQueryHitHost, fileIndex, fileNameAndExtension, pathInfo,
				phexDownload.getSize(), urn, fileMetadata, (short) score);
		remoteFile.setInDownloadQueue(true);

		SWDownloadFile downloadFile = phexCore.getDownloadFileByURN(urn);
		if (downloadFile != null) {
			log.debug("adding a Candidate for downloadFile: " + downloadFile);
			phexDownload.setDownloadFile(downloadFile);
			downloadFile.addDownloadCandidate(remoteFile);
		} else {
			createNewDownload(remoteFile, phexDownload);
		}
	}

	private DefaultDestAddress convertInetToDefaultDest(String host) {
		return new DefaultDestAddress(host, PhexCore.PORT);
	}

	private void createNewDownload(RemoteFile remoteFile, PhexDownload phexDownload) {
		log.debug("name File from phexDownload: " + phexDownload.getFileNameAndExtension());
		String filename = phexDownload.getFileNameAndExtension();
		
		String searchTerm = phex.utils.StringUtils.createNaturalSearchTerm(filename);
		
		SWDownloadFile downloadFile = phexCore.addFileToDownload(remoteFile, filename, searchTerm);
		phexDownload.setDownloadFile(downloadFile);

		// if we leave search term with <2 characters will do a hashcode search
		// but not recommended as limewire clients disabled the hashcode flood
		// search, instead limewire uses mojito DHT to find more sources
		// downloadFile.getResearchSetting().setSearchTerm("");
		downloadFile.startSearchForCandidates();
	}

	public void lookForCandidatesFromSearchResults(List<RemoteFile> allSearchResults, PhexDownload phexDownload) throws SearchException {
		String fileHashCode = phexDownload.getFileHashcode();
		verifyIfAllSearchResultEmpty(allSearchResults);
		for (RemoteFile remoteFile : allSearchResults) {
			String urnsha = remoteFile.getURN().getAsString();
			if (urnsha.endsWith(fileHashCode)) {
				download(phexDownload, remoteFile);
			}
		}
	}
	
	public boolean lookForSources(List<RemoteFile> allSearchResults, String fileHashCode) throws SearchException, DownloadException{
		if(fileHashCode == null){
			throw new DownloadException("gnutella urnsha is null, not a gnutella download");
		}
		verifyIfAllSearchResultEmpty(allSearchResults);
		for (RemoteFile remoteFile : allSearchResults) {
			String urnsha = remoteFile.getURN().getAsString();
			if (urnsha.endsWith(fileHashCode)) {
				return true;
			}
		}
		return false;
	}

	private void verifyIfAllSearchResultEmpty(List<RemoteFile> allSearchResults) throws SearchException {
		if(allSearchResults.isEmpty()){
			throw new SearchException("There are no peer(s) to do a download");
		}
	}

	void download(PhexDownload phexDownload, RemoteFile remoteFile) {
		SWDownloadFile downloadFile = phexDownload.getDownloadFile();
		if (downloadFile == null) {
			log.info(String.format("Adding FirstCandidate: %s %s %s", remoteFile.getDisplayName(), remoteFile.getHostAddress(), remoteFile.getURN().getAsString()));
			createNewDownload(remoteFile, phexDownload);
		} else {
			boolean result = downloadFile.addDownloadCandidate(remoteFile);
			log.info(String.format("Added candidate %b %s %s %s", result, remoteFile.getDisplayName(),
					remoteFile.getHostAddress(), remoteFile.getURN().getAsString()));
		}
	}

	

}
