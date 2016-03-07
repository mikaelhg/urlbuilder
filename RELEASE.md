How to release:

Create a GPG key and push it out into the world.

```
j7 mvn -Prelease release:prepare
j7 mvn -Prelease release:perform
git push --tags
```

Upload files from `target/checkout/target` to https://oss.sonatype.org/ as a Staging Upload.

Release the passed staging repo from the confusing UI.
