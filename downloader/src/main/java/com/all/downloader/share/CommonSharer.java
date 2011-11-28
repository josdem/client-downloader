package com.all.downloader.share;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class CommonSharer implements Sharer {

	protected Set<SharerListener> listeners = new CopyOnWriteArraySet<SharerListener>();
	
	@Override
	public void addSharerListener(SharerListener sharerListener) {
		listeners.add(sharerListener);
	}
	
	@Override
	public void removeSharerListener(SharerListener sharerListener) {
		listeners.remove(sharerListener);
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
