package com.all.downloader.share;

import java.util.EventObject;

import com.all.downloader.alllink.AllLink;

public class FileSharedEvent extends EventObject {

	private static final long serialVersionUID = -3419476051730955313L;
	private final AllLink allLink;

	public FileSharedEvent(Object source, AllLink allLink) {
		super(source);
		this.allLink = allLink;
	}

	public AllLink getAllLink() {
		return allLink;
	}
}
