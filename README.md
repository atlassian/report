# Performance reporting
Produces various reports out of raw performance data.
Includes automated judgement calls.

## API
The API consists of all public Java types from `com.atlassian.performance.tools.report.api` and its subpackages:

  * [source compatibility]
  * [binary compatibility]
  * [behavioral compatibility] with behavioral contracts expressed via Javadoc

[source compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#source_compatibility
[binary compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#binary_compatibility
[behavioral compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#behavioral_compatibility

## Releasing
Versioning, releasing and distribution are managed by the [gradle-release] plugin.
See the [changelog](CHANGELOG.md).

[gradle-release]: https://bitbucket.org/atlassian/gradle-release/src/release-0.0.2/README.md

## License
Copyright (c) 2018 Atlassian and others.
Apache 2.0 licensed, see [LICENSE.txt](LICENSE.txt) file.