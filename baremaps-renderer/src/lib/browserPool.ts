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

import puppeteer, { Page, Browser } from 'puppeteer';
import { RunnableTask } from './runnableTask';

/** Pool of browsers that can be used to run tasks in parallel */
export class BrowserPool {
  private instances: number;
  private headless: boolean;
  private initPage: (page: Page) => Promise<any>;
  private pool: Browser[];
  private queue: RunnableTask[] = [];

  /**
   * Create a new BrowserPool
   *
   * @param instances number of browsers to create
   * @param headless whether to run the browsers in headless mode
   * @param initPage function to run on each page before running a task
   * @returns a new BrowserPool
   */
  constructor(
    instances: number,
    headless: boolean = true,
    initPage?: (page: Page) => Promise<any>,
  ) {
    this.instances = instances;
    this.headless = headless;
    this.pool = [];
    this.initPage = initPage || (async (page: Page) => {});
  }

  /** Initialize the browser pool */
  public async init() {
    for (let i = 0; i < this.instances; i++) {
      const browser = await puppeteer.launch({
        headless: this.headless ? 'new' : false,
        args: ['--enable-webgl', '--no-sandbox', '--disable-web-security'],
      });
      const [page] = await browser.pages();
      await this.initPage(page);
      this.pool.push(browser);
    }
  }

  /**
   * Queue a task to be run
   *
   * @param task task to run
   */
  public async queueTask(task: RunnableTask) {
    this.queue.push(task);
  }

  /** Run all queued tasks in parallel */
  public async run() {
    const results = [];
    // run the queue in parallel with only one browser per task at max
    while (this.queue.length) {
      const res = await Promise.all(
        this.queue.splice(0, this.instances).map(async (task, index) => {
          const browser = this.pool[index];
          const [page] = await browser.pages();
          return await task.run(page);
        }),
      );
      results.push(...res);
    }
    return results;
  }

  /** Close all browsers in the pool */
  public async close() {
    await Promise.all(this.pool.map((browser) => browser.close()));
  }
}
