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

DIR="$(dirname "$0")"
cd "$DIR" || exit

echo ""
echo "--------------------------------------------------------------------"
echo "Build the project"
echo "--------------------------------------------------------------------"
echo ""

cd ..
./mvnw spotless:apply clean install -DskipTests
rm -fr baremaps
tar -xvf ./baremaps-cli/target/baremaps-0.7.3-SNAPSHOT-bin.tar.gz -C target/
mv ./target/baremaps-0.7.3-SNAPSHOT-bin ./baremaps
export PATH=$PATH:`pwd`/baremaps/bin

echo ""
echo "--------------------------------------------------------------------"
echo "Display the path of the baremaps executable"
echo "--------------------------------------------------------------------"
echo ""

which baremaps

echo ""
echo "--------------------------------------------------------------------"
echo "Display the version of the baremaps executable"
echo "--------------------------------------------------------------------"
echo ""

baremaps --version
