package org.gungurbuz.service;

import org.gungurbuz.data.StopEvent;
import org.gungurbuz.data.TriasClient;
import org.gungurbuz.data.TriasStopEventParser;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DepartureBoardService {
	
	private final TriasClient client;
	private final TriasStopEventParser parser;
	
	private final ZoneId zone = ZoneId.systemDefault();
	private final DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm").withZone(zone);
	
	public DepartureBoardService(TriasClient client, TriasStopEventParser parser) {
		this.client = client;
		this.parser = parser;
	}
	
	public List<DepartureRow> loadDepartures(String stopPointRef, int n, List<String> allowedLines) throws Exception {
		String xml = client.fetchStopEventsXml(stopPointRef, "departure", n);
		List<StopEvent> events = parser.parseStopEvents(xml);
		
		List<DepartureRow> rows = new ArrayList<>();
		for (StopEvent e : events) {
			if (e.line() == null) continue;
			
			if (allowedLines != null && !allowedLines.isEmpty() && !allowedLines.contains(e.line())) {
				continue;
			}
			
			String sched = toLocalHHmm(e.timetabledTimeUtc());
			String real  = toLocalHHmm(e.estimatedTimeUtc());
			
			rows.add(new DepartureRow(
					e.line(),
					e.direction() != null ? e.direction() : "?",
					sched,
					real
			));
		}
		
		// Sort by the best available time (real if present, else scheduled)
		rows.sort(Comparator.comparing(r -> (r.realHHmm() != null ? r.realHHmm() : r.schedHHmm())));
		return rows;
	}
	
	private String toLocalHHmm(String utcIsoZ) {
		if (utcIsoZ == null || utcIsoZ.isBlank()) return null;
		return hhmm.format(Instant.parse(utcIsoZ.trim()));
	}
}