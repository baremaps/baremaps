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

/** Represent the result of tests */
export interface TestResults {
  [key: string]: boolean | undefined;
}

/** Metadata file spec for tests */
export interface Metadata {
  width: number;
  height: number;
  center: [number, number];
  zoom: number;
}

/** Data spec for the HTML generator */
export interface TestData {
  path: string;
  name: string;
  metadata: Metadata;
  success: boolean;
  expectedImagePath: string;
  actualImagePath: string;
  diffImagePath: string;
  diff: number;
}
