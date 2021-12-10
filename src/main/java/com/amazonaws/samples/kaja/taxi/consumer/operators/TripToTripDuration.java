package com.amazonaws.samples.kaja.taxi.consumer.operators;

import ch.hsr.geohash.GeoHash;
import com.amazonaws.samples.kaja.taxi.consumer.events.flink.TripDuration;
import com.amazonaws.samples.kaja.taxi.consumer.events.kinesis.TripEvent;
import com.amazonaws.samples.kaja.taxi.consumer.utils.GeoUtils;
import java.time.Duration;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

public class TripToTripDuration implements FlatMapFunction<TripEvent, TripDuration> {
	private static final long serialVersionUID = 1L;

	@Override
	public void flatMap(TripEvent tripEvent, Collector<TripDuration> collector) {
		String pickupGeoHash = GeoHash.geoHashStringWithCharacterPrecision(tripEvent.pickupLatitude, tripEvent.pickupLongitude, 6);

		long tripDuration = Duration.between(tripEvent.pickupDatetime, tripEvent.dropoffDatetime).toMinutes();

		if (GeoUtils.nearJFK(tripEvent.dropoffLatitude, tripEvent.dropoffLongitude)) {
			collector.collect(new TripDuration(tripDuration, pickupGeoHash, "JFK"));
		} else if (GeoUtils.nearLGA(tripEvent.dropoffLatitude, tripEvent.dropoffLongitude)) {
			collector.collect(new TripDuration(tripDuration, pickupGeoHash, "LGA"));
		}
	}
}
