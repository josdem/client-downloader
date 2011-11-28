package com.all.downloader.util;

import java.io.File;

public class FileFactory {
	public File getFile(String completePath) {
		return new File(completePath);
	}
}
