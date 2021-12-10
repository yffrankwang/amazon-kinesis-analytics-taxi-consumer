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

package com.amazonaws.samples.kaja.taxi.consumer.operators;

import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;

public class AmazonS3FileSink {
	public static <T> StreamingFileSink<T> buildS3FileSink(String s3SinkPath) {
		final StreamingFileSink<T> sink = StreamingFileSink
			.forRowFormat(new Path(s3SinkPath), new SimpleStringEncoder<T>("UTF-8"))
			.withBucketAssigner(new DateTimeBucketAssigner<T>("yyyyMMdd-HHmm"))
			.withRollingPolicy(DefaultRollingPolicy.builder().build())
			.build();

		return sink;
	}
}
