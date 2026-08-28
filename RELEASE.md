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

Everything is published on GitHub. Pushing a tag of the form `vX.Y.Z` triggers the
[release workflow](.github/workflows/release.yml), which:

- sets the Maven version from the tag,
- publishes the Maven artifacts (jars, sources, javadocs) to
  [GitHub Packages](https://github.com/orgs/baremaps/packages?repo_name=baremaps),
- builds the source and binary archives with their SHA-512 checksums,
- drafts a GitHub release with generated release notes and the archives attached.

Tags with a suffix (e.g. `v1.2.3-rc1`, `v1.2.3-beta1`) are marked as pre-releases.

The version in `pom.xml` stays a `-SNAPSHOT`; the workflow derives the release version from the tag.

To release:

```bash
export RELEASE_VERSION=<release_version> # e.g. 0.8.3
git checkout main && git pull
git tag -a v$RELEASE_VERSION -m "Baremaps $RELEASE_VERSION"
git push origin v$RELEASE_VERSION
```

Then review the generated release notes on GitHub and publish the draft release.

After a final release, bump the development version on `main`:

```bash
export NEXT_VERSION=<next_version> # e.g. 0.8.4
./mvnw versions:set -DnewVersion=$NEXT_VERSION-SNAPSHOT -DgenerateBackupPoms=false
git commit -a -m "Prepare for next development iteration"
git push origin main
```

## Using the Maven artifacts

GitHub Packages requires authentication, even for public packages. Add a `github` server with a
personal access token (`read:packages` scope) to your `~/.m2/settings.xml` and declare the repository:

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/baremaps/baremaps</url>
</repository>
```

## Verifying the archives

```bash
shasum -a 512 -c baremaps-$RELEASE_VERSION-bin.tar.gz.sha512
shasum -a 512 -c baremaps-$RELEASE_VERSION-src.tar.gz.sha512
```
