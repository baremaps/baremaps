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

/**
 * This is a simple server to serve the static files
 * generated with the baremaps export command.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const hostname = 'localhost';
const port = 9000;

http.createServer((req, res) => {
    let file = '.' + req.url;
    if (file === './') {
        file = './index.html';
    }

    const extension = String(path.extname(file)).toLowerCase();
    const mimeTypes = {
        '.html': 'text/html',
        '.js': 'text/javascript',
        '.css': 'text/css',
        '.mvt': 'application/vnd.mapbox-vector-tile',
    };

    const contentType = mimeTypes[extension] || 'application/octet-stream';

    fs.readFile(file, (error, content) => {
        if (error) {
            res.writeHead(404);
            res.end('Not found');
        } else {
            let headers = {'Content-Type': contentType};
            if (extension === '.mvt') {
                headers['Content-Encoding'] = 'gzip';
            }
            res.writeHead(200, headers);
            res.end(content, 'utf-8');
        }
    });
}).listen(port, hostname, () => {
    console.log(`Server running at http://${hostname}:${port}/`);
});
