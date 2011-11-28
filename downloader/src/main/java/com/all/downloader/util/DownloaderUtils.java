package com.all.downloader.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DownloaderUtils {

	private static final int ATTEMPTS = 30;
	private static final String DOT = ".";
	private static final String SEPARATOR = System.getProperty("file.separator");
	private static  FileFactory fileFactory = new FileFactory();
	
	
	public static String getValidFileName(String name, String extension) {
		return convertToLocalSystemFilename(name) + DOT + extension.toLowerCase();
	}

	public static String getValidFilePath(String dir, final String fileName) throws IOException {
		String tryName = fileName;
		String completePath = dir + SEPARATOR + tryName;
		int counter = 0;
		int attempts = 0;
		File destFile = fileFactory.getFile(completePath);
		while (destFile.exists()) {
			counter++;
			tryName = "(" + counter + ")" + fileName;
			completePath = dir + SEPARATOR + tryName;
			destFile = new File(completePath);
			if (++attempts >= ATTEMPTS) {
				throw new IOException(completePath + "has been looked for up to " + ATTEMPTS + " times.");
			}
		}
		return completePath;
	}

	public static String convertToLocalSystemFilename(String filename) {
		List<Character> invalidChars = Arrays.asList(new Character[] { '/', ':', '\"', '<', '>', ' ', '\'', '_', '*', '\\',
				'?', '|' });
		char[] charArray = filename.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (Character strChar : charArray) {
			if (!invalidChars.contains(strChar)) {
				sb.append(strChar);
			}
		}
		return sb.toString().substring(0, Math.min(255, sb.length()));
	}

}
