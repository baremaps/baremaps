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

package com.baremaps.dem;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.locationtech.jts.geom.Geometry;

/**
 * A simple renderer that traces contour lines on a raster image. This class is used for testing
 * purposes only, hence it presence in the test folder.
 */
public class ContourRenderer {

  public static void main(String[] args) throws IOException {
    // Load the image

    var file = Path.of("dem.png").toFile();
    var image = ImageIO.read(file);

    // Convert the image to a grid
    double[] grid = ElevationUtils.imageToGrid(image, ElevationUtils::terrariumToElevation);

    // Trace the contours
    List<Geometry> contours = new ArrayList<>();
    for (int i = 0; i < 10000; i += 100) {
      contours.addAll(new ContourTracer(grid, image.getWidth(), image.getHeight(), false, false)
          .traceContours(i));
    }

    // Create a frame to display the contours
    JFrame frame = new JFrame("Contour Lines");
    frame.setSize(image.getWidth() + 20, image.getHeight() + 48);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.add(new ContourCanvas(image, contours));
    frame.setVisible(true);
  }

  // Custom Canvas to draw the contours
  static class ContourCanvas extends Canvas {

    Image image;

    List<Geometry> contours;

    public ContourCanvas(Image image, List<Geometry> contours) {
      this.image = image;
      this.contours = contours;
    }

    @Override
    public void paint(Graphics g) {

      // Draw the image
      g.drawImage(image, 10, 10, null);

      g.setColor(Color.RED);
      for (Geometry geometry : contours) {
        List<Point> points = Stream.of(geometry.getCoordinates())
            .map(p -> new Point((int) p.getX() + 10, (int) p.getY() + 10))
            .toList();
        for (int i = 0; i < points.size() - 1; i++) {
          Point p1 = points.get(i);
          Point p2 = points.get(i + 1);
          g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
      }
    }
  }
}
