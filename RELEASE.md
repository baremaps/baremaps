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

# Release instructions

# Release instructions

The following instructions assume that the release candidate version has been set in an environment variable:

```bash
export RELEASE_VERSION=<release_version> # e.g. 0.7.1
export NEXT_VERSION=<next_version> # e.g. 0.7.2
export CANDIDATE_NUMBER=<candidate_number> # e.g. 1
export RELEASE_MANAGER_NAME=<release_manager_name> # e.g. John Doe
export COMMIT_HASH=<commit_hash> # e.g. 1234567890
```

In order to release a new version of Baremaps, follow these steps:

- [ ] Notify the community and ask everyone to pause commits on the main branch
- [ ] Create a new issue on GitHub with the title "Release Baremaps $RELEASE_VERSION"
- [ ] Create a new branch for the release (e.g. `release-$RELEASE_VERSION`)

```bash
cd baremaps
git checkout -b release-$RELEASE_VERSION
```

- [ ] Set the release version and commit the changes:

```bash 
./mvnw versions:set -DnewVersion=$RELEASE_VERSION -DgenerateBackupPoms=false
git commit -a -m "Release Baremaps $RELEASE_VERSION"
git push --set-upstream origin release-$RELEASE_VERSION
```

- [ ] Tag the last commit with the release candidate version:

```bash
git tag v$RELEASE_VERSION-rc$CANDIDATE_NUMBER
```

- [ ] Push the tag to the remote repository (this will trigger a GitHub Action to build, sign and hash the release candidate, and draft a release on GitHub):

```bash
git push origin v$RELEASE_VERSION-rc$CANDIDATE_NUMBER
```

- [ ] Edit the release notes for this tag on GitHub.
- [ ] Ask the community to test and review the release candidate.
- [ ] If the release candidate is not approved, commit the necessary changes, clean the git history, create a new release candidate, and repeat the process.
- [ ] If the release candidate is approved, tag the release commit with the release version (this will trigger the same GitHub Action as before):

```bash
git tag -a v$RELEASE_VERSION
git push origin v$RELEASE_VERSION
```

- [ ] Rebase the release branch into the main branch.
- [ ] Clean up all the release candidate branches and tags.
- [ ] Publish the release on GitHub.
- [ ] Publish the release artifacts to the Maven repository.

```bash
./mvnw clean deploy -Prelease
```

- [ ] Set the version of the next iteration and commit the changes:

```bash
./mvnw versions:set -DnewVersion=$NEXT_VERSION-SNAPSHOT -DgenerateBackupPoms=false
git commit -a -m "Prepare for next development iteration"
git push origin
```

- [ ] Notify the community of the release.

## Reproducing the build

The release artifacts are bit-by-bit reproducible if the following conditions are met:
- The build is run with the same version of the JDK (e.g. OpenJDK 17 temurin)
- The build is run with the maven wrapper (e.g. `./mvnw`)

The procedure has been tested on different operating systems (e.g. Linux and MacOS).
For convenience, we suggest to build the release artifacts on a clean environment (e.g. a fresh Docker container).

```bash
git checkout v$RELEASE_VERSION-rc$CANDIDATE_NUMBER
docker run \
  -v $(pwd):/baremaps \
  -w /baremaps \
  eclipse-temurin:17-jdk \
  ./mvnw clean verify -DskipTests
```

## Verifying the release artifacts

Verify the GPG signature of the release artifacts:

```bash
gpg --verify baremaps-$RELEASE_VERSION-bin.tar.gz.asc
gpg --verify baremaps-$RELEASE_VERSION-src.tar.gz.asc
```

Verify the SHA512 checksum of the release artifacts:

```bash
shasum -a 512 -c baremaps-$RELEASE_VERSION-bin.tar.gz.sha512
shasum -a 512 -c baremaps-$RELEASE_VERSION-src.tar.gz.sha512
```

## Announce template

```bash
cat << EOT
subject: [ANNOUNCE] Baremaps $RELEASE_VERSION released

Hello Everyone,

The Baremaps community is pleased to announce the release of Baremaps $RELEASE_VERSION.
Baremaps is a toolkit and a set of infrastructure components for creating, publishing, and operating online maps.
<short description of the release which should include release highlights>

The release notes and artifacts are available here:
https://github.com/baremaps/baremaps/releases/tag/v$RELEASE_VERSION

We are looking to grow our community and welcome new contributors.
If you are interested in contributing to the project, please reach out on GitHub.
We will be happy to help you get started.

The repository is available here:
https://github.com/baremaps/baremaps

The documentation is available here:
https://baremaps.com

The issue tracker is available here:
https://github.com/baremaps/baremaps/issues

Best regards,

$RELEASE_MANAGER_NAME
EOT
```
