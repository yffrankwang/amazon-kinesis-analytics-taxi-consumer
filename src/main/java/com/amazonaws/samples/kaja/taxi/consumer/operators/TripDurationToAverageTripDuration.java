package com.amazonaws.samples.kaja.taxi.consumer.operators;

import java.util.stream.StreamSupport;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import com.amazonaws.samples.kaja.taxi.consumer.events.es.AverageTripDuration;
import com.amazonaws.samples.kaja.taxi.consumer.events.flink.TripDuration;
import com.amazonaws.samples.kaja.taxi.consumer.utils.GeoUtils;
import com.google.common.collect.Iterables;

public class TripDurationToAverageTripDuration implements WindowFunction<TripDuration, AverageTripDuration, Tuple2<String, String>, TimeWindow> {
	private static final long serialVersionUID = 1L;

	@Override
	public void apply(Tuple2<String, String> tuple, TimeWindow timeWindow, Iterable<TripDuration> iterable, Collector<AverageTripDuration> collector) {
		if (Iterables.size(iterable) > 1) {
			TripDuration tripDuration = Iterables.get(iterable, 0);

			String geohash = tripDuration.pickupGeoHash;
			String location = GeoUtils.geoHashToGeoPoint(geohash);
			String airportCode = tripDuration.airportCode;

			long sumDuration = StreamSupport
					.stream(iterable.spliterator(), false)
					.mapToLong(trip -> trip.tripDuration)
					.sum();

			double avgDuration = (double) sumDuration / Iterables.size(iterable);

			collector.collect(new AverageTripDuration(location, geohash, airportCode, sumDuration, avgDuration, timeWindow.getEnd()));
		}
	}
}
