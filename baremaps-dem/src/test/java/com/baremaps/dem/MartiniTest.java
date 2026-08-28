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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MartiniTest {

  @Test
  @Disabled("This test must be reworked as it relied on an unlicensed file")
  void generateAMesh() throws IOException {
    var file = Path.of("dem.png").toFile();
    var png = ImageIO.read(file);
    var terrainGrid = Martini.createGrid(png);
    var martini = new Martini(png.getWidth() + 1);
    var tile = martini.createTile(terrainGrid);
    var mesh = tile.getMesh(500);

    assertArrayEquals(new int[] {
        320, 64, 256, 128, 320, 128, 384, 128, 256, 0, 288, 160, 256, 192, 288, 192, 320, 192, 304,
        176, 256, 256, 288, 224, 352, 160, 320, 160, 512, 0, 384, 0, 128, 128, 128, 0, 64, 64, 64,
        0, 0, 0, 32, 32, 192, 192, 384, 384, 512, 256, 384, 256, 320, 320, 320, 256, 512, 512, 512,
        128, 448, 192, 384, 192, 128, 384, 256, 512, 256, 384, 0, 512, 128, 256, 64, 192, 0, 256,
        64, 128, 32, 96, 0, 128, 32, 64, 16, 48, 0, 64, 0, 32
    }, mesh.getVertices());

    assertArrayEquals(new int[] {
        0, 1, 2, 3, 0, 2, 4, 1, 0, 5, 6, 7, 7, 8, 9, 5, 7, 9, 1, 6, 5, 6, 10, 11, 11, 8, 7, 6, 11,
        7, 12, 2, 13, 8, 12, 13, 3, 2, 12, 2, 1, 5, 13, 5, 9, 8, 13, 9, 2, 5, 13, 3, 14, 15, 15, 4,
        0, 3, 15, 0, 16, 4, 17, 18, 17, 19, 19, 20, 21, 18, 19, 21, 16, 17, 18, 1, 16, 22, 22, 10,
        6, 1, 22, 6, 4, 16, 1, 23, 24, 25, 26, 25, 27, 10, 26, 27, 23, 25, 26, 28, 24, 23, 29, 3,
        30, 24, 29, 30, 14, 3, 29, 8, 25, 31, 31, 3, 12, 8, 31, 12, 27, 8, 11, 10, 27, 11, 25, 8,
        27, 25, 24, 30, 30, 3, 31, 25, 30, 31, 32, 33, 34, 10, 32, 34, 35, 33, 32, 33, 28, 23, 34,
        23, 26, 10, 34, 26, 33, 23, 34, 36, 16, 37, 38, 36, 37, 36, 10, 22, 16, 36, 22, 39, 18, 40,
        41, 39, 40, 16, 18, 39, 42, 21, 43, 44, 42, 43, 18, 21, 42, 21, 20, 45, 45, 44, 43, 21, 45,
        43, 44, 41, 40, 40, 18, 42, 44, 40, 42, 41, 38, 37, 37, 16, 39, 41, 37, 39, 38, 35, 32, 32,
        10, 36, 38, 32, 36
    }, mesh.getTriangles());
  }
}
