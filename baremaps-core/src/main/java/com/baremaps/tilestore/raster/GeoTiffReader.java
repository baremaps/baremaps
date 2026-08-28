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

package com.baremaps.tilestore.raster;

import com.baremaps.tilestore.TileCoord;
import java.nio.file.Path;
import org.apache.sis.coverage.grid.*;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.image.ImageProcessor;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A reader for geotiff files based on the Apache SIS library.
 */
public class GeoTiffReader implements AutoCloseable {

  private static final CoordinateReferenceSystem WEB_MERCATOR;

  static {
    try {
      WEB_MERCATOR = CRS.fromWKT("""
          ProjectedCRS["WGS 84 / Pseudo-Mercator",
            BaseGeodCRS["WGS 84",
              Datum["World Geodetic System 1984",
                Ellipsoid["WGS 84", 6378137.0, 298.257223563]],
              Unit["degree", 0.017453292519943295]],
            Conversion["Popular Visualisation Pseudo-Mercator",
              Method["Popular Visualisation Pseudo Mercator"]],
            CS[Cartesian, 2],
              Axis["Easting (X)", east],
              Axis["Northing (Y)", north],
              Unit["metre", 1],
            Scope["Certain Web mapping and visualisation applications."],
            Id["EPSG", 3857]]
          """);
    } catch (FactoryException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private final DataStore dataStore;

  private final GridCoverage gridCoverage;

  /**
   * Constructs a {@code GeoTiffReader} with the specified path.
   *
   * @param path the path of the geotiff file
   * @throws GeoTiffException if an error occurs
   */
  public GeoTiffReader(Path path) throws GeoTiffException {
    try {
      this.dataStore = DataStores.open(path);
      var allImages = ((Aggregate) dataStore).components();
      var firstImage = (GridCoverageResource) allImages.iterator().next();
      firstImage.setLoadingStrategy(RasterLoadingStrategy.AT_GET_TILE_TIME);
      gridCoverage = firstImage.read(null);
    } catch (DataStoreException e) {
      throw new GeoTiffException(e);
    }
  }

  /**
   * Read the elevation data for the specified tile coordinate.
   *
   * @param tileCoord the tile coordinate
   * @param tileSizePx the size of the tile in pixels
   * @param tileBufferPx the buffer size in pixels
   * @return the elevation data
   * @throws GeoTiffException if an error occurs
   */
  public double[] read(TileCoord tileCoord, int tileSizePx, int tileBufferPx)
      throws GeoTiffException {
    try {
      // Compute the buffer size and expand the envelope
      var imageSize = tileSizePx + 2 * tileBufferPx;
      var tileEnvelope = tileCoord.envelope();
      var tileWidth = tileEnvelope.getWidth();
      var tileHeight = tileEnvelope.getHeight();
      var pixelWidth = tileWidth / tileSizePx;
      var pixelHeight = tileHeight / tileSizePx;
      tileEnvelope.expandBy(pixelWidth * tileBufferPx, pixelHeight * tileBufferPx);

      // Create the target grid geometry in the web mercator projection
      var sourceEnvelope = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
      sourceEnvelope.setRange(0, tileEnvelope.getMinX(), tileEnvelope.getMaxX());
      sourceEnvelope.setRange(1, tileEnvelope.getMinY(), tileEnvelope.getMaxY());
      var targetEnvelope = Envelopes.transform(sourceEnvelope, WEB_MERCATOR);
      var targetSize = new GridExtent(imageSize, imageSize);
      var targetGridGeometry =
          new GridGeometry(targetSize, targetEnvelope, GridOrientation.DISPLAY);

      // Resample and render the image
      var gridCoverageProcessor = new GridCoverageProcessor();
      gridCoverageProcessor.setInterpolation(new BicubicInterpolation());
      var tileGridCoverage = gridCoverageProcessor.resample(this.gridCoverage, targetGridGeometry);
      var renderedImage = tileGridCoverage.render(null);

      // Convert the image to the terrarium color scale
      var imageProcessor = new ImageProcessor();
      renderedImage = imageProcessor.prefetch(renderedImage, null);

      // Convert the image to a grid
      var values = new double[renderedImage.getWidth() * renderedImage.getHeight()];
      renderedImage.getData().getPixels(
          0,
          0,
          renderedImage.getWidth(),
          renderedImage.getHeight(),
          values);

      return values;
    } catch (TransformException e) {
      throw new GeoTiffException(e);
    }
  }

  /**
   * Closes the reader.
   *
   * @throws Exception if an error occurs
   */
  @Override
  public void close() throws Exception {
    this.dataStore.close();
  }
}
