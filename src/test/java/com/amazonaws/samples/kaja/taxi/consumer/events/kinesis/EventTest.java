package com.amazonaws.samples.kaja.taxi.consumer.events.kinesis;

import org.junit.Assert;
import org.junit.Test;

public class EventTest {

	@Test
	public void testParse() {
		String data = "{\"dropoff_datetime\":\"2018-01-01 03:56:26\",\"dropoff_location_id\":\"7\",\"passenger_count\":1,\"pickup_datetime\":\"2018-01-01 03:52:40\",\"pickup_location_id\":\"179\",\"rate_code\":\"1\",\"store_and_fwd_flag\":\"N\",\"type\":\"green\",\"vendor_id\":\"2\"}";
		
		Event evt = Event.parseEvent(data.getBytes());
		
		Assert.assertTrue(evt instanceof TripEvent);
		
		TripEvent te = (TripEvent)evt;
		Assert.assertEquals(te.taxiType, "green");
		
		System.out.println(evt);
	}
}
