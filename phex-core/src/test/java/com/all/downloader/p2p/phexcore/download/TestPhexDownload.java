package com.all.downloader.p2p.phexcore.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mock;

import phex.common.TransferDataProvider;
import phex.download.swarming.SWDownloadFile;
import phex.gui.comparator.ETAComparator;

import com.all.downloader.bean.DownloadState;
import com.all.downloader.p2p.phexcore.BasePhexTestCase;

public class TestPhexDownload extends BasePhexTestCase {
	private static final String allLinkAsString = "allLink:urnsha1=J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
	private static final String urnsha = "J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
	private static final String hashcode = "00a9ae41a50cfece357f26e786db6fa014af765b";
	private String allLinkString = "allLink:hashcode=downloadId";

	@Mock
	SWDownloadFile downloadFile;
	
	PhexDownload phexDownload = new PhexDownload(allLinkAsString);
	
	@Mock
	private ETAComparator etaComparator;
	
	@Test
	public void shouldUpdateStatus() throws Exception {
		int progress = 50;
		int peers = 3;
		int leechers = 5;
		int statusFileQueued = 5;
		int downloadRate = 2049;
	    int timeRemaining = 250;
		
		when(downloadFile.getTransferSpeed()).thenReturn((long)downloadRate);
		when(downloadFile.getProgress()).thenReturn(progress);
		when(downloadFile.getBadCandidateCount()).thenReturn(leechers);
		when(downloadFile.getGoodCandidateCount()).thenReturn(peers);
		when(downloadFile.getStatus()).thenReturn(statusFileQueued);
		when(downloadFile.getTransferDataSize()).thenReturn(1000L);
		when(downloadFile.getDataTransferStatus()).thenReturn(SWDownloadFile.TRANSFER_RUNNING);
		when(downloadFile.getLongTermTransferRate()).thenReturn(4);
		
		phexDownload.setDownloadFile(downloadFile);

		phexDownload.updateStatus();
		
		assertEquals(downloadRate, phexDownload.getDownloadRate());
		assertEquals(progress, phexDownload.getProgress());
		assertEquals(DownloadState.Downloading, phexDownload.getState());
		assertEquals(peers, phexDownload.getFreeNodes());
		assertEquals(leechers, phexDownload.getBusyNodes());
		assertEquals(timeRemaining, phexDownload.getRemainingSeconds() );
	}
	
	@Test
	public void shouldLeaveTimeIn0WhenInfinityRemainingSeconds() throws Exception {
		phexDownload.setDownloadFile(downloadFile);
		phexDownload.eta = etaComparator;
		when(etaComparator.calcTimeRemaining(downloadFile)).thenReturn((long) TransferDataProvider.INFINITY_ETA_INT);
		
		phexDownload.updateStatus();
		
		assertEquals(0, phexDownload.getRemainingSeconds());
	}
	
	@Test
	public void shouldResetProgressIfUnkownSize() throws Exception{
		int progressExpected = 0;
		when(downloadFile.getProgress()).thenReturn(PhexDownload.UNKOWN_SIZE);
		phexDownload.setDownloadFile(downloadFile);
		
		phexDownload.updateStatus();
		
		assertEquals(progressExpected, phexDownload.getProgress());
	}
	
	@Test
	public void shouldNotSetSizeWhenIsZero() throws Exception {
		long zeroSize = 0;
		
		phexDownload.setSize(zeroSize);
		
		assertEquals(PhexDownload.UNKOWN_SIZE, phexDownload.getSize());
	}
	
	
	@Test
	public void shouldRespectExtension() throws Exception {
		String extension = "MP3";
		PhexDownload phexDownload = new PhexDownload(allLinkString);
		phexDownload.setFileExtension(extension);
		assertEquals(".MP3", phexDownload.getFileExtension());
	}
	
	@Test
	public void shouldSetAllLink() throws Exception {
		String allLinkAsString = "allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b&urnsha1=J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
		PhexDownload phexDownload = new PhexDownload();
		phexDownload.setAllLink(allLinkAsString);
		assertEquals(urnsha, phexDownload.getFileHashcode());
		assertEquals(hashcode, phexDownload.getHashcode());
	}
	
	@Test
	public void shouldNotAssignAllLink() throws Exception {
		PhexDownload phexDownload = new PhexDownload();
		phexDownload.setAllLink("");
		assertNull(phexDownload.getFileHashcode());
	}
	
	@Test
	public void shouldGetAllLinkAsString() throws Exception {
		assertEquals(allLinkAsString, phexDownload.getAllLinkAsString());
	}
	
	@Test
	public void shouldSetStartTimeStamp() throws Exception {
		PhexDownload phexDownload = new PhexDownload();
		phexDownload.setStartTimeStamp();
		assertTrue(phexDownload.getStartTimeStamp()>0);
	}
	
	@Test
	public void shouldNotSetProgressAt100WhenStillDownloading() throws Exception {
		phexDownload.setProgress(100);
		when(downloadFile.getTransferSpeed()).thenReturn(1024L);
		phexDownload.setDownloadFile(downloadFile);
		
		int progress = phexDownload.verifyDownloadProgress(100);
		
		assertEquals(98, progress);
	}
	
	@Test
	public void shouldSetProgress100WhenFinishDownloaging() throws Exception {
		phexDownload.setProgress(100);
		when(downloadFile.getTransferSpeed()).thenReturn(0L);
		phexDownload.setDownloadFile(downloadFile);
		
		int progress = phexDownload.verifyDownloadProgress(100);
		
		assertEquals(100, progress);
	}
}
