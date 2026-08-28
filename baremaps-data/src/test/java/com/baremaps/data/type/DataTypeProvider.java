/*
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.data.type;


import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.locationtech.jts.geom.*;

public class DataTypeProvider {

  private static final GeometryFactory geometryFactory = new GeometryFactory();

  private static Stream<Arguments> dataTypes() {
    return Stream.of(
        // String
        Arguments.of(new StringDataType(), ""),
        Arguments.of(new StringDataType(), "Hello, World!"),

        // Boolean
        Arguments.of(new BooleanDataType(), true),
        Arguments.of(new BooleanDataType(), false),
        Arguments.of(new BooleanArrayDataType(), new boolean[] {}),
        Arguments.of(new BooleanArrayDataType(), new boolean[] {true, false}),
        Arguments.of(new BooleanListDataType(), List.of()),
        Arguments.of(new BooleanListDataType(), List.of(true, false)),

        // Byte
        Arguments.of(new ByteDataType(), Byte.MIN_VALUE),
        Arguments.of(new ByteDataType(), Byte.MAX_VALUE),
        Arguments.of(new ByteDataType(), (byte) 0b0),
        Arguments.of(new ByteDataType(), (byte) 0b1),
        Arguments.of(new ByteDataType(), (byte) -0b1),
        Arguments.of(new ByteListDataType(), List.of()),
        Arguments.of(new ByteListDataType(), List.of((byte) 1, (byte) 2, (byte) 3)),
        Arguments.of(new ByteArrayDataType(), new byte[] {}),
        Arguments.of(new ByteArrayDataType(), new byte[] {(byte) 1, (byte) 2, (byte) 3}),

        // Double
        Arguments.of(new DoubleDataType(), Double.MIN_VALUE),
        Arguments.of(new DoubleDataType(), Double.MAX_VALUE),
        Arguments.of(new DoubleDataType(), Double.NaN),
        Arguments.of(new DoubleDataType(), Double.NEGATIVE_INFINITY),
        Arguments.of(new DoubleDataType(), Double.POSITIVE_INFINITY),
        Arguments.of(new DoubleDataType(), 0d),
        Arguments.of(new DoubleDataType(), 1d),
        Arguments.of(new DoubleDataType(), -1d),

        Arguments.of(new DoubleArrayDataType(), new double[] {}),
        Arguments.of(new DoubleArrayDataType(), new double[] {(double) 1, (double) 2, (double) 3}),
        Arguments.of(new DoubleListDataType(), List.of()),
        Arguments.of(new DoubleListDataType(), List.of((double) 1, (double) 2, (double) 3)),

        // Float
        Arguments.of(new FloatDataType(), Float.MIN_VALUE),
        Arguments.of(new FloatDataType(), Float.MAX_VALUE),
        Arguments.of(new FloatDataType(), Float.NaN),
        Arguments.of(new FloatDataType(), Float.NEGATIVE_INFINITY),
        Arguments.of(new FloatDataType(), Float.POSITIVE_INFINITY),
        Arguments.of(new FloatDataType(), 0f),
        Arguments.of(new FloatDataType(), 1f),
        Arguments.of(new FloatDataType(), -1f),
        Arguments.of(new FloatArrayDataType(), new float[] {}),
        Arguments.of(new FloatArrayDataType(), new float[] {(float) 1, (float) 2, (float) 3}),
        Arguments.of(new FloatListDataType(), List.of()),
        Arguments.of(new FloatListDataType(), List.of((float) 1, (float) 2, (float) 3)),

        // Integer
        Arguments.of(new IntegerDataType(), Integer.MIN_VALUE),
        Arguments.of(new IntegerDataType(), Integer.MAX_VALUE),
        Arguments.of(new IntegerDataType(), 0),
        Arguments.of(new IntegerDataType(), 1),
        Arguments.of(new IntegerDataType(), -1),
        Arguments.of(new IntegerArrayDataType(), new int[] {}),
        Arguments.of(new IntegerArrayDataType(), new int[] {1, 2, 3}),
        Arguments.of(new IntegerListDataType(), List.of()),
        Arguments.of(new IntegerListDataType(), List.of(1, 2, 3)),

        // Long
        Arguments.of(new LongDataType(), Long.MIN_VALUE),
        Arguments.of(new LongDataType(), Long.MAX_VALUE),
        Arguments.of(new LongDataType(), 0l),
        Arguments.of(new LongDataType(), 1l),
        Arguments.of(new LongDataType(), -1l),
        Arguments.of(new LongArrayDataType(), new long[] {}),
        Arguments.of(new LongArrayDataType(), new long[] {1l, 2l, 3l}),
        Arguments.of(new LongListDataType(), List.of()),
        Arguments.of(new LongListDataType(), List.of(1l, 2l, 3l)),

        // Short
        Arguments.of(new ShortDataType(), Short.MIN_VALUE),
        Arguments.of(new ShortDataType(), Short.MAX_VALUE),
        Arguments.of(new ShortDataType(), (short) 0),
        Arguments.of(new ShortDataType(), (short) 1),
        Arguments.of(new ShortDataType(), (short) -1),
        Arguments.of(new ShortArrayDataType(), new short[] {}),
        Arguments.of(new ShortArrayDataType(), new short[] {(short) 1, (short) 2, (short) 3}),
        Arguments.of(new ShortListDataType(), List.of()),
        Arguments.of(new ShortListDataType(), List.of((short) 1, (short) 2, (short) 3)),

        // List
        Arguments.of(new ListDataType<>(new IntegerDataType()), List.of(0)),
        Arguments.of(new ListDataType<>(new IntegerDataType()), List.of(1, 2, 3)),
        Arguments.of(new ListDataType<>(new StringDataType()), List.of()),
        Arguments.of(new ListDataType<>(new StringDataType()), List.of("aaaa", "bbbb", "cccc")),

        // Map
        Arguments.of(new MapDataType<>(new IntegerDataType(), new IntegerDataType()),
            Map.of()),
        Arguments.of(new MapDataType<>(new IntegerDataType(), new IntegerDataType()),
            Map.of(1, 2, 3, 4, 5, 6)),
        Arguments.of(new MapDataType<>(new StringDataType(), new StringDataType()),
            Map.of()),
        Arguments.of(new MapDataType<>(new StringDataType(), new StringDataType()),
            Map.of("k1", "v1", "k2", "v2", "k3", "v3")),
        Arguments.of(
            new MapDataType<>(new StringDataType(), new ListDataType<>(new StringDataType())),
            Map.of("k1", List.of(), "k2", List.of("v1", "v2", "v3"))),

        Arguments.of(new CoordinateDataType(), new Coordinate(-180, -90)),
        Arguments.of(new CoordinateDataType(), new Coordinate(180, 90)),
        Arguments.of(new LonLatDataType(), new Coordinate(-180, -90)),
        Arguments.of(new LonLatDataType(), new Coordinate(180, 90)),
        Arguments.of(new SmallIntegerDataType(1), (int) Math.pow(2, 7) - 1),
        Arguments.of(new SmallIntegerDataType(1), (int) -Math.pow(2, 7)),
        Arguments.of(new SmallIntegerDataType(2), (int) Math.pow(2, 15) - 1),
        Arguments.of(new SmallIntegerDataType(2), (int) -Math.pow(2, 15)),
        Arguments.of(new SmallIntegerDataType(3), (int) Math.pow(2, 23) - 1),
        Arguments.of(new SmallIntegerDataType(3), (int) -Math.pow(2, 23)),
        Arguments.of(new SmallIntegerDataType(4), Integer.MAX_VALUE),
        Arguments.of(new SmallIntegerDataType(4), Integer.MIN_VALUE),
        Arguments.of(new SmallLongDataType(1), (long) Math.pow(2, 7) - 1),
        Arguments.of(new SmallLongDataType(1), (long) -Math.pow(2, 7)),
        Arguments.of(new SmallLongDataType(2), (long) Math.pow(2, 15) - 1),
        Arguments.of(new SmallLongDataType(2), (long) -Math.pow(2, 15)),
        Arguments.of(new SmallLongDataType(3), (long) Math.pow(2, 23) - 1),
        Arguments.of(new SmallLongDataType(3), (long) -Math.pow(2, 23)),
        Arguments.of(new SmallLongDataType(4), (long) Integer.MAX_VALUE),
        Arguments.of(new SmallLongDataType(4), (long) Integer.MIN_VALUE),
        Arguments.of(new SmallLongDataType(5), (long) Math.pow(2, 39) - 1),
        Arguments.of(new SmallLongDataType(5), (long) -Math.pow(2, 39)),
        Arguments.of(new SmallLongDataType(6), (long) Math.pow(2, 47) - 1),
        Arguments.of(new SmallLongDataType(6), (long) -Math.pow(2, 47)),
        Arguments.of(new SmallLongDataType(7), (long) Math.pow(2, 55) - 1),
        Arguments.of(new SmallLongDataType(7), (long) -Math.pow(2, 55)),
        Arguments.of(new SmallLongDataType(8), Long.MAX_VALUE),
        Arguments.of(new SmallLongDataType(8), Long.MIN_VALUE),
        Arguments.of(
            new PairDataType<>(new LongDataType(), new LongDataType()),
            new PairDataType.Pair<>(1l, 2l)),

        // WKB (Well Known Binary)
        Arguments.of(new WKBDataType(),
            new GeometryFactory().createEmpty(0)),
        Arguments.of(new WKBDataType(),
            new GeometryFactory().createPoint(new Coordinate(1, 1))),
        Arguments.of(new WKBDataType(),
            new GeometryFactory()
                .createLineString(new Coordinate[] {new Coordinate(1, 1), new Coordinate(2, 2)})),

        // Point
        Arguments.of(new PointDataType(),
            new GeometryFactory().createPoint()),
        Arguments.of(new PointDataType(),
            new GeometryFactory()
                .createPoint(new Coordinate(1, 1))),

        // MultiPoint
        Arguments.of(new MultiPointDataType(),
            new GeometryFactory().createMultiPoint()),
        Arguments.of(new MultiPointDataType(),
            new GeometryFactory()
                .createMultiPoint(new Coordinate[] {new Coordinate(1, 1), new Coordinate(2, 2)})),

        // LineString
        Arguments.of(new LineStringDataType(),
            new GeometryFactory().createLineString()),
        Arguments.of(new LineStringDataType(),
            new GeometryFactory()
                .createLineString(new Coordinate[] {new Coordinate(1, 1), new Coordinate(2, 2)})),

        // MultiLineString
        Arguments.of(new MultiLineStringDataType(),
            new GeometryFactory().createMultiLineString()),
        Arguments.of(new MultiLineStringDataType(),
            new GeometryFactory()
                .createMultiLineString(
                    new LineString[] {
                        new GeometryFactory()
                            .createLineString(
                                new Coordinate[] {new Coordinate(1, 1), new Coordinate(2, 2)}),
                        new GeometryFactory()
                            .createLineString(
                                new Coordinate[] {new Coordinate(3, 3), new Coordinate(4, 4)})})),

        // Polygon
        Arguments.of(new PolygonDataType(),
            new GeometryFactory().createPolygon()),
        Arguments.of(new PolygonDataType(),
            new GeometryFactory()
                .createPolygon(
                    new GeometryFactory()
                        .createLinearRing(
                            new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 3),
                                new Coordinate(5, 3), new Coordinate(5, 0), new Coordinate(0, 0)}),
                    new LinearRing[] {
                        new GeometryFactory()
                            .createLinearRing(new Coordinate[] {new Coordinate(1, 1),
                                new Coordinate(1, 2), new Coordinate(2, 2), new Coordinate(2, 1),
                                new Coordinate(1, 1)}),
                        new GeometryFactory()
                            .createLinearRing(new Coordinate[] {new Coordinate(3, 1),
                                new Coordinate(3, 2), new Coordinate(4, 2), new Coordinate(4, 1),
                                new Coordinate(3, 1)})})),

        // MultiPolygon
        Arguments.of(new MultiPolygonDataType(),
            new GeometryFactory().createMultiPolygon()),
        Arguments.of(new MultiPolygonDataType(),
            new GeometryFactory()
                .createMultiPolygon(
                    new Polygon[] {
                        new GeometryFactory()
                            .createPolygon(
                                new GeometryFactory()
                                    .createLinearRing(new Coordinate[] {new Coordinate(0, 0),
                                        new Coordinate(0, 3), new Coordinate(5, 3),
                                        new Coordinate(5, 0), new Coordinate(0, 0)}),
                                new LinearRing[] {
                                    new GeometryFactory()
                                        .createLinearRing(new Coordinate[] {new Coordinate(1, 1),
                                            new Coordinate(1, 2), new Coordinate(2, 2),
                                            new Coordinate(2, 1), new Coordinate(1, 1)}),
                                    new GeometryFactory()
                                        .createLinearRing(new Coordinate[] {new Coordinate(3, 1),
                                            new Coordinate(3, 2), new Coordinate(4, 2),
                                            new Coordinate(4, 1), new Coordinate(3, 1)})}),
                        new GeometryFactory()
                            .createPolygon(
                                new GeometryFactory()
                                    .createLinearRing(new Coordinate[] {new Coordinate(1, 4),
                                        new Coordinate(1, 5), new Coordinate(2, 5),
                                        new Coordinate(2, 4), new Coordinate(1, 4)}))})),

        // GeometryCollection
        Arguments.of(new GeometryCollectionDataType(),
            new GeometryFactory().createGeometryCollection()),
        Arguments.of(new GeometryCollectionDataType(),
            new GeometryFactory()
                .createGeometryCollection(
                    new Geometry[] {
                        new GeometryFactory()
                            .createPoint(new Coordinate(1, 1)),
                        new GeometryFactory()
                            .createLineString(
                                new Coordinate[] {new Coordinate(1, 1), new Coordinate(2, 2)}),
                        new GeometryFactory()
                            .createPolygon(
                                new GeometryFactory()
                                    .createLinearRing(new Coordinate[] {new Coordinate(0, 0),
                                        new Coordinate(0, 3), new Coordinate(5, 3),
                                        new Coordinate(5, 0), new Coordinate(0, 0)}),
                                new LinearRing[] {
                                    new GeometryFactory()
                                        .createLinearRing(new Coordinate[] {new Coordinate(1, 1),
                                            new Coordinate(1, 2), new Coordinate(2, 2),
                                            new Coordinate(2, 1), new Coordinate(1, 1)}),
                                    new GeometryFactory()
                                        .createLinearRing(new Coordinate[] {new Coordinate(3, 1),
                                            new Coordinate(3, 2), new Coordinate(4, 2),
                                            new Coordinate(4, 1), new Coordinate(3, 1)})})})),

        // Geometry
        Arguments.of(new GeometryDataType(),
            new GeometryFactory().createEmpty(0)),
        Arguments.of(new GeometryDataType(),
            new GeometryFactory()
                .createPoint(new Coordinate(1, 1))));


  }
}
