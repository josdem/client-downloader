package com.all.downloader.alllink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestAllLink {
	String allLinkString = "allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b&urnsha1=TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI&magnetLink=9d50b64820e7e288146a9ae4e14902a3ba584999&candidate=josdem@all.com";
	private static final String DOWNLOAD_ID = "downloadId";
	AllLink allLink = AllLink.parse(allLinkString);

	@Test
	public void shouldGetHashCodeAndUrnSha() throws Exception {
		assertEquals("00a9ae41a50cfece357f26e786db6fa014af765b", allLink.getHashCode());
		assertTrue(allLink.containsUrnSha());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void shouldNotCreateAllLinkWithoutHascodeNorUrnSha() throws Exception {
		new AllLink(null, null);
	}

	@Test
	public void shouldRespectHashCodePosition() throws Exception {
		String allLinkHashCodeOtherPosition = "allLink:urnsha1=TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI&magnetLink=TTYJZFSAJF5EXZYII7CKPISWSAWNZZZZ&hashcode=00a9ae41a50cfece357f26e786db6fa014af765b";
		allLink = AllLink.parse(allLinkHashCodeOtherPosition);
		assertEquals("00a9ae41a50cfece357f26e786db6fa014af765b", allLink.getHashCode());
	}

	@Test
	public void shouldGetFileHash() throws Exception {
		String fileHash = "TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI";
		assertEquals(fileHash, allLink.getUrnSha());
	}

	@Test
	public void shouldPrintAllLinkAsString() throws Exception {
		assertEquals("allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b&urnsha1=TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI", allLink.toString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void souldFailOnInvalidString() throws Exception {
		String allLinkString = "";
		AllLink.parse(allLinkString);
	}

	@Test
	public void shouldGetOnlyHashCode() throws Exception {
		String allLinkString = "allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b";
		AllLink allLink = AllLink.parse(allLinkString);
		assertEquals("00a9ae41a50cfece357f26e786db6fa014af765b", allLink.getHashCode());
		assertNull(null, allLink.getUrnSha());
	}

	@Test
	public void shouldGetOnlyHashCodeAndFileHash() throws Exception {
		String allLinkString = "allLink:hashcode=00a9ae41a50cfece357f26e786db6fa014af765b&urnsha1=TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI";
		AllLink allLink = AllLink.parse(allLinkString);
		assertEquals("00a9ae41a50cfece357f26e786db6fa014af765b", allLink.getHashCode());
		assertEquals("TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI", allLink.getUrnSha());
	}

	@Test
	public void shouldCreateAllLinkWithOnlyHashCode() throws Exception {
		String hashcode = "01a8c9b1ec3c321d187dca8f0562f7f8165507bf";
		AllLink allLink = new AllLink(hashcode, null);
		assertEquals(hashcode, allLink.getHashCode());
		assertEquals(null, allLink.getUrnSha());
	}

	@Test
	public void shouldCreateAllLinkWithOnlyUrnsha() throws Exception {
		String urnsha = "TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI";
		AllLink allLink = new AllLink(null, urnsha);
		assertEquals(urnsha, allLink.getUrnSha());
		assertEquals(null, allLink.getHashCode());
	}

	@Test
	public void shouldCreateALLLinkToGnutella() throws Exception {
		String expectedALLLink = "allLink:urnsha1=TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI";
		String urnsha1 = "TTYJZFSAJF5EXZYII7CKPISWSAWNCCHI";
		AllLink allLink = new AllLink(null, urnsha1);
		assertEquals(expectedALLLink, allLink.toString());
	}

	@Test
	public void shouldCreateAllLinkWithFirstCandidate() throws Exception {
		String fileHash = "fileHash";
		AllLink allLink = AllLink
				.parse("allLink:hashcode=downloadId&urnsha1=fileHash&candidate=rosario@all.com,josdem@all.com,hilda@all.com,omar@all.com,arturo@all.com");
		assertEquals(DOWNLOAD_ID, allLink.getHashCode());
		assertEquals(fileHash, allLink.getUrnSha());
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldSendIllegalArgumentException() throws Exception {
		AllLink.parse("aStringWithOutAnyFormat");
	}

}
