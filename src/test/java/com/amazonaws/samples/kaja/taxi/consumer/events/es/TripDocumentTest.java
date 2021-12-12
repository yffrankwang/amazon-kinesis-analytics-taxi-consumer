package com.amazonaws.samples.kaja.taxi.consumer.events.es;

import org.junit.Test;

public class TripDocumentTest {

	@Test
	public void testToString() throws Exception {
		TripDocument td = new TripDocument();
		System.out.println(td);
	}
}
