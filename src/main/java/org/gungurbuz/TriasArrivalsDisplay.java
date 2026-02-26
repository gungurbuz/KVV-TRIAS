package org.gungurbuz;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TriasArrivalsDisplay {
	
	private static final String TRIAS_NS = "http://www.vdv.de/trias";
	private static final String API_KEY = System.getenv("TRIAS_API_KEY");
	
	static {
		if (API_KEY == null || API_KEY.isBlank()) {
			throw new RuntimeException("TRIAS_API_KEY not set!");
		}
	}
	
	private static final String STOP_POINT_REF = "de:08212:1103:2:2";
	private static final String URL = "https://projekte.kvv-efa.de/guerbueztrias/trias";
	
	// Output format requested: HH:MM (in Java that's HH:mm)
	private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
	
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
		
		// TRIAS servers typically accept ISO timestamps; keeping your current format.
		String timestamp = java.time.LocalDateTime.now()
				.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		
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
                                    <StopEventType>departure</StopEventType>
                                </Params>
                            </StopEventRequest>
                        </RequestPayload>
                    </ServiceRequest>
                </Trias>
                """.formatted(timestamp, API_KEY, STOP_POINT_REF);
	}
	
	private static void parseAndDisplay(String xml) throws Exception {
		
		if (xml == null || xml.isBlank()) {
			System.out.println("Empty HTTP response body.");
			return;
		}
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		Document doc = dbf.newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		
		NodeList stopEvents = doc.getElementsByTagNameNS(TRIAS_NS, "StopEvent");
		
		if (stopEvents.getLength() == 0) {
			System.out.println("No <StopEvent> found. First part of response:\n");
			System.out.println(xml.substring(0, Math.min(500, xml.length())));
			return;
		}
		
		System.out.println("\nUpcoming Departures:\n");
		
		for (int i = 0; i < stopEvents.getLength(); i++) {
			Element stopEvent = (Element) stopEvents.item(i);
			
			String line = firstTextOf(stopEvent, "PublishedLineName");
			String direction = firstTextOf(stopEvent, "DestinationText");
			
			String schedRaw = firstTextOf(stopEvent, "TimetabledTime");
			String realRaw  = firstTextOf(stopEvent, "EstimatedTime");
			
			String sched = toLocalHHmm(schedRaw);
			String real  = toLocalHHmm(realRaw);
			
			if (line == null) continue;
			if (!line.equals("S1") && !line.equals("S11")) continue;
			
			String timeText;
			if (sched == null && real == null) {
				timeText = "--:--";
			} else if (real == null || real.equals(sched)) {
				timeText = sched != null ? sched : real;
			} else {
				timeText = sched + " → " + real;  // scheduled → real
			}
			
			System.out.printf("%s → %s  %s%n",
					line,
					direction != null ? direction : "?",
					timeText);
		}
	}
	
	/**
	 * Convert TRIAS time (usually ISO 8601 with Z or +hh:mm) to local HH:mm.
	 */
	private static String toLocalHHmm(String triasTime) {
		if (triasTime == null || triasTime.isBlank()) return null;
		
		Instant instant = Instant.parse(triasTime.trim());
		
		return DateTimeFormatter.ofPattern("HH:mm")
				.withZone(ZoneId.systemDefault())
				.format(instant);
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
		
		NodeList textNodes = e.getElementsByTagNameNS(TRIAS_NS, "Text");
		if (textNodes.getLength() > 0) {
			String v = textNodes.item(0).getTextContent();
			return v != null ? v.trim() : null;
		}
		
		String v = e.getTextContent();
		return v != null ? v.trim() : null;
	}
}