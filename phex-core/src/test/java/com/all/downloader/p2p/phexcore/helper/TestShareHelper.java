package com.all.downloader.p2p.phexcore.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import phex.common.URN;
import phex.prefs.api.Setting;
import phex.share.ShareFile;
import phex.share.SharedFilesService;
import phex.share.UrnGenerator;

import com.all.downloader.p2p.phexcore.PhexCoreImpl;
import com.all.downloader.p2p.phexcore.helper.ShareHelper.FileHelper;
import com.all.testing.MockInyectRunner;
import com.all.testing.UnderTest;

@RunWith(MockInyectRunner.class)
public class TestShareHelper {
	private static final String URN_SHA = "J2C4MZ3OFLAYGSD2VLET7PK5SCWLWBRL";
	private static final String FILE_PATH = "FILE_PATH";
	@UnderTest
	private ShareHelper shareHelper;

	@Mock
	private PhexCoreImpl phexCore;
	@Mock
	private File fileToShare;
	@Mock
	private Setting<Set<String>> sharedDirectories;
	@Mock
	private File directoryToShare;
	@Mock
	private ShareFile shareFile;
	@Mock
	private SharedFilesService shareFileService;
	@Mock
	private UrnGenerator urnGenerator;
	@Mock
	private FileHelper fileHelper;

	@Before
	public void setup() {
		when(phexCore.getSharedFilesService()).thenReturn(shareFileService);
	}

	@SuppressWarnings("unchecked")
	@Test
	@Ignore
	public void shouldShareFile() throws Exception {
		String filePath = "filePath";
		String parentFolderName = "folderName";

		File fileToShare = mock(File.class);
		File parentFolder = mock(File.class);
		SharedFilesService sharedFilesService = mock(SharedFilesService.class);
		final ShareFile shareFile = mock(ShareFile.class);
		final Setting setting = mock(Setting.class);

		Set<String> folders = mock(Set.class);
		folders.add(new String("anotherFolder"));

		URN urn = mock(URN.class);
		shareFile.setURN(urn);

		// when(provider.getFileForShare(DOWNLOAD_ID)).thenReturn(fileToShare);
		when(fileToShare.exists()).thenReturn(true);
		// when(servent.getShareFileService()).thenReturn(sharedFilesService);
		when(fileToShare.getAbsolutePath()).thenReturn(filePath);
		when(sharedFilesService.getFileByName(filePath)).thenReturn(null);
		when(shareFile.getSystemFile()).thenReturn(parentFolder);
		when(parentFolder.getParent()).thenReturn(parentFolderName);
		when(parentFolder.isDirectory()).thenReturn(true);
		when(setting.get()).thenReturn(folders);
		when(shareFile.getURN()).thenReturn(urn);
		when(shareFile.getSha1()).thenReturn(URN_SHA);

		// manager = new PhexManager(){
		// @Override
		// ShareFile createShareFile(File fileToSeed) {
		// return shareFile;
		// }
		// @Override
		// Setting<Set<String>> getSharedDirectories() {
		// return setting;
		// }
		// };
		// manager.sWrapper = servent;

		String result = shareHelper.getUrnsha(fileToShare);

		assertNotNull(shareFile);
		verify(sharedFilesService).queueUrnCalculation(shareFile);
		verify(sharedFilesService).addSharedFile(shareFile);
		verify(shareFile).getSha1();
		assertEquals(URN_SHA, result);
	}

	@Test
	public void shouldShareAFileThatExist() throws Exception {
		ShareFile shareFile = mock(ShareFile.class);
		when(fileToShare.getAbsolutePath()).thenReturn(FILE_PATH);

		when(shareFileService.getFileByName(FILE_PATH)).thenReturn(shareFile);
		when(shareFile.getSha1()).thenReturn(URN_SHA);

		String result = shareHelper.getUrnsha(fileToShare);
		assertEquals(URN_SHA, result);
	}

	@Test
	public void shouldNotAddFileToDirectoryIfExist() throws Exception {
		String directoryName = "directoryName";
		Set<String> sharedDirs = new HashSet<String>();
		sharedDirs.add("directoryName");

		ShareFile shareFile = setSharedDirectoriesExpectations(directoryName, sharedDirs);

		shareHelper.addFileDirectory(shareFile);

		verify(directoryToShare, never()).getAbsolutePath();
		verify(sharedDirectories, never()).changed();
	}

	private ShareFile setSharedDirectoriesExpectations(String directoryName, Set<String> sharedDirs) {
		File systemFile = mock(File.class);
		ShareFile shareFile = mock(ShareFile.class);

		when(shareFile.getSystemFile()).thenReturn(systemFile);
		when(systemFile.getParent()).thenReturn(directoryName);
		when(fileHelper.getSharedDirectories()).thenReturn(sharedDirectories);
		when(sharedDirectories.get()).thenReturn(sharedDirs);
		return shareFile;
	}

	@Test
	public void shouldNotShareDirRecursiveIfNoDirectory() throws Exception {
		File file = mock(File.class);
		when(file.isDirectory()).thenReturn(false);
		boolean result = shareHelper.shareDirRecursive(file);
		verify(file, never()).getAbsolutePath();
		assertFalse(result);
	}

	private void setUrnGeneratorExpectations() {
		when(phexCore.getSharedFilesService()).thenReturn(shareFileService);

		when(urnGenerator.generate(shareFile)).thenReturn(true);
	}

	@Test
	public void shouldNotCompleteShareProcessIfNoURN() throws Exception {
		setUrnGeneratorExpectations();

		String result = shareHelper.getUrnsha(fileToShare);
		verify(shareFileService, never()).addSharedFile(shareFile);
		assertNull(result);
	}

	@Test
	public void shouldAddSharedFile() throws Exception {
		String directoryName = "directoryName";
		Set<String> sharedDirs = new HashSet<String>();
		sharedDirs.add("directoryName");
		File systemFile = mock(File.class);

		when(shareFile.getSystemFile()).thenReturn(systemFile);
		when(systemFile.getParent()).thenReturn(directoryName);
		when(fileHelper.getSharedDirectories()).thenReturn(sharedDirectories);
		when(sharedDirectories.get()).thenReturn(sharedDirs);
		setUrnGeneratorExpectations();

		URN urn = mock(URN.class);
		when(fileHelper.createShareFile(fileToShare)).thenReturn(shareFile);
		when(shareFile.getURN()).thenReturn(urn);
		when(shareFile.getSha1()).thenReturn(URN_SHA);

		String result = shareHelper.getUrnsha(fileToShare);

		verify(shareFileService).addSharedFile(shareFile);
		assertEquals(URN_SHA, result);

	}
}
