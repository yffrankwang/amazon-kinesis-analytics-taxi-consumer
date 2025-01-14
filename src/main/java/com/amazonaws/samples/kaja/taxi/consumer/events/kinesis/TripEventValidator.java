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

package com.amazonaws.samples.kaja.taxi.consumer.events.kinesis;

import com.amazonaws.samples.kaja.taxi.consumer.utils.GeoUtils;


public class TripEventValidator {
	public static boolean hasValidDatetime(TripEvent te) {
		if (te.dropoffDatetime == null || te.pickupDatetime == null) {
			return false;
		}

		if (te.dropoffDatetime.getTime() < te.pickupDatetime.getTime()) {
			return false;
		}

//		if (te.dropoffDatetime.getTime() - te.pickupDatetime.getTime() > 12 * 60 * 60 * 1000) {
//		return false;
//	}
	
		return true;
	}
	
	public static boolean isValidTrip(TripEvent te) {
		if (!hasValidDatetime(te)) {
			return false;
		}
		
		if (!GeoUtils.hasValidCoordinates(te)) {
			return false;
		}
		
		return true;
	}
}
