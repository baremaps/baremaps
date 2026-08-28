#!/bin/sh

# Licensed under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

osmium cat sample.osm.xml -o sample.osm.pbf -f sample.pbf --overwrite
cd 000/000/
rm 001.osc.gz && gzip -k 001.osc.xml && mv 001.osc.xml.gz 001.osc.gz
rm 002.osc.gz && gzip -k 002.osc.xml && mv 002.osc.xml.gz 002.osc.gz
rm 003.osc.gz && gzip -k 003.osc.xml && mv 003.osc.xml.gz 003.osc.gz
rm 004.osc.gz && gzip -k 004.osc.xml && mv 004.osc.xml.gz 004.osc.gz
cd -