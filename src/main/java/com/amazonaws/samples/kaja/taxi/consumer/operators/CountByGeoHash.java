package com.amazonaws.samples.kaja.taxi.consumer.operators;

import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import com.amazonaws.samples.kaja.taxi.consumer.events.es.PickupCount;
import com.amazonaws.samples.kaja.taxi.consumer.events.flink.TripGeoHash;
import com.amazonaws.samples.kaja.taxi.consumer.utils.GeoUtils;
import com.google.common.collect.Iterables;

public class CountByGeoHash implements WindowFunction<TripGeoHash, PickupCount, String, TimeWindow> {
	private static final long serialVersionUID = 1;

	@Override
	public void apply(String key, TimeWindow timeWindow, Iterable<TripGeoHash> iterable, Collector<PickupCount> collector) throws Exception {
		long count = Iterables.size(iterable);

		String geohash = Iterables.get(iterable, 0).geoHash;
		String location = GeoUtils.geoHashToGeoPoint(geohash);

		collector.collect(new PickupCount(location, geohash, count, timeWindow.getEnd()));
	}
}
