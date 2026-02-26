package org.gungurbuz;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import java.io.StringReader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

public class TriasArrivalsDisplay {
	
	private static final String TRIAS_NS = "http://www.vdv.de/trias";
	private static final String API_KEY = System.getenv("TRIAS_API_KEY");;
	static {
		if (API_KEY == null || API_KEY.isBlank()) {
			throw new RuntimeException(
					"TRIAS_API_KEY not set!");
		}
	}
	private static final String STOP_PLACE_REF = "de:08212:1103:2:2";
	private static final String URL =
			"https://projekte.kvv-efa.de/guerbueztrias/trias";
	
	public static void main(String[] args) throws Exception {
		
		String xml = buildRequest();
		
		HttpClient client = HttpClient.newHttpClient();
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(URL))
				.header("Content-Type", "application/xml")
				.POST(HttpRequest.BodyPublishers.ofString(xml))
				.build();
		
		HttpResponse<String> response =
				client.send(request, HttpResponse.BodyHandlers.ofString());
		
		parseAndDisplay(response.body());
	}
	
	private static String buildRequest() {
		
		String timestamp = LocalDateTime.now()
				.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		
		return """
				<Trias version="1.1"
				       xmlns="http://www.vdv.de/trias"
				       xmlns:siri="http://www.siri.org.uk/siri">
				    <ServiceRequest>
				        <siri:RequestTimeStamp>%s</siri:RequestTimeStamp>
				        <siri:RequestorRef>%s</siri:RequestorRef>
				        <RequestPayload>
				            <StopEventRequest>
				                <Location>
				                    <LocationRef>
				                        <StopPointRef>%s</StopPointRef>
				                    </LocationRef>
				                </Location>
				                <Params>
				                    <NumberOfResults>2</NumberOfResults>
				                    <StopEventType>arrival</StopEventType>
				                </Params>
				            </StopEventRequest>
				        </RequestPayload>
				    </ServiceRequest>
				</Trias>
				""".formatted(timestamp, API_KEY, STOP_PLACE_REF);
	}
	
	private static void parseAndDisplay(String xml) throws Exception {
		
		// Helpful debug: if the server sent an error, you’ll see it right away.
		if (xml == null || xml.isBlank()) {
			System.out.println("Empty HTTP response body.");
			return;
		}
		
		// Parse XML WITH namespaces enabled
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		Document doc = dbf.newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		
		// Find StopEvent elements using namespace
		NodeList stopEvents = doc.getElementsByTagNameNS(TRIAS_NS, "StopEvent");
		
		// If nothing found, print the first ~500 chars so you can see what came back.
		if (stopEvents.getLength() == 0) {
			System.out.println("No <StopEvent> found. First part of response:\n");
			System.out.println(xml.substring(0, Math.min(500, xml.length())));
			return;
		}
		
		System.out.println("\nUpcoming Arrivals:\n");
		
		for (int i = 0; i < stopEvents.getLength(); i++) {
			Element stopEvent = (Element) stopEvents.item(i);
			
			// Line name is usually PublishedLineName/Text
			String line = firstTextOf(stopEvent, "PublishedLineName");
			
			// DestinationText/Text is commonly used for direction
			String direction = firstTextOf(stopEvent, "DestinationText");
			
			// Arrival time is often nested; try EstimatedTime first, then TimetabledTime
			String time = firstTextOf(stopEvent, "EstimatedTime");
			if (time == null) time = firstTextOf(stopEvent, "TimetabledTime");
			
			// If you only want S1/S11:
			if (line == null) continue;
			if (!line.equals("S1") && !line.equals("S11")) continue;
			
			System.out.printf("%s → %s  %s%n",
					line,
					direction != null ? direction : "?",
					time != null ? time : "no time");
		}
	}
	
	/**
	 * Returns the first human-readable text found under a TRIAS element name.
	 * Works for patterns like <PublishedLineName><Text>...</Text></PublishedLineName>
	 * and also direct text content.
	 */
	private static String firstTextOf(Element scope, String triasElementLocalName) {
		NodeList nodes = scope.getElementsByTagNameNS(TRIAS_NS, triasElementLocalName);
		if (nodes.getLength() == 0) return null;
		
		Element e = (Element) nodes.item(0);
		
		// Common TRIAS pattern: <Something><Text>value</Text></Something>
		NodeList textNodes = e.getElementsByTagNameNS(TRIAS_NS, "Text");
		if (textNodes.getLength() > 0) {
			String v = textNodes.item(0).getTextContent();
			return v != null ? v.trim() : null;
		}
		
		// Fallback: any text content
		String v = e.getTextContent();
		return v != null ? v.trim() : null;
	}
	
}