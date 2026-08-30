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
import java.util.List;
import javax.swing.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;

/**
 * A simple renderer that displays the cases of the Marching Square algorithm. This class is used
 * for testing purposes only, hence it presence in the test folder.
 */
public class MarchingSquareRenderer extends JPanel {
  private List<Geometry> geometries;

  public MarchingSquareRenderer(List<Geometry> geometries) {
    this.geometries = geometries;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    for (Geometry geometry : geometries) {
      if (geometry instanceof Polygon) {
        drawPolygon(g2d, (Polygon) geometry);
      }
      if (geometry instanceof LineString) {
        drawLineString(g2d, (LineString) geometry);
      }
    }
  }

  private void drawLineString(Graphics2D g2d, LineString geometry) {
    if (geometry.isClosed()) {
      g2d.setColor(Color.RED);
    } else {
      g2d.setColor(Color.BLUE);
    }

    Coordinate[] coordinates = geometry.getCoordinates();
    int[] xPoints = new int[coordinates.length];
    int[] yPoints = new int[coordinates.length];

    for (int i = 0; i < coordinates.length; i++) {
      xPoints[i] = (int) coordinates[i].getX();
      yPoints[i] = (int) coordinates[i].getY();
    }

    g2d.drawPolyline(xPoints, yPoints, coordinates.length);
  }

  private void drawPolygon(Graphics2D g2d, Polygon polygon) {
    Coordinate[] coordinates = polygon.getCoordinates();
    int[] xPoints = new int[coordinates.length];
    int[] yPoints = new int[coordinates.length];

    for (int i = 0; i < coordinates.length; i++) {
      xPoints[i] = (int) coordinates[i].getX();
      yPoints[i] = (int) coordinates[i].getY();
    }

    g2d.drawPolygon(xPoints, yPoints, coordinates.length);
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame("Geometry Drawer");

    JPanel mainPanel = new JPanel(new GridLayout(4, 4));
    for (double[] c : MarchingSquareUtils.CASES) {

      int size = (int) Math.sqrt(c.length);
      List<Geometry> geometries = new ContourTracer(c, size, size).tracePolygons(0.5)
          .stream()
          .map(Geometry.class::cast)
          .map(AffineTransformation.scaleInstance(50, 50).translate(0, 0)::transform)
          .toList();

      MarchingSquareRenderer drawer = new MarchingSquareRenderer(geometries);
      mainPanel.add(drawer);
    }

    frame.add(mainPanel);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null); // Center the frame on the screen
    frame.setVisible(true);
  }
}
