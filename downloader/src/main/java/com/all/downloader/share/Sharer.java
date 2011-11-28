package com.all.downloader.share;

import com.all.downloader.alllink.AllLink;

public interface Sharer {

	void share(AllLink currentAllLink) throws ShareException;

	void addSharerListener(SharerListener sharerListener);

	void removeSharerListener(SharerListener sharerListener);

}
