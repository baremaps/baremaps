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
const geonamesUrl = "https://download.geonames.org/export/dump/allCountries.zip";

// Fetch and unzip Geonames
const FetchAndUnzipGeonames = {id: "fetch-geonames-allcountries", needs: [], tasks: [
    {type: "DownloadUrl", source: geonamesUrl, target: "downloads/geonames-allcountries.zip", replaceExisting: true},
    {type: "DecompressFile", source: "downloads/geonames-allcountries.zip", target: "archives", compression: "zip"}
]};

// Create the Geocoder index
const createGeonamesIndex = {id: "geocoder-index", needs: [FetchAndUnzipGeonames.id], tasks: [
    {type: "CreateGeonamesIndex", dataFile: "archives/allCountries.txt", indexDirectory: "geocoder-index"}
]};

export default {"steps": [FetchAndUnzipGeonames, createGeonamesIndex]};
