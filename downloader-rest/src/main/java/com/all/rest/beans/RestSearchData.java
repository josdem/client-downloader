package com.all.rest.beans;

import org.apache.commons.lang.StringUtils;

import com.all.downloader.search.SearchData;
import com.all.shared.mc.TrackSearchResult;
import com.all.shared.model.Track;

public final class RestSearchData implements SearchData {

	private static final int DEFAULT_REST_PEERS = 10;
	private final Track foundTrack;
	private final TrackSearchResult searchResult;
	private final String name;

	public RestSearchData(TrackSearchResult searchResult) {
		this.searchResult = searchResult;
		this.foundTrack = searchResult.getTrack();
		StringBuilder sb = new StringBuilder(foundTrack.getName());
		sb.append(StringUtils.isEmpty(foundTrack.getArtist()) ? "" : "_" + foundTrack.getArtist());
		sb.append(StringUtils.isEmpty(foundTrack.getAlbum()) ? "" : "_" + foundTrack.getAlbum());
		this.name = buildName(foundTrack);
	}

	private String buildName(Track track) {
		StringBuilder sb = new StringBuilder(track.getName());
		if (StringUtils.isNotEmpty(track.getArtistAlbum())) {
			sb.append(" (");
			boolean addSeparator = false;
			if (StringUtils.isNotEmpty(track.getArtist())) {
				sb.append(track.getArtist());
				addSeparator = true;
			}
			if (addSeparator) {
				sb.append(" - ");
			}
			if (StringUtils.isNotEmpty(track.getAlbum())) {
				sb.append(track.getAlbum());
			}
			sb.append(")");
		}
		return sb.toString();
	}

	@Override
	public String getFileHash() {
		return foundTrack.getHashcode();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getPeers() {
		double keywordAffinity = searchResult.getScore();
		if (keywordAffinity >= 1.0) {
			return DEFAULT_REST_PEERS;
		}
		if (keywordAffinity > 0.5) {
			return (int) (DEFAULT_REST_PEERS * keywordAffinity);
		}
		return 1;
	}

	@Override
	public long getSize() {
		return foundTrack.getSize();
	}

	@Override
	public String getFileType() {
		return foundTrack.getFileFormat();
	}

	@Override
	public Track toTrack() {
		return foundTrack;
	}
	
	@Override
	public Source getSource() {
		return Source.ALL;
	}

}
