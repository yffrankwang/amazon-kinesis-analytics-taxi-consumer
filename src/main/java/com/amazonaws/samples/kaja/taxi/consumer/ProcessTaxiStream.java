/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may
 * not use this file except in compliance with the License. A copy of the
 * License is located at
 *
 *    http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.samples.kaja.taxi.consumer;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer;
import org.apache.flink.streaming.connectors.kinesis.config.AWSConfigConstants;
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants;
import org.apache.flink.streaming.runtime.operators.util.AssignerWithPunctuatedWatermarksAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.amazonaws.samples.kaja.taxi.consumer.events.EventDeserializationSchema;
import com.amazonaws.samples.kaja.taxi.consumer.events.TimestampAssigner;
import com.amazonaws.samples.kaja.taxi.consumer.events.es.TripDocument;
import com.amazonaws.samples.kaja.taxi.consumer.events.kinesis.Event;
import com.amazonaws.samples.kaja.taxi.consumer.events.kinesis.TripEvent;
import com.amazonaws.samples.kaja.taxi.consumer.events.kinesis.TripEventValidator;
import com.amazonaws.samples.kaja.taxi.consumer.operators.AmazonElasticsearchSink;
import com.amazonaws.samples.kaja.taxi.consumer.operators.AmazonS3FileSink;
import com.amazonaws.samples.kaja.taxi.consumer.operators.CalcByGeoHash;
import com.amazonaws.samples.kaja.taxi.consumer.operators.TripEventToTripData;
import com.amazonaws.samples.kaja.taxi.consumer.utils.ParameterToolUtils;
import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;


public class ProcessTaxiStream {
	private static final Logger LOG = LoggerFactory.getLogger(ProcessTaxiStream.class);

	private static final String DEFAULT_STREAM_NAME = "streaming-analytics-workshop";
	private static final String DEFAULT_REGION_NAME = Regions.getCurrentRegion()==null ? "us-east-1" : Regions.getCurrentRegion().getName();
	private static final String DEFAULT_INDEX_NAME = "trip";

	public static void main(String[] args) throws Exception {
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		ParameterTool parameter;

		if (env instanceof LocalStreamEnvironment) {
			//read the parameters specified from the command line
			parameter = ParameterTool.fromArgs(args);
		} else {
			//read the parameters from the Kinesis Analytics environment
			Map<String, Properties> applicationProperties = KinesisAnalyticsRuntime.getApplicationProperties();

			Properties flinkProperties = applicationProperties.get("FlinkApplicationProperties");

			if (flinkProperties == null) {
				throw new RuntimeException("Unable to load FlinkApplicationProperties properties from the Kinesis Analytics Runtime.");
			}

			parameter = ParameterToolUtils.fromApplicationProperties(flinkProperties);
		}

		//enable event time processing
		env.getConfig().setAutoWatermarkInterval(parameter.get("EventTime", "true").equals("true") ? 200 : 0);

		//set Kinesis consumer properties
		Properties kinesisConsumerConfig = new Properties();
		//set the region the Kinesis stream is located in
		kinesisConsumerConfig.setProperty(AWSConfigConstants.AWS_REGION, parameter.get("Region", DEFAULT_REGION_NAME));
		//obtain credentials through the DefaultCredentialsProviderChain, which includes the instance metadata
		kinesisConsumerConfig.setProperty(AWSConfigConstants.AWS_CREDENTIALS_PROVIDER, "AUTO");
		//poll new events from the Kinesis stream once every second
		kinesisConsumerConfig.setProperty(ConsumerConfigConstants.SHARD_GETRECORDS_INTERVAL_MILLIS, "1000");


		//create Kinesis source
		DataStream<Event> kinesisStream = env.addSource(new FlinkKinesisConsumer<>(
				//read events from the Kinesis stream passed in as a parameter
				parameter.get("InputStreamName", DEFAULT_STREAM_NAME),
				//deserialize events with EventSchema
				new EventDeserializationSchema(),
				//using the previously defined properties
				kinesisConsumerConfig
		));


		DataStream<TripEvent> tripEvents = kinesisStream
				//extract watermarks from watermark events
				.assignTimestampsAndWatermarks(new AssignerWithPunctuatedWatermarksAdapter.Strategy<>(new TimestampAssigner()))
				//remove all events that aren't TripEvents
				.filter(event -> TripEvent.class.isAssignableFrom(event.getClass()))
				//cast Event to TripEvent
				.map(event -> (TripEvent) event)
				//remove all events with geo coordinates outside of NYC
				.filter(TripEventValidator::isValidTrip);


		DataStream<TripDocument> tripDocs = tripEvents
				//compute geo hash for every event
				.map(new TripEventToTripData())
				.keyBy(item -> item.geohash)
				//collect all events in 10 minutes time window
				.window(TumblingEventTimeWindows.of(Time.minutes(10)))
				//count events per geo hash in the time window
				.apply(new CalcByGeoHash());

		String elasticsearchEndpoint = parameter.get("ElasticsearchEndpoint");
		if (StringUtils.isNotEmpty(elasticsearchEndpoint)) {
			String region = parameter.get("Region", DEFAULT_REGION_NAME);
			String index = parameter.get("IndexName", DEFAULT_INDEX_NAME);

			//remove trailling /
			elasticsearchEndpoint = StringUtils.stripEnd(elasticsearchEndpoint, "/");

			LOG.info("Enable sink to elasticsearch: [{}] {}", index, elasticsearchEndpoint);

			tripDocs.addSink(AmazonElasticsearchSink.buildElasticsearchSink(elasticsearchEndpoint, region, index, "_doc"));
		}

		String s3SinkPath = parameter.get("S3SinkPath");
		if (StringUtils.isNotEmpty(s3SinkPath)) {
			LOG.info("Enable sink to s3: {}", s3SinkPath);

			// https://docs.aws.amazon.com/zh_cn/kinesisanalytics/latest/java/examples-s3.html
			tripDocs.addSink(AmazonS3FileSink.buildS3FileSink(s3SinkPath));
		}
		
		LOG.info("Reading events from stream {}", parameter.get("InputStreamName", DEFAULT_STREAM_NAME));

		env.execute();
	}
}