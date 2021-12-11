package com.amazonaws.samples.kaja.taxi.consumer.events.kinesis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.Test;

import com.amazonaws.samples.kaja.taxi.consumer.events.EventDeserializationSchema;
import com.amazonaws.samples.kaja.taxi.consumer.utils.GeoUtils;

public class EventDeserializationSchemaTest {

	private void deserialize(String fin) throws Exception {
		if (!(new File(fin).canRead())) {
			return;
		}
		
		EventDeserializationSchema edss = new EventDeserializationSchema();

		BufferedReader br = new BufferedReader(new FileReader(fin));

		int errgeo = 0;
		int lineno = 0;
		String line;
		while ((line = br.readLine()) != null) {
			lineno++;
			Event evt = edss.deserialize(line.getBytes());
			if (evt == null || !(evt instanceof TripEvent)) {
				System.out.println(lineno + " - error event: " + line);
				return;
			}

			TripEvent te = (TripEvent)evt;
			if (!GeoUtils.hasValidCoordinates(te)) {
				if (errgeo % 10000 == 0) {
					System.out.println(lineno + " - error geo: " + line);
				}
				errgeo++;
			}
		}
		
		System.out.println("geo error " + errgeo + " / " + lineno);
	}
	
	@Test
	public void testDeserializeGreen() throws Exception {
		deserialize("D:\\Develop\\Projects\\aws\\tlc\\data\\green_tripdata_2018-01.csv.json");
	}
	
	@Test
	public void testDeserializeYellow() throws Exception {
		deserialize("D:\\Develop\\Projects\\aws\\tlc\\data\\yellow_tripdata_2018-01.csv.json");
	}
}
