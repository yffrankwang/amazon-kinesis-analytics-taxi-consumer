package com.amazonaws.samples.kaja.taxi.consumer.operators;

import java.util.Iterator;

import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import com.amazonaws.samples.kaja.taxi.consumer.events.es.TripDocument;
import com.amazonaws.samples.kaja.taxi.consumer.events.flink.TripData;
import com.google.common.collect.Iterables;

public class CalcByGeoHash implements WindowFunction<TripData, TripDocument, String, TimeWindow> {
	private static final long serialVersionUID = 1;

	@Override
	public void apply(String key, TimeWindow timeWindow, Iterable<TripData> iterable, Collector<TripDocument> collector) throws Exception {
		long count = Iterables.size(iterable);
		if (count < 1) {
			return;
		}

		TripData data = Iterables.get(iterable, 0);
		TripDocument doc = new TripDocument(timeWindow.getEnd());

		doc.geohash = data.geohash;
		doc.location = data.location;
		doc.hotspot = data.hotspot;

		double sumTripSpeed = 0;

		Iterator<TripData> it = iterable.iterator();
		while (it.hasNext()) {
			data = it.next();
			doc.sumTripDistance += data.tripDistance;
			doc.sumTripDuration = data.tripDuration;
			if (data.tripDuration > 0) {
				sumTripSpeed += (double)(data.tripDistance * 60) / data.tripDuration / 1000;
			}
		}

		doc.avgTripDuration = doc.sumTripDuration / count;
		doc.avgTripDistance = doc.sumTripDistance / count;
		doc.avgTripSpeed = sumTripSpeed / count;
		doc.pickupCount = count;

		collector.collect(doc);
	}

}
