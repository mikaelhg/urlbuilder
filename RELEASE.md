How to release:

If you haven't done this before, create a GPG key and push it out into the world.

Before the release, remember to update the README.md example version.

```
j8 mvn -Prelease release:prepare
j8 mvn -Prelease release:perform -Darguments="-Dmaven.deploy.skip=true"
git push --tags
```

Upload files from `target/checkout/target` to https://oss.sonatype.org/ as a Staging Upload.

Release the passed staging repo from the confusing UI.
