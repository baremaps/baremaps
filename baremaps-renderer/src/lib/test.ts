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

import fs from 'fs';
import path from 'path';
import pixelmatch from 'pixelmatch';
import { PNG } from 'pngjs';
import { Page } from 'puppeteer';
import { MaplibreBrowserHelpers } from './maplibreBrowserHelpers';
import { TestsLogger } from './testsLogger';
import { Metadata } from '../types';
import { RunnableTask } from './runnableTask';

/** Test is responsible for running a single test */
export class Test implements RunnableTask {
  public static readonly metadataFilename = 'metadata.json';
  public static readonly expectedFilename = 'expected.png';
  public static readonly actualFilename = 'actual.png';
  public static readonly diffFilename = 'diff.png';

  private _testPath: string;
  private styleUrl: string;
  private refStyleUrl: string;
  private testLogger: TestsLogger;
  private threshold: number;

  private _metadata: Metadata;

  private _success?: boolean;
  private _diff?: number;

  /**
   * Create a new Test
   *
   * @param testPath path to the test
   * @param styleUrl URL to the style
   * @param refStyleUrl URL to the reference style
   * @param testLogger logger for tests
   * @param threshold threshold for image comparison
   * @returns a new Test
   * @throws if the test is not setup correctly
   */
  constructor(
    testPath: string,
    styleUrl: string,
    refStyleUrl: string,
    testLogger: TestsLogger,
    threshold: number,
  ) {
    this._testPath = testPath;
    this.styleUrl = styleUrl;
    this.refStyleUrl = refStyleUrl;
    this.testLogger = testLogger;
    this.threshold = threshold;
    try {
      this._metadata = JSON.parse(
        fs
          .readFileSync(path.join(this.testPath, Test.metadataFilename))
          .toString(),
      );
    } catch (e) {
      throw new Error(
        `Could not read metadata in test '${this.testPath}'. ` +
          `Make sure the file '${Test.metadataFilename}' exists and is valid JSON`,
      );
    }
    // check if metadata is valid
    if (
      !this.metadata ||
      !this.metadata.width ||
      !this.metadata.height ||
      !this.metadata.center ||
      !this.metadata.zoom
    ) {
      throw new Error(
        `Invalid metadata in test '${this.testPath}'. ` +
          'Metadata must contain width, height, center and zoom',
      );
    }
  }

  public get testPath(): string {
    return this._testPath;
  }

  public get metadata(): Metadata {
    return this._metadata;
  }

  public get success(): boolean | undefined {
    return this._success;
  }

  public get diff(): number | undefined {
    return this._diff;
  }

  /**
   * Abstraction function for running the test
   *
   * @param page puppeteer page
   * @returns true if the test was successful, false otherwise
   * @throws if the test fails to run
   */
  public async run(page: Page): Promise<boolean> {
    const success = await this.runTest(page)
      .then((s: boolean) => {
        this.testLogger.logTest(this.testPath, s);
        return s;
      })
      .catch((e) => {
        this.testLogger.logError(this.testPath, e);
        return false;
      });
    this._success = success;
    return success;
  }

  /**
   * Run the test
   *
   * @param page puppeteer page
   * @returns true if the test was successful, false otherwise
   */
  private async runTest(page: Page): Promise<boolean> {
    const image = await MaplibreBrowserHelpers.getImageFromMetadata(
      page,
      this.metadata,
      this.styleUrl,
    );

    const refImage = await MaplibreBrowserHelpers.getImageFromMetadata(
      page,
      this.metadata,
      this.refStyleUrl,
    );

    // compare image to reference image
    const width = this.metadata.width;
    const height = this.metadata.height;

    const diffImg = new PNG({ width, height });

    const diff = pixelmatch(
      PNG.sync.read(refImage).data,
      PNG.sync.read(image).data,
      diffImg.data,
      width,
      height,
      { threshold: this.threshold / (width * height) },
    );
    this._diff = diff;

    // save actual image
    fs.writeFileSync(path.join(this.testPath, Test.actualFilename), image);
    // save expected image
    fs.writeFileSync(path.join(this.testPath, Test.expectedFilename), refImage);
    // save diff image
    fs.writeFileSync(
      path.join(this.testPath, Test.diffFilename),
      PNG.sync.write(diffImg, { filterType: 4 }),
    );

    if (diff > 0) {
      return false;
    }
    return true;
  }
}
