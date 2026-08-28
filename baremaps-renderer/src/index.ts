#!/usr/bin/env node

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

import commander from 'commander';
import http from 'http';
import st from 'st';
import { exec } from 'child_process';
import { BrowserPool } from './lib/browserPool';
import { TestsLogger } from './lib/testsLogger';
import { MaplibreBrowserHelpers } from './lib/maplibreBrowserHelpers';
import { TestManager } from './lib/testManager';
import { ReportGenerator } from './lib/reportGenerator';

/** Subcommand for running the tests */
const run = async (options: any) => {
  // Parse options
  const instances = parseInt(options.instances);
  const threshold = parseFloat(options.threshold);

  // Discover tests
  const testLogger = new TestsLogger();
  const testManager = new TestManager(
    options.path,
    'integration',
    options.style,
    options.refStyle,
    testLogger,
    threshold,
  );

  // Setup browser pool
  const maplibreHelpers = new MaplibreBrowserHelpers(options.debug);
  const browserPool = new BrowserPool(
    instances,
    true,
    maplibreHelpers.initPage,
  );
  await browserPool.init();

  // Queue tests
  for (const test of testManager.getTests()) {
    browserPool.queueTask(test);
  }

  // Run tests
  const testResults: boolean[] = await browserPool.run();
  await browserPool.close();

  // Generate summary and report
  testLogger.logSummary();
  new ReportGenerator(options.path, testManager.getTests()).generate();

  if (testResults.some((res) => !res)) {
    process.exit(1);
  }
};

/** Subcommand for viewing the report */
const viewReport = async (options: any) => {
  const server = http.createServer(
    st({ path: options.path, index: ReportGenerator.reportFilename }),
  );

  const port = 3000;
  const url = `http://localhost:${port}`;
  server.listen(port, () => {
    console.log(`INFO: Running at ${url}`);
  });
  // open the browser
  if (options.open) {
    if (process.platform === 'linux') {
      exec(`xdg-open ${url}`);
    } else if (process.platform === 'darwin') {
      exec(`open ${url}`);
    } else if (process.platform === 'win32') {
      exec(`start ${url}`);
    }
  }
};

/** Main entrypoint */
(async () => {
  const program = new commander.Command();

  program.version('0.0.2').description('Baremaps renderer utility tool');

  program
    .command('run')
    .description('run integration tests defined in the tests folder')
    .requiredOption('-s, --style <style>', 'style url to use')
    .option(
      '-r, --refStyle <refStyle>',
      'reference style url to use',
      'https://demo.baremaps.com/style.json',
    )
    .option(
      '-t, --threshold <threshold>',
      'threshold to use for comparing images',
      '0.1',
    )
    .option('-p, --path <testsPath>', 'tests folder path to use', 'tests')
    .option('-d, --debug', 'debug output', false)
    .option(
      '-i, --instances <instances>',
      'number of concurrent browsers to use',
      '2',
    )
    .action(run);

  program
    .command('report')
    .description('view the report of the latest test run')
    .option('-o, --open', 'open the report in the browser', false)
    .option('-p, --path <testsPath>', 'tests folder path to use', 'tests')
    .action(viewReport);

  program.parse(process.argv);
})();
