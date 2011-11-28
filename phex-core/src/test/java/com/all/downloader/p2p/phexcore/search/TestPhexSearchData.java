package com.all.downloader.p2p.phexcore.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mock;

import phex.download.RemoteFile;

import com.all.downloader.p2p.phexcore.BasePhexTestCase;
import com.all.downloader.p2p.phexcore.search.PhexSearchData;
import com.all.downloader.search.SearchData;


public class TestPhexSearchData extends BasePhexTestCase {
	SearchData phexSearchData;

	@Mock
	private RemoteFile remoteFile;
	
	@Test
	public void shouldAssignValuesToBean() throws Exception {
		String urnsha = "sha1";
		String extension = "extension";
		String name = "name";
		long size = 1L;
		int peers = 1;
		
		when(remoteFile.getSHA1()).thenReturn(urnsha);
		when(remoteFile.getFileExt()).thenReturn(extension);
		when(remoteFile.getDisplayName()).thenReturn(name);
		when(remoteFile.getFileSize()).thenReturn(size);
		
		phexSearchData = PhexSearchData.createFrom(remoteFile);
		
		assertEquals(urnsha, phexSearchData.getFileHash());
		assertEquals(extension, phexSearchData.getFileType());
		assertEquals(name, phexSearchData.getName());
		assertEquals(size, phexSearchData.getSize());
		assertEquals(peers, phexSearchData.getPeers());
	}
	
	@Test
	public void shouldReturnNullIFRemoteFileIsNull() throws Exception {
		assertNull(PhexSearchData.createFrom(null));
	}
	
}
