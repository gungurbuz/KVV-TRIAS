package org.gungurbuz.data;

public record StopEvent(
		String line,
		String direction,
		String timetabledTimeUtc,   // e.g. 2026-02-26T22:42:35Z
		String estimatedTimeUtc     // may be null
) {}