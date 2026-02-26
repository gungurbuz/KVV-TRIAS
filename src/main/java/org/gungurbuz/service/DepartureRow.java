package org.gungurbuz.service;

public record DepartureRow(
		String line,
		String direction,
		String schedHHmm,   // local time
		String realHHmm     // local time, may be null
) {}