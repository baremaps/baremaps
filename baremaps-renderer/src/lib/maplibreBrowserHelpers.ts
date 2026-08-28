/**
 Licensed under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 **/

import { Page } from 'puppeteer';
import { Metadata } from '../types';

/** Helper class for running maplibre-gl in a browser */
export class MaplibreBrowserHelpers {
  private debug: boolean;

  /**
   * Create a new MaplibreBrowserHelpers
   *
   * @param debug whether to enable debug logging
   */
  constructor(debug: boolean = false) {
    this.debug = debug;
  }

  /**
   * Initialize a page for running maplibre-gl
   *
   * @param page puppeteer page
   */
  public async initPage(page: Page) {
    if (this.debug) {
      page
        .on('console', (message: any) =>
          console.log(
            `${message.type().slice(0, 3).toUpperCase()} ${message.text()}`,
          ),
        )
        .on('pageerror', ({ message }: any) => console.log(message))
        .on('response', (response: any) =>
          console.log(`${response.status()} ${response.url()}`),
        )
        .on('requestfailed', (request: any) =>
          console.log(`${request.failure().errorText} ${request.url()}`),
        );
    }

    await page.addScriptTag({
      url: 'https://unpkg.com/maplibre-gl@5.0.1/dist/maplibre-gl.js',
    });
    await page.addStyleTag({
      url: 'https://unpkg.com/maplibre-gl@5.0.1/dist/maplibre-gl.css',
    });
  }

  /**
   * Get an image from a maplibre-gl style
   *
   * @param page puppeteer page
   * @param metadata metadata for the map
   * @param styleUrl url to the maplibre-gl style
   * @returns image as a base64 encoded string
   */
  public static async getImageFromMetadata(
    page: Page,
    metadata: Metadata,
    styleUrl: string,
  ) {
    const width = metadata.width;
    const height = metadata.height;

    await page.setViewport({ width, height, deviceScaleFactor: 1 });
    await page.setContent(`
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <title>Query Test Page</title>
            <meta charset='utf-8'>
            <link rel="icon" href="about:blank">
            <style>#map {
                box-sizing:content-box;
                width:${width}px;
                height:${height}px;
            }</style>
        </head>
        <body>
            <div id='map'></div>
        </body>
        </html>`);

    // create map
    await page.evaluate(
      (metadata: Metadata, styleUrl: string) => {
        (window as any).map = new (window as any).maplibregl.Map({
          container: 'map',
          style: styleUrl,
          center: metadata.center,
          zoom: metadata.zoom,
          interactive: false,
          attributionControl: false,
          // If true, the map's canvas can be exported to a PNG using map.getCanvas().toDataURL(). This is false by default as a performance optimization.
          preserveDrawingBuffer: true,
          // Prevents the fading-in of layers after the style has loaded.
          fadeDuration: 0,
        });
      },
      metadata,
      styleUrl,
    );

    // wait for map to load
    await page.waitForFunction(() => {
      return (
        (window as any).map?.loaded() && (window as any).map?.isStyleLoaded()
      );
    });

    // export image from map
    const image = await page.evaluate(() => {
      return (window as any).map.getCanvas().toDataURL();
    });

    return Buffer.from(image.split(',')[1], 'base64');
  }
}
