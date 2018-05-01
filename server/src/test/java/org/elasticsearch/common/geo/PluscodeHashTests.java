/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.common.geo;

import org.elasticsearch.test.ESTestCase;

import static com.google.openlocationcode.OpenLocationCode.CODE_PRECISION_NORMAL;

/**
 * Tests for {@link PluscodeHash}
 */
public class PluscodeHashTests extends ESTestCase {
    public void testPluscodeAsLongRoutines() {
        final GeoPoint expected = new GeoPoint();
        final GeoPoint actual = new GeoPoint();
        //Ensure that for all points at all supported levels of precision
        // that the long encoding of a geohash is compatible with its
        // String based counterpart
        for (double lat = -90; lat < 90; lat++) {
            for (double lng = -180; lng < 180; lng++) {
                for (int p = 4; p <= 14; p++) {
                    // code must be even if less than 10 digits
                    if (p < CODE_PRECISION_NORMAL && p % 2 == 1) continue;

                    long geoAsLong = PluscodeHash.latLngToPluscodeHash(lng, lat, p);

                    // string encode from geohashlong encoded location
                    String geohashFromLong = PluscodeHash.decodePluscode(geoAsLong);

                    // string encode from full res lat lon
                    String geohash = PluscodeHash.latLngToPluscode(lng, lat, p);

                    // ensure both strings are the same
                    assertEquals(geohash, geohashFromLong);
                }
            }
        }
    }

}
