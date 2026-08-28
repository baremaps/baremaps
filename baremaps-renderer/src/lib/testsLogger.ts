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

import path from 'path';
import chalk from 'chalk';
import { TestResults } from '../types';

/** Class to log tests */
export class TestsLogger {
  private tests: TestResults;
  private startTime: number;

  /**
   * Create a new TestsLogger
   *
   * @returns a new TestsLogger
   */
  constructor() {
    this.tests = {} as TestResults;
    this.startTime = Date.now();
  }

  /**
   * Log a test to the console
   *
   * @param testPath path to the test
   * @param success whether the test was successful
   */
  public logTest(testPath: string, success: boolean) {
    const testName = path.basename(testPath);
    this.tests[testName] = success;

    const testRepr = this.getTestRepr(testPath);
    console.log(
      success
        ? chalk.bgGreenBright(chalk.whiteBright(chalk.bold(' PASS '))) +
            testRepr
        : chalk.bgRedBright(chalk.whiteBright(chalk.bold(' FAIL '))) + testRepr,
    );
  }

  /**
   * Log an error to the console
   *
   * @param testPath path to the test
   * @param error error to log
   */
  public logError(testPath: string, error: Error | unknown) {
    const testName = path.basename(testPath);
    this.tests[testName] = false;

    const testRepr = this.getTestRepr(testPath);
    console.log(
      chalk.bgRedBright(chalk.whiteBright(chalk.bold(' ERROR '))) + testRepr,
    );
    console.log(error);
  }

  /** Log a summary of the tests to the console */
  public logSummary() {
    // add empty line
    console.log();
    console.log(
      chalk.bgWhiteBright(chalk.blackBright(chalk.bold(' SUMMARY '))),
    );

    // log time taken
    const timeTaken = Date.now() - this.startTime;
    console.log(
      'Time taken: ' +
        chalk.bold(`${Math.round(timeTaken / 1000 / 60)}m`) +
        chalk.bold(`${(timeTaken / 1000).toFixed(2)}s`),
    );

    // print summary total failed and total passed
    const totPassed = Object.values(this.tests).filter(
      (success) => success,
    ).length;
    const totFailed = Object.values(this.tests).length - totPassed;

    console.log(
      'Out of ' +
        chalk.bold(Object.values(this.tests).length) +
        ' tests: ' +
        chalk.greenBright(chalk.bold(`${totPassed} passed`)) +
        ', ' +
        chalk.redBright(chalk.bold(`${totFailed} failed `)),
    );

    // add empty line
    console.log();
  }

  /**
   * Helper function to get a string representation for a test
   *
   * @param testPath path to the test
   * @returns string representation of the test
   */
  private getTestRepr(testPath: string) {
    return ` Ran test: ${path.basename(testPath)} (${testPath})`;
  }
}
