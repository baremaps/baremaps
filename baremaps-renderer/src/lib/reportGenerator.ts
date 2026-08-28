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
import { Test } from './test';
import { TestData } from '../types';

/**
 * ReportGenerator class
 *
 * Generates an HTML report from generated images during tests
 */
export class ReportGenerator {
  public static readonly reportFilename = 'report.html';
  private static readonly assetsPath = 'assets';
  private static readonly reportTemplateFilename = 'report-template.html';

  private testFolder: string;
  private tests: Test[];
  private htmlTemplate: string;

  /**
   * Creates a new ReportGenerator
   *
   * @param testFolder Path to the test folder
   * @param tests Array of tests
   * @returns A new ReportGenerator
   */
  constructor(testFolder: string, tests: Test[]) {
    this.testFolder = testFolder;
    if (!path.isAbsolute(this.testFolder)) {
      this.testFolder = path.join(process.cwd(), this.testFolder);
    }
    this.tests = tests;
    // root dir of the package
    const rootDirname = path.dirname(__dirname);
    this.htmlTemplate = fs.readFileSync(
      path.join(
        rootDirname,
        ReportGenerator.assetsPath,
        ReportGenerator.reportTemplateFilename,
      ),
      'utf8',
    );
  }

  /** Generates and saves the HTML report */
  public generate() {
    const testsData = this.tests
      .map((test) => this.getTestData(test))
      .filter((test) => test !== null);

    const html = this.htmlTemplate.replace(
      '{{ TESTS_DATA }}',
      JSON.stringify(testsData, null, 2),
    );
    this.writeHtml(html);

    console.log(
      "INFO: Report generated at '" +
        path.join(this.testFolder, ReportGenerator.reportFilename) +
        "'",
    );
  }

  /**
   * Returns an object with the test images paths and metadata for a given test
   *
   * @param test The test to get the data from
   * @returns An object with the test images paths and metadata
   */
  private getTestData(test: Test): TestData | null {
    // return if the test is not completed
    if (test.success === undefined) {
      return null;
    }
    // check if the images exist
    if (
      !fs.existsSync(path.join(test.testPath, Test.expectedFilename)) ||
      !fs.existsSync(path.join(test.testPath, Test.actualFilename)) ||
      !fs.existsSync(path.join(test.testPath, Test.diffFilename))
    ) {
      console.log(
        'WARN: Missing images for test ' + test.testPath + ', skipping...',
      );
      return null;
    }
    const testName = path.basename(test.testPath);
    const testsRelativePath = path.relative(this.testFolder, test.testPath);
    return {
      path: test.testPath,
      name: testName,
      metadata: test.metadata,
      success: test.success,
      expectedImagePath: path.join(testsRelativePath, Test.expectedFilename),
      actualImagePath: path.join(testsRelativePath, Test.actualFilename),
      diffImagePath: path.join(testsRelativePath, Test.diffFilename),
      // The diff is not undefined because the test is completed
      diff: test.diff as number,
    };
  }

  /**
   * Writes the HTML report to disk
   *
   * @param html The HTML report
   */
  private writeHtml(html: string) {
    fs.writeFileSync(
      path.join(this.testFolder, ReportGenerator.reportFilename),
      html,
    );
  }
}
