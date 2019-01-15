# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## Compatibility
The library offers compatibility contracts on the Java API and the POM.

## Java API
The API consists of all public Java types from `com.atlassian.performance.tools.report.api` and its subpackages:

  * [source compatibility]
  * [binary compatibility]
  * [behavioral compatibility] with behavioral contracts expressed via Javadoc

[source compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#source_compatibility
[binary compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#binary_compatibility
[behavioral compatibility]: http://cr.openjdk.java.net/~darcy/OpenJdkDevGuide/OpenJdkDevelopersGuide.v0.777.html#behavioral_compatibility

### POM
Changing the license is breaking a contract.
Adding a requirement of a major version of a dependency is breaking a contract.
Dropping a requirement of a major version of a dependency is a new contract.

## [Unreleased]
[Unreleased]: https://bitbucket.org/atlassian/report/branches/compare/release%2F2.x.y%0Drelease-2.7.0

## [2.7.0] - 2019-01-16
[2.7.0]: https://bitbucket.org/atlassian/report/branches/compare/release-2.7.0%0Drelease-2.6.0

### Deprecated
- Deprecate Kotlin data-class generated methods from API for `PerformanceCriteria`, `Verdict`, `DurationData`, `InteractionStats`.

## [2.6.0] - 2019-01-11
[2.6.0]: https://bitbucket.org/atlassian/report/branches/compare/release-2.6.0%0Drelease-2.5.0

### Fixed
- Remove `WaterfallHighlightReport` in order to restore compatibility with `jira-actions:[2.0.0,3.1.0)`.  
  This is not a breaking change, because the API from 2.5.0 was not stable.  

## [2.5.0] - 2019-01-10
[2.5.0]: https://bitbucket.org/atlassian/report/branches/compare/release-2.5.0%0Drelease-2.4.0

### INCOMPATIBILITY BUG
2.5.0 is botched.
Don't use new APIs from this version. It's not compatible with `jira-actions:[2.0.0,3.1.0)`. 
Roll back to 2.4.0 or roll forward to 2.6.0 or later to restore this compatibility.

### Added
- Add `WaterfallHighlightReport`.
- Enable `WaterfallHighlightReport` in `FullReport`. Resolve [JPERF-316].

[JPERF-316]: https://ecosystem.atlassian.net/browse/JPERF-316

## [2.4.0] - 2019-01-08
[2.4.0]: https://bitbucket.org/atlassian/report/branches/compare/release-2.4.0%0Drelease-2.3.1

### Added
- Provide builder for EdibleResult.
- Provide API to perform calculations on action metrics. Resolves [JPERF-338].

[JPERF-338]: https://ecosystem.atlassian.net/browse/JPERF-338

## [2.3.1] - 2019-01-08
[2.3.1]: https://bitbucket.org/atlassian/report/branches/compare/release-2.3.1%0Drelease-2.3.0

### Fixed
- Create a JUnit report from exceptions containing '%' character. Fix [JPERF-339].

[JPERF-339]: https://ecosystem.atlassian.net/browse/JPERF-339

## [2.3.0] - 2018-11-29
[2.3.0]: https://bitbucket.org/atlassian/report/branches/compare/release-2.3.0%0Drelease-2.2.3

### Added
- Add support for `infrastructure:3`.
- Add support for `infrastructure:4`.
- Add support for `jira-actions:3`.
- Add support for `virtual-users:3`.

## [2.2.3] - 2018-10-31
[2.2.3]: https://bitbucket.org/atlassian/report/branches/compare/release-2.2.3%0Drelease-2.2.2

### Fixed
- Fix NPE in `RelativeTypicalPerformanceJudge` when action results are missing. Fix [JPERF-248].

[JPERF-248]: https://ecosystem.atlassian.net/browse/JPERF-248

## [2.2.2] - 2018-10-23
[2.2.2]: https://bitbucket.org/atlassian/report/branches/compare/release-2.2.2%0Drelease-2.2.1

### Fixed
- Restore original column ordering in `IndependentCohortsJudge`. Fix [JPERF-192].

[JPERF-192]: https://ecosystem.atlassian.net/browse/JPERF-192

## [2.2.1] - 2018-09-21
[2.2.1]: https://bitbucket.org/atlassian/report/branches/compare/release-2.2.1%0Drelease-2.2.0

### Fixed
- Skip secondary verdicts if failure verdict is negative. Fix [JPERF-106].

[JPERF-106]: https://ecosystem.atlassian.net/browse/JPERF-106

## [2.2.0] - 2018-09-13
[2.2.0]: https://bitbucket.org/atlassian/report/branches/compare/release-2.2.0%0Drelease-2.1.0

### Added 
- Log test errors in `EdibleResult`.

### Fixed
- Fix date conversion in `MeanLatencyChart`. Fix [JPERF-77](https://ecosystem.atlassian.net/browse/JPERF-77).

## [2.1.0] - 2018-09-11
[2.1.0]: https://bitbucket.org/atlassian/report/branches/compare/release-2.1.0%0Drelease-2.0.0

### Added
- Add mean latency chart. Resolve [JPERF-65](https://ecosystem.atlassian.net/browse/JPERF-65).

### Fixed
- Abbreviate action names in plain text report.
- Fix SampleSizeJudge when sample size is missing. Fix [JPERF-59](https://ecosystem.atlassian.net/browse/JPERF-59).

### Deprecated
- Deprecate `HistoricalCohortsReporter#report(report: Path)`.

## [2.0.0] - 2018-09-06
[2.0.0]: https://bitbucket.org/atlassian/report/branches/compare/release-2.0.0%0Drelease-1.0.0

### Changed 
- Use stable APT APIs.
- Require APT `infrastructure:2`.
- Require APT `virtual-users:1`.
- API of `Timelines`.

### Added
- Add APT `virtual-users:2` compatibility.

### Fixed
- Sort plain text report's actions alphabetically.
- Linear interpolation on charts.

## [1.0.0] - 2018-08-24
[1.0.0]: https://bitbucket.org/atlassian/report/branches/compare/release-1.0.0%0Drelease-0.0.3

### Changed
- Define the public API.

### Added
- Include the plain text report in the full report.

### Fixed
- Extract reports from judges.
- Depend on a stable APT `infrastructure` version.

## [0.0.3] - 2018-08-22
[0.0.3]: https://bitbucket.org/atlassian/report/branches/compare/release-0.0.3%0Drelease-0.0.2

### Added
- Add plain text report.

## [0.0.2] - 2018-08-21
[0.0.2]: https://bitbucket.org/atlassian/report/branches/compare/release-0.0.2%0Drelease-0.0.1

### Added
- Add missing [CHANGELOG.md](CHANGELOG.md).

### Fixed
- Depend on a stable version of APT `workspace`.

## [0.0.1] - 2018-08-06
[0.0.1]: https://bitbucket.org/atlassian/report/branches/compare/release-0.0.1%0Dinitial-commit

### Added
- Migrate performance reporting from [JPT submodule].
- Add [README.md](README.md).
- Configure Bitbucket Pipelines.

[JPT submodule]: https://stash.atlassian.com/projects/JIRASERVER/repos/jira-performance-tests/browse/report?at=b63a98c0283b875b212962237b3e3a04e24006cf
