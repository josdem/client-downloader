package com.all.downloader.portmapping;

public interface PortMapListener {
	
	void onPortMap(PortMapEvent portMapEvent);
	
	void onPortMapError(PortMapErrorEvent portMapErrorEvent);
	
}
