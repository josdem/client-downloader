package com.all.downloader.p2p.phexcore.helper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;

import com.all.downloader.download.DownloadException;
import com.all.downloader.p2p.phexcore.PhexCore;
import com.all.downloader.p2p.phexcore.download.PhexDownload;
import com.all.downloader.search.SearchException;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestDownloadHelper {
	static final String ADDRESS = "192.168.1.200";
	static final String URN_SHA = "J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
	static final String TRACK_NAME = "trackName";
	static final int PORT = 10010;
	@UnderTest
	private DownloadHelper downloadHelper;
	@Mock
	private PhexCore phexCore;
	@Mock
	private PhexDownload phexDownload;
	@Mock
	private RemoteFile remoteFile;
	@Mock
	private URN urn;
	@Mock
	private DestAddress destAddress;
	@Mock
	private SWDownloadFile downloadFile;

	InetSocketAddress defaultDestAddres;

	private String urnsha = "someurnsha";

	@Before
	public void setup() {

		defaultDestAddres = new InetSocketAddress(ADDRESS, PORT);

		when(remoteFile.getHostAddress()).thenReturn(destAddress);
	}

	@Test
	public void shouldDownloadATrackFromALocalPeer() throws Exception {
		when(phexDownload.getFileHashcode()).thenReturn(URN_SHA);
		when(phexDownload.getFileNameAndExtension()).thenReturn(TRACK_NAME);
		URN urn = new URN("urn:sha1:" + phexDownload.getFileHashcode());
		when(phexCore.getDownloadFileByURN(urn)).thenReturn(downloadFile);

		downloadHelper.addCandidateToDownload(ADDRESS, phexDownload);

		verify(downloadFile).addDownloadCandidate(any(RemoteFile.class));
	}

	@Test
	public void shouldDownloadATrackFromSwarming() throws Exception {

		when(phexDownload.getFileHashcode()).thenReturn(URN_SHA);
		when(phexDownload.getFileNameAndExtension()).thenReturn(TRACK_NAME);
		when(phexCore.addFileToDownload(any(RemoteFile.class), anyString(), anyString())).thenReturn(downloadFile);

		downloadHelper.addCandidateToDownload(ADDRESS, phexDownload);

		verify(phexDownload).setDownloadFile(downloadFile);
		verify(downloadFile).startSearchForCandidates();
	}

	@Test
	public void shouldAddCandidateToDownloadAlthoughTheFileNameHasSpecialCharacters() throws Exception {
		String trackNameWithSpecialCharacters = "Would?";
		String fileExtension = ".mp3";
		String allLinkString = "allLink:urnsha1=" + URN_SHA;

		PhexDownload phexDownload = new PhexDownload(allLinkString);
		phexDownload.setFileName(trackNameWithSpecialCharacters);
		phexDownload.setFileExtension(fileExtension);

		when(phexCore.addFileToDownload(any(RemoteFile.class), anyString(), anyString())).thenReturn(downloadFile);

		downloadHelper.addCandidateToDownload(ADDRESS, phexDownload);

		verify(phexCore).addFileToDownload(isA(RemoteFile.class), anyString(), anyString());
	}

	@Test(expected = SearchException.class)
	public void shouldNotLookForCandidatesFromSearchResultsIfEmpty() throws Exception {
		List<RemoteFile> allSearchResults = new ArrayList<RemoteFile>();
		downloadHelper.lookForCandidatesFromSearchResults(allSearchResults, phexDownload);
	}

	@Test(expected = DownloadException.class)
	public void shouldNotFindResourcesIfUrnshaIsNull() throws Exception {
		RemoteFile remoteFile = setRemoteFileExpectations();
		List<RemoteFile> allSearchResults = new ArrayList<RemoteFile>();
		allSearchResults.add(remoteFile);

		boolean result = downloadHelper.lookForSources(allSearchResults, null);

		assertTrue(result);
	}

	@Test
	public void shouldFindResources() throws Exception {
		RemoteFile remoteFile = setRemoteFileExpectations();
		List<RemoteFile> allSearchResults = new ArrayList<RemoteFile>();
		allSearchResults.add(remoteFile);

		boolean result = downloadHelper.lookForSources(allSearchResults, urnsha);

		assertTrue(result);
	}

	@Test
	public void shouldNotFindResources() throws Exception {
		RemoteFile remoteFile = setRemoteFileExpectations();

		List<RemoteFile> allSearchResults = new ArrayList<RemoteFile>();
		allSearchResults.add(remoteFile);

		boolean result = downloadHelper.lookForSources(allSearchResults, "anotherDifferentUrnsha");

		assertFalse(result);
	}

	@Test
	public void shouldAddFistCandidateOnLookForCandidatesFromSearchResults() throws Exception {
		when(remoteFile.getURN()).thenReturn(urn);
		when(phexCore.addFileToDownload(isA(RemoteFile.class), anyString(), anyString())).thenReturn(downloadFile);

		downloadHelper.download(phexDownload, remoteFile);

		verify(phexDownload).setDownloadFile(isA(SWDownloadFile.class));
		verify(phexCore).addFileToDownload(isA(RemoteFile.class), anyString(), anyString());
		verify(downloadFile).startSearchForCandidates();
	}

	@Test
	public void shouldAddDownloadCandidateOnLookForCandidatesFromSearchResults() throws Exception {
		RemoteFile remoteFile = setRemoteFileExpectations();
		List<RemoteFile> allSearchResults = new ArrayList<RemoteFile>();
		allSearchResults.add(remoteFile);

		when(phexDownload.getDownloadFile()).thenReturn(downloadFile);

		downloadHelper.lookForCandidatesFromSearchResults(allSearchResults, phexDownload);

		verify(downloadFile).addDownloadCandidate(remoteFile);
	}

	private RemoteFile setRemoteFileExpectations() {
		when(urn.getAsString()).thenReturn(urnsha);
		when(remoteFile.getURN()).thenReturn(urn);
		when(phexDownload.getFileHashcode()).thenReturn(urnsha);
		when(destAddress.getHostName()).thenReturn("hostname");
		return remoteFile;
	}
}
