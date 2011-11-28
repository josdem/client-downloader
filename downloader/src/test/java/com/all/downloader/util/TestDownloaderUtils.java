package com.all.downloader.util;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


public class TestDownloaderUtils {
	private String dir = "directory";
	private String fileName = "fileName";
	private static final String SEPARATOR = System.getProperty("file.separator");
	
	@SuppressWarnings("unused")
	@InjectMocks
	private DownloaderUtils downloaderUtils = new DownloaderUtils();
	@Mock
	private FileFactory fileFactory;
	@Mock
	private File file;
	private String expectedValidPath = "directory" + SEPARATOR + "fileName";
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		
	}

	@Test
	public void shouldGetValidFilePath() throws Exception {
		when(fileFactory.getFile(expectedValidPath)).thenReturn(file);
		when(file.exists()).thenReturn(false);
		String result = DownloaderUtils.getValidFilePath(dir, fileName);
		assertEquals(expectedValidPath, result);
	}
	
	@Test
	public void shouldGetAnotherNameIfFileExists() throws Exception {
		String renamedValidPath = "directory" + SEPARATOR + "(1)fileName";
		when(fileFactory.getFile(expectedValidPath)).thenReturn(file);
		when(file.exists()).thenReturn(true);
		String result = DownloaderUtils.getValidFilePath(dir, fileName);
		assertEquals(renamedValidPath, result);
	}
}
