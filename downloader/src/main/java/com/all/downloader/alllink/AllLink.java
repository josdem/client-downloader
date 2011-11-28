package com.all.downloader.alllink;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class AllLink {

	private static final String ALL_LINK_HEADER = "allLink:";

	private static final String URNSHA1_IDENTIFIER = "urnsha1";
	private static final String HASHCODE_IDENTIFIER = "hashcode";
	private static final String DELIMITER = "&";
	private static final String EQUALS_CHAR = "=";
	private Map<String, String> urisMap = new HashMap<String, String>();

	public AllLink(String downloadId, String urnSha) {
		fillMap(downloadId, urnSha);
	}

	public static AllLink parse(String allLink) {
		if (allLink.startsWith(ALL_LINK_HEADER)) {
			allLink = allLink.substring(ALL_LINK_HEADER.length());
			String[] keyValues = allLink.split(DELIMITER);
			String downloadId = null;
			String urnSha = null;
			for (String keyValue : keyValues) {
				String key = keyValue.substring(0, keyValue.indexOf(EQUALS_CHAR));
				if (key.equals(HASHCODE_IDENTIFIER)) {
					downloadId = keyValue.substring(keyValue.indexOf(EQUALS_CHAR) + 1);
				}
				if (key.equals(URNSHA1_IDENTIFIER)) {
					urnSha = keyValue.substring(keyValue.indexOf(EQUALS_CHAR) + 1);
				}
			}
			return new AllLink(downloadId, urnSha);
		} else {
			throw new IllegalArgumentException("The string provided is not a valid AllLink.");
		}
	}

	public boolean containsUrnSha() {
		return urisMap.containsKey(URNSHA1_IDENTIFIER);
	}

	private void fillMap(String downloadId, String urnSha) {
		if (StringUtils.isEmpty(downloadId) && StringUtils.isEmpty(urnSha)) {
			throw new IllegalArgumentException("Cannot create AllLink with args: " + downloadId + ", " + urnSha);
		}
		put(HASHCODE_IDENTIFIER, downloadId);
		put(URNSHA1_IDENTIFIER, urnSha);
	}

	private void put(String key, String value) {
		if (StringUtils.isNotEmpty(value)) {
			urisMap.put(key, value);
		}
	}

	public String getHashCode() {
		return urisMap.get(HASHCODE_IDENTIFIER);
	}

	public String getUrnSha() {
		return urisMap.get(URNSHA1_IDENTIFIER);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(ALL_LINK_HEADER);
		appendToAllLink(HASHCODE_IDENTIFIER, sb);
		appendToAllLink(URNSHA1_IDENTIFIER, sb);
		return sb.toString();
	}

	void appendToAllLink(String key, StringBuilder sb) {
		String value = urisMap.get(key);
		if (value != null) {
			if (!sb.toString().equals(ALL_LINK_HEADER)) {
				sb.append(DELIMITER);
			}
			sb.append(key);
			sb.append(EQUALS_CHAR);
			sb.append(value);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((urisMap == null) ? 0 : urisMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AllLink other = (AllLink) obj;
		if (urisMap == null) {
			if (other.urisMap != null) {
				return false;
			}
		} else if (!urisMap.equals(other.urisMap)) {
			return false;
		}
		return true;
	}

}
