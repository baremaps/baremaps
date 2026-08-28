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

/** Abstract class for a task that can be run on a puppeteer page */
export abstract class RunnableTask {
  /**
   * Abstract function for running the task
   *
   * @param page puppeteer page
   * @returns true if the task was successful, false otherwise
   */
  abstract run(page: Page): Promise<boolean>;
}
