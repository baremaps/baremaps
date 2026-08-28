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

package com.baremaps.flatgeobuf;

import static org.junit.jupiter.api.Assertions.*;

import com.baremaps.flatgeobuf.generated.Feature;
import com.baremaps.flatgeobuf.generated.Header;
import com.baremaps.testing.TestFiles;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlatGeoBufTest {

  @Test
  void readWrite() throws IOException {
    Path file = TestFiles.POINT_FLATGEOBUF;
    Path tempFile = Files.createTempFile("countries", ".fgb");

    Header headerFlatGeoBuf1 = null;
    FlatGeoBuf.Header headerRecord1 = null;
    List<FlatGeoBuf.Feature> featureRecords = new ArrayList<>();

    // Copy the file
    try (
        FlatGeoBufReader reader =
            new FlatGeoBufReader(FileChannel.open(file, StandardOpenOption.READ));
        FlatGeoBufWriter writer =
            new FlatGeoBufWriter(FileChannel.open(tempFile, StandardOpenOption.WRITE))) {

      // Copy the header
      headerRecord1 = reader.readHeader();
      headerFlatGeoBuf1 = FlatGeoBufWriter.asFlatBuffer(headerRecord1);
      writer.writeHeader(headerRecord1);

      // Copy the index
      ByteBuffer indexBuffer = reader.readIndexBuffer();
      writer.writeIndexBuffer(indexBuffer);

      // Copy the features
      for (long i = 0; i < headerFlatGeoBuf1.featuresCount(); i++) {
        FlatGeoBuf.Feature feature = reader.readFeature();
        writer.writeFeature(feature);
        featureRecords.add(feature);
      }
    }

    // Read the copied file
    try (FlatGeoBufReader reader =
        new FlatGeoBufReader(FileChannel.open(tempFile, StandardOpenOption.READ))) {

      // Read the header
      FlatGeoBuf.Header headerRecord2 = reader.readHeader();
      Header headerFlatGeoBuf2 = FlatGeoBufWriter.asFlatBuffer(headerRecord2);
      assertNotNull(headerFlatGeoBuf2);
      assertEquals(headerRecord1, headerRecord2);

      // Read the index
      reader.skipIndex();

      // Read the features
      for (long i = 0; i < headerFlatGeoBuf2.featuresCount(); i++) {
        FlatGeoBuf.Feature featureRecord = reader.readFeature();
        assertNotNull(featureRecord);
        assertEquals(featureRecords.get((int) i), featureRecord);
      }
    }
  }

  @Test
  void readWriteBuffer() throws IOException {
    Path file = TestFiles.POINT_FLATGEOBUF;
    Path tempFile = Files.createTempFile("countries", ".fgb");

    Header headerFlatGeoBuf1 = null;
    FlatGeoBuf.Header headerRecord1 = null;
    List<FlatGeoBuf.Feature> featureRecords = new ArrayList<>();

    // Copy the file
    try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ);
        WritableByteChannel tempChannel = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {

      // Copy the header
      headerFlatGeoBuf1 = FlatGeoBufReader.readHeaderBuffer(channel);
      headerRecord1 = FlatGeoBufReader.asFlatGeoBuf(headerFlatGeoBuf1);
      FlatGeoBufWriter.writeHeaderBuffer(tempChannel, headerFlatGeoBuf1);

      // Copy the index
      ByteBuffer indexBuffer = FlatGeoBufReader.readIndexBuffer(channel, headerFlatGeoBuf1);
      FlatGeoBufWriter.writeIndexBuffer(tempChannel, indexBuffer);

      // Copy the features
      var buffer = ByteBuffer.allocate(1 << 10).order(ByteOrder.LITTLE_ENDIAN);
      for (long i = 0; i < headerFlatGeoBuf1.featuresCount(); i++) {
        Feature feature = FlatGeoBufReader.readFeatureBuffer(channel, buffer);
        FlatGeoBufWriter.writeFeatureBuffer(tempChannel, feature);
        FlatGeoBuf.Feature featureRecord =
            FlatGeoBufReader.asFlatGeoBuf(headerFlatGeoBuf1, feature);
        featureRecords.add(featureRecord);
      }
    }

    // Read the copied file
    try (var channel = FileChannel.open(tempFile, StandardOpenOption.READ)) {

      // Read the header
      Header headerFlatGeoBuf2 = FlatGeoBufReader.readHeaderBuffer(channel);
      FlatGeoBuf.Header headerRecord2 = FlatGeoBufReader.asFlatGeoBuf(headerFlatGeoBuf2);
      assertNotNull(headerFlatGeoBuf2);
      assertEquals(headerRecord1, headerRecord2);

      // Read the index
      FlatGeoBufReader.skipIndex(channel, headerFlatGeoBuf2);

      // Read the features
      ByteBuffer buffer = ByteBuffer.allocate(1 << 10).order(ByteOrder.LITTLE_ENDIAN);
      for (long i = 0; i < headerFlatGeoBuf2.featuresCount(); i++) {
        Feature featureFlatGeoBuf = FlatGeoBufReader.readFeatureBuffer(channel, buffer);
        FlatGeoBuf.Feature featureRecord =
            FlatGeoBufReader.asFlatGeoBuf(headerFlatGeoBuf2, featureFlatGeoBuf);
        assertNotNull(featureRecord);
        assertEquals(featureRecords.get((int) i), featureRecord);
      }
    }
  }

  @Test
  void readWriteRecord() throws IOException {
    Path file = TestFiles.POINT_FLATGEOBUF;
    Path tempFile = Files.createTempFile("countries", ".fgb");

    Header headerFlatGeoBuf1 = null;
    FlatGeoBuf.Header headerRecord1 = null;
    List<FlatGeoBuf.Feature> featureRecords = new ArrayList<>();

    // Copy the file
    try (ReadableByteChannel channel = FileChannel.open(file, StandardOpenOption.READ);
        WritableByteChannel tempChannel = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {

      // Copy the header
      headerRecord1 = FlatGeoBufReader.readHeader(channel);
      headerFlatGeoBuf1 = FlatGeoBufWriter.asFlatBuffer(headerRecord1);
      FlatGeoBufWriter.writeHeader(tempChannel, headerRecord1);

      // Copy the index
      ByteBuffer indexBuffer = FlatGeoBufReader.readIndexBuffer(channel, headerFlatGeoBuf1);
      FlatGeoBufWriter.writeIndexBuffer(tempChannel, indexBuffer);

      // Copy the features
      var buffer = ByteBuffer.allocate(1 << 10).order(ByteOrder.LITTLE_ENDIAN);
      for (long i = 0; i < headerFlatGeoBuf1.featuresCount(); i++) {
        FlatGeoBuf.Feature feature =
            FlatGeoBufReader.readFeature(channel, headerFlatGeoBuf1, buffer);
        FlatGeoBufWriter.writeFeature(tempChannel, headerFlatGeoBuf1, feature);
        featureRecords.add(feature);
      }
    }

    // Read the copied file
    try (var channel = FileChannel.open(tempFile, StandardOpenOption.READ)) {

      // Read the header
      FlatGeoBuf.Header headerRecord2 = FlatGeoBufReader.readHeader(channel);
      Header headerFlatGeoBuf2 = FlatGeoBufWriter.asFlatBuffer(headerRecord2);
      assertNotNull(headerFlatGeoBuf2);
      assertEquals(headerRecord1, headerRecord2);

      // Read the index
      FlatGeoBufReader.skipIndex(channel, headerFlatGeoBuf2);

      // Read the features
      ByteBuffer buffer = ByteBuffer.allocate(1 << 10).order(ByteOrder.LITTLE_ENDIAN);
      for (long i = 0; i < headerFlatGeoBuf2.featuresCount(); i++) {
        FlatGeoBuf.Feature featureRecord =
            FlatGeoBufReader.readFeature(channel, headerFlatGeoBuf2, buffer);
        assertNotNull(featureRecord);
        assertEquals(featureRecords.get((int) i), featureRecord);
      }
    }
  }

}
