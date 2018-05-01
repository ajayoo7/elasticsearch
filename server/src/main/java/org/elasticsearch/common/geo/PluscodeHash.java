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

import com.google.openlocationcode.OpenLocationCode;
import org.apache.lucene.geo.Rectangle;

import java.util.Arrays;

public class PluscodeHash {

    /**
     * Maximum plus code length (without the '+' symbol) that we support
     * 21^14 is the largest value that can fit within a long value
     */
    private static final int MAX_LENGTH = 14;

    /**
     * Same as official plus code alphabet, but also includes "0" to preserve the code length
     */
    private static final String ALPHABET0 = "023456789CFGHJMPQRVWX";

    /**
     * Length of the extended alphabet (21)
     */
    private static final int ALPHABET0_SIZE = ALPHABET0.length();

    // Initialize ALPHABET0_LOOKUP table for quick O(1) lookup of alphabet letters -> int
    // There is some wasted space (first 32 values, and a few gaps), but results is slightly better perf
    static {
        int size = ALPHABET0_SIZE;
        int[] lookup = new int[ALPHABET0.charAt(size - 1) + 1];
        Arrays.fill(lookup, -1);
        for (int i = 0; i < size; i++) {
            lookup[ALPHABET0.charAt(i)] = i;
        }
        ALPHABET0_LOOKUP = lookup;
    }

    private static final int[] ALPHABET0_LOOKUP;

    /**
     * Convert latitude+longitude to the plus code of a given length
     */
    public static String latLngToPluscode(final double lon, final double lat, final int codeLength) {
        return new OpenLocationCode(lat, lon, codeLength).getCode();
    }

    /**
     * Convert latitude+longitude to a hash value with a given precision.
     * Internally, the hash is created by converting plus code string into a base-21 number.
     * Plus codes use base 20, but they get appended with 0s if the precision is low.
     * Using base-21 allows us to preserve those zeroes
     */
    public static long latLngToPluscodeHash(final double lon, final double lat, final int codeLength) {

        String pluscode = latLngToPluscode(lon, lat, codeLength);

        long result = 0;
        for (int i = 0; i < pluscode.length(); i++) {
            char ch = pluscode.charAt(i);
            if (ch == '+') continue;
            int pos = ALPHABET0_LOOKUP[ch];
            if (pos < 0) {
                throw new IllegalArgumentException("Character '" + ch + "' is not a valid plus code");
            }
            result = result * ALPHABET0_SIZE + pos;
        }
        return result;
    }

    /**
     * Decode plus code hash back into a string
     */
    public static String decodePluscode(final long hash) {

        StringBuilder result = new StringBuilder(MAX_LENGTH + 1);

        long rest = hash;
        while (rest > 0) {
            long val = rest % ALPHABET0_SIZE;
            result.append(ALPHABET0.charAt((int) val));
            rest = rest / ALPHABET0_SIZE;
        }

        result.reverse();
        result.insert(8, '+');

        return result.toString();
    }

    /**
     * Computes the bounding box coordinates from a given geohash
     *
     * @param hashcode Geohash of the defined cell
     * @return Rectangle rectangle defining the bounding box
     */
    public static Rectangle bboxFromPluscode(final long hashcode) {
        final String pluscode = decodePluscode(hashcode);

        OpenLocationCode.CodeArea area = new OpenLocationCode(pluscode).decode();

        return new Rectangle(area.getSouthLatitude(), area.getNorthLatitude(),
            area.getWestLongitude(), area.getEastLongitude());
    }

    /**
     * Validate precision parameter
     * @param precision as submitted by the user
     */
    public static void validatePrecision(int precision) {
        if ((precision < 4) || (precision > MAX_LENGTH) ||
            (precision < OpenLocationCode.CODE_PRECISION_NORMAL && precision % 2 == 1)
        ) {
            throw new IllegalArgumentException("Invalid geohash pluscode aggregation precision of " + precision
                + ". Must be between 4 and " + MAX_LENGTH + " , and must be even if less than 8.");
        }
    }

    private PluscodeHash() {
    }
}
