<!--
Licensed under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<h1>Baremaps Renderer</h1>

- [Installation](#installation)
- [Usage](#usage)
  - [Running the Tests](#running-the-tests)
- [Creating a New Test](#creating-a-new-test)

## Installation

First, install the required dependencies:

```bash
npm install
```

Then, build the project:

```bash
npm run build
```

You can then link the script to your path in order to use it as a command line tool:

```bash
npm link
```

## Usage

In order to get a list of the available commands, run:

```bash
baremaps-renderer --help
```

### Running the Tests

Within a directory containing a `tests` folder, you can run the tests by running:

```bash
baremaps-renderer run -s <styleUrl>
```

After the tests are run, a report is generated `tests/report.html`. You can view the report by opening the file in your browser or by running:

```bash
baremaps-renderer report --open
```

## Creating a New Test

Within a directory containing a `tests` folder, you can add a new test by creating a folder in the `tests/integration` directory. Each test is a folder containing a metadata file `metadata.json`. The metadata file is as follows:

```json
{
  "width": 512,
  "height": 512,
  "center": [6.6323, 46.5197],
  "zoom": 14
}
```

- `width:` the width of the image in pixels
- `height:` the height of the image in pixels
- `center:` the center of the map longitude, latitude
- `zoom:` the zoom level of the map
