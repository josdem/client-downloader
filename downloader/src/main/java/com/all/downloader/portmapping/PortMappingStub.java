package com.all.downloader.portmapping;


public class PortMappingStub implements UpnpService, NatPmpService {

	@Override
	public void addPortMapListener(PortMapListener portMapListener) {
	}

	@Override
	public int addUpnpMapping(int protocolType, int localPort, int externalPortRequested) {
		return 0;
	}

	@Override
	public void removeUpnpMapping(int mappingIndex) {
	}

	@Override
	public int addNatPmpMapping(int protocolType, int localPort, int externalPortRequested) {
		return 0;
	}

	@Override
	public void removeNatPmpMapping(int mappingIndex) {
	}

}
