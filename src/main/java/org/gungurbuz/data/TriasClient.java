package org.gungurbuz.data;

import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TriasClient {
	
	private final String endpointUrl;
	private final String apiKey;
	private final HttpClient http;
	
	public TriasClient(String endpointUrl, String apiKey) {
		this.endpointUrl = endpointUrl;
		this.apiKey = apiKey;
		this.http = HttpClient.newHttpClient();
	}
	
	public String fetchStopEventsXml(String stopPointRef, String stopEventType, int numberOfResults) throws Exception {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		
		String requestXml = """
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
                      <NumberOfResults>%d</NumberOfResults>
                      <StopEventType>%s</StopEventType>
                    </Params>
                  </StopEventRequest>
                </RequestPayload>
              </ServiceRequest>
            </Trias>
            """.formatted(timestamp, apiKey, stopPointRef, numberOfResults, stopEventType);
		
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(endpointUrl))
				.header("Content-Type", "application/xml")
				.POST(HttpRequest.BodyPublishers.ofString(requestXml))
				.build();
		
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
}