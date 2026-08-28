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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeListener;

public class HillShadeRenderer extends JFrame {

  private BufferedImage originalImage;
  private double[] grid;
  private JSlider altitudeSlider;
  private JSlider azimuthSlider;
  private JSlider scaleSlider;
  private JCheckBox isSimpleCheckbox;
  private JLabel imageLabel;
  private JLabel altitudeLabel;
  private JLabel azimuthLabel;
  private JLabel scaleLabel;

  public HillShadeRenderer() throws IOException {
    super("Hillshade Display");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Load the image
    originalImage = ImageIO.read(Path.of("dem.png").toFile());
    grid = ElevationUtils.imageToGrid(originalImage, ElevationUtils::rgbToElevation);

    // Create UI components
    altitudeSlider = new JSlider(JSlider.VERTICAL, 0, 90, 45);
    azimuthSlider = new JSlider(JSlider.VERTICAL, 0, 360, 315);
    scaleSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 10); // Scale from 0.1 to 10.0
    isSimpleCheckbox = new JCheckBox("Simple Algorithm", true);
    imageLabel = new JLabel();
    altitudeLabel = new JLabel("Sun Altitude: 45°");
    azimuthLabel = new JLabel("Sun Azimuth: 315°");
    scaleLabel = new JLabel("Scale: 1.0");

    // Set up sliders
    altitudeSlider.setMajorTickSpacing(15);
    altitudeSlider.setPaintTicks(true);
    altitudeSlider.setPaintLabels(true);

    azimuthSlider.setMajorTickSpacing(45);
    azimuthSlider.setPaintTicks(true);
    azimuthSlider.setPaintLabels(true);

    scaleSlider.setMajorTickSpacing(10);
    scaleSlider.setPaintTicks(true);
    scaleSlider.setPaintLabels(true);

    // Add listeners
    ChangeListener listener = e -> {
      updateLabels();
      redrawHillshade();
    };
    altitudeSlider.addChangeListener(listener);
    azimuthSlider.addChangeListener(listener);
    scaleSlider.addChangeListener(listener);
    isSimpleCheckbox.addActionListener(e -> redrawHillshade());

    // Set up layout
    setLayout(new BorderLayout());
    JPanel controlPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.insets = new Insets(5, 5, 5, 5);
    controlPanel.add(altitudeLabel, gbc);
    gbc.gridy++;
    controlPanel.add(altitudeSlider, gbc);
    gbc.gridy++;
    controlPanel.add(azimuthLabel, gbc);
    gbc.gridy++;
    controlPanel.add(azimuthSlider, gbc);
    gbc.gridy++;
    controlPanel.add(scaleLabel, gbc);
    gbc.gridy++;
    controlPanel.add(scaleSlider, gbc);
    gbc.gridy++;
    controlPanel.add(isSimpleCheckbox, gbc);

    add(imageLabel, BorderLayout.CENTER);
    add(controlPanel, BorderLayout.EAST);

    // Initial draw
    redrawHillshade();

    pack();
    setVisible(true);
  }

  private void updateLabels() {
    altitudeLabel.setText("Sun Altitude: " + altitudeSlider.getValue() + "°");
    azimuthLabel.setText("Sun Azimuth: " + azimuthSlider.getValue() + "°");
    scaleLabel.setText("Scale: " + (scaleSlider.getValue() / 10.0));
  }

  private void redrawHillshade() {
    int sunAltitude = altitudeSlider.getValue();
    int sunAzimuth = azimuthSlider.getValue();
    double scale = scaleSlider.getValue() / 10.0;

    double[] hillshade = new HillshadeCalculator(grid, originalImage.getWidth(),
        originalImage.getHeight(), scale).calculate(sunAltitude, sunAzimuth);

    BufferedImage hillshadeImage = new BufferedImage(originalImage.getWidth(),
        originalImage.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    for (int y = 0; y < originalImage.getHeight(); y++) {
      for (int x = 0; x < originalImage.getWidth(); x++) {
        int shade = (int) hillshade[y * originalImage.getWidth() + x];
        int rgb = new Color(shade, shade, shade).getRGB();
        hillshadeImage.setRGB(x, y, rgb);
      }
    }

    imageLabel.setIcon(new ImageIcon(hillshadeImage));
    revalidate();
    repaint();
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try {
        new HillShadeRenderer();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }
}
