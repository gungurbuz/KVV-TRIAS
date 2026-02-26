package org.gungurbuz.app;

import org.gungurbuz.data.TriasClient;
import org.gungurbuz.data.TriasStopEventParser;
import org.gungurbuz.service.DepartureBoardService;
import org.gungurbuz.service.DepartureRow;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DepartureBoardApp {
	
	private static final String URL = "https://projekte.kvv-efa.de/guerbueztrias/trias";
	private static final String STOP_POINT_REF = "de:08212:1103:2:2";
	
	// Filter only these lines (set to List.of() to show all)
	private static final List<String> ALLOWED_LINES = List.of("S1", "S11");
	
	private static volatile boolean running = true;
	
	public static void main(String[] args) throws Exception {
		
		String apiKey = System.getenv("TRIAS_API_KEY");
		if (apiKey == null || apiKey.isBlank()) throw new RuntimeException("TRIAS_API_KEY not set!");
		
		TriasClient client = new TriasClient(URL, apiKey);
		TriasStopEventParser parser = new TriasStopEventParser();
		DepartureBoardService service = new DepartureBoardService(client, parser);
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
		
		while (running) {
			// Sleep until the next exact boundary (:00 or :30)
			sleepUntilNextMinuteBoundary();
			
			try {
				clearConsole();
				System.out.println("Last update: " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
				System.out.println();
				
				List<DepartureRow> rows = service.loadDepartures(STOP_POINT_REF, 10, ALLOWED_LINES);
				
				System.out.println("Upcoming Departures:");
				System.out.println();
				
				for (DepartureRow r : rows) {
					String timeText;
					if (r.schedHHmm() == null && r.realHHmm() == null) {
						timeText = "--:--";
					} else if (r.realHHmm() == null || r.realHHmm().equals(r.schedHHmm())) {
						timeText = r.schedHHmm() != null ? r.schedHHmm() : r.realHHmm();
					} else {
						timeText = r.schedHHmm() + " -> " + r.realHHmm();
					}
					
					System.out.printf("%-4s -> %-30s %s%n",
							r.line(),
							r.direction(),
							timeText);
				}
				
			} catch (Exception e) {
				// Don’t exit; try again at next boundary
				System.out.println("Update failed: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Sleeps until the next wall-clock boundary where seconds == 0 or 30, and nanos == 0.
	 * Example: if now is 12:00:12.3, sleep to 12:00:30.0
	 *          if now is 12:00:30.1, sleep to 12:01:00.0
	 */
	private static void sleepUntilNextMinuteBoundary() throws InterruptedException {
		ZoneId zone = ZoneId.systemDefault();
		
		while (running) {
			ZonedDateTime now = ZonedDateTime.now(zone);
			
			ZonedDateTime target = now
					.plusMinutes(1)
					.withSecond(0)
					.withNano(0);
			
			long sleepMillis = Duration.between(now, target).toMillis();
			
			if (sleepMillis <= 0) return;
			
			// Sleep in small chunks so Ctrl+C stops quickly
			long chunk = Math.min(250, sleepMillis);
			Thread.sleep(chunk);
			
			if (Duration.between(ZonedDateTime.now(zone), target).toMillis() <= 0) return;
		}
	}
	private static void clearConsole() {
		// ANSI clear screen + cursor home. If your console doesn’t support ANSI, remove this.
		System.out.print("\u001b[2J\u001b[H");
		System.out.flush();
	}
}