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

# Prompt for GPG_PASSPHRASE
echo "Enter GPG_KEY:"
read -s GPG_KEY

# Prompt for GPG_PASSPHRASE
echo "Enter GPG_PASSPHRASE:"
read -s GPG_PASSPHRASE

# Extract the current version with Maven
version=$(./mvnw -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)

# Clean and install the project with java 17 in a docker container
docker run \
  -v $(pwd):/baremaps \
  -w /baremaps \
  eclipse-temurin:17-jdk \
  ./mvnw clean install -DskipTests

# Go to the target directory
cd ./baremaps-cli/target/

# Hash and sign the artifacts
for artifact in ./baremaps-$version-*; do

  # Hash the artifact
  shasum -a 512 "$artifact" > "$artifact.sha512"

  # Sign the artifact
  gpg --no-tty --quiet --pinentry-mode loopback --default-key "$GPG_KEY" --batch --yes --passphrase "$GPG_PASSPHRASE" --output "$artifact.asc" --detach-sign --armor "$artifact"
done
