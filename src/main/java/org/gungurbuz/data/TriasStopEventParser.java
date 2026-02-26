package org.gungurbuz.data;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class TriasStopEventParser {
	
	private static final String TRIAS_NS = "http://www.vdv.de/trias";
	
	public List<StopEvent> parseStopEvents(String xml) throws Exception {
		if (xml == null || xml.isBlank()) return List.of();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		Document doc = dbf.newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		
		NodeList stopEvents = doc.getElementsByTagNameNS(TRIAS_NS, "StopEvent");
		List<StopEvent> out = new ArrayList<>(stopEvents.getLength());
		
		for (int i = 0; i < stopEvents.getLength(); i++) {
			Element ev = (Element) stopEvents.item(i);
			
			String line = firstTextOf(ev, "PublishedLineName");
			String dir  = firstTextOf(ev, "DestinationText");
			String tt   = firstTextOf(ev, "TimetabledTime");
			String est  = firstTextOf(ev, "EstimatedTime");
			
			out.add(new StopEvent(line, dir, tt, est));
		}
		
		return out;
	}
	
	private static String firstTextOf(Element scope, String triasElementLocalName) {
		NodeList nodes = scope.getElementsByTagNameNS(TRIAS_NS, triasElementLocalName);
		if (nodes.getLength() == 0) return null;
		
		Element e = (Element) nodes.item(0);
		
		NodeList textNodes = e.getElementsByTagNameNS(TRIAS_NS, "Text");
		if (textNodes.getLength() > 0) {
			String v = textNodes.item(0).getTextContent();
			return v != null ? v.trim() : null;
		}
		
		String v = e.getTextContent();
		return v != null ? v.trim() : null;
	}
}