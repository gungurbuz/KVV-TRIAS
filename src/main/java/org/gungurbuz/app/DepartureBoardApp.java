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
	private static final List<String> ALLOWED_LINES = List.of();
	
	private static volatile boolean running = true;
	
	public static void main(String[] args) throws Exception {
		
		String apiKey = System.getenv("TRIAS_API_KEY");
		if (apiKey == null || apiKey.isBlank()) {
			throw new RuntimeException("TRIAS_API_KEY not set!");
		}
		
		TriasClient client = new TriasClient(URL, apiKey);
		TriasStopEventParser parser = new TriasStopEventParser();
		DepartureBoardService service = new DepartureBoardService(client, parser);
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
		
		// Immediate first refresh
		try {
			updateDisplay(service);
		} catch (Exception e) {
			System.out.println("Update failed: " + e.getMessage());
		}
		
		// Then align to the next system minute and refresh every minute
		while (running) {
			sleepUntilNextMinuteBoundary();
			
			try {
				updateDisplay(service);
			} catch (Exception e) {
				System.out.println("Update failed: " + e.getMessage());
			}
		}
	}
	
	private static void updateDisplay(DepartureBoardService service) throws Exception {
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
	}
	
	/**
	 * Sleeps until the next full minute boundary (seconds == 0, nanos == 0).
	 * Example:
	 *   12:00:12 -> 12:01:00
	 *   12:00:59.5 -> 12:01:00
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