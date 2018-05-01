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

import org.apache.lucene.geo.Rectangle;
import org.apache.lucene.util.BitUtil;

import static org.elasticsearch.common.geo.GeoUtils.normalizeLat;
import static org.elasticsearch.common.geo.GeoUtils.normalizeLon;


/**
 * Converting geopoints to Spherical Mercator in the same way as map tile services
 */
public class MaptileHash {

    private static final int MAX_ZOOM = 26;

    /**
     * Convert [longitude, latitude] to a hash tha combines zoom, x, and y of the tile.
     */
    public static long geoToMapTileHash(final double longitude, final double latitude, final int zoom) {
        // Adapted from https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java

        if (zoom < 0 || zoom > MAX_ZOOM) {
            throw new IllegalArgumentException("zoom");
        }
        // How many tiles in X and in Y
        final int tiles = 1 << zoom;
        final double lon = normalizeLon(longitude);
        final double lat = normalizeLat(latitude);

        int xtile = (int) Math.floor((lon + 180) / 360 * tiles);
        int ytile = (int) Math.floor(
            (1 - Math.log(
                Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))
            ) / Math.PI) / 2 * tiles);
        if (xtile < 0)
            xtile = 0;
        if (xtile >= tiles)
            xtile = (tiles - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= tiles)
            ytile = (tiles - 1);

        // with max zoom being 26, the largest index would be 2^52 (51st..0th),
        // leaving 12 bits unused. Zoom cannot be >26, so it can fit into 5 bits (56th..52nd)
        return BitUtil.interleave(xtile, ytile) | ((long) zoom << 52);
    }

    private static int[] parseMapTileHash(final long hash) {
        int zoom = (int) (hash >> 52);
        if (zoom < 0 || zoom > MAX_ZOOM) {
            throw new IllegalArgumentException("hash-zoom");
        }

        final int tiles = 1 << zoom;
        // decode last 52 bits as xtile and ytile
        long val = hash & 0x000FFFFFFFFFFFFFL;
        int xtile = (int) BitUtil.deinterleave(val);
        int ytile = (int) BitUtil.deinterleave(val >> 1);
        if (xtile < 0 || ytile < 0 || xtile >= tiles || ytile >= tiles) {
            throw new IllegalArgumentException("hash-tile");
        }

        return new int[]{zoom, xtile, ytile};
    }

    public static String geoTileMapHashToKey(final long geohashAsLong) {
        int[] res = parseMapTileHash(geohashAsLong);
        return "" + res[0] + "/" + res[1] + "/" + res[2];
    }

    private static double tile2lon(final int x, final double tiles) {
        return x / tiles * 360.0 - 180;
    }

    private static double tile2lat(final int y, final double tiles) {
        double n = Math.PI - (2.0 * Math.PI * y) / tiles;
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    public static Rectangle bboxFromTileIndex(long geohashAsLong) {
        int[] res = parseMapTileHash(geohashAsLong);
        int zoom = res[0], x = res[1], y = res[2];
        double tiles = Math.pow(2.0, zoom); // optimization

        return new Rectangle(tile2lat(y, tiles), tile2lat(y + 1, tiles),
            tile2lon(x, tiles), tile2lon(x + 1, tiles));
    }

    /**
     * Validate precision parameter
     * @param precision as submitted by the user
     */
    public static void validatePrecision(int precision) {
        if (precision < 0 || precision > MAX_ZOOM) {
            throw new IllegalArgumentException("Invalid geohash maptile aggregation precision of " + precision +
                ". Must be between 0 and " + MAX_ZOOM + ".");
        }
    }

    private MaptileHash() {
    }

}
