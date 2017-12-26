[![Build Status](https://travis-ci.org/kszilagyi/pipeline-board.svg?branch=master)](https://travis-ci.org/kszilagyi/pipeline-board)

# Pipeline board
Simple dashboard to aggregate results of multiple jobs from multiple heterogeneous CI servers.

Currently supports the following CI servers:
* Jenkins
* GitLab CI
* TeamCity

## Features
* Configurable from yaml
* Show the builds for a customizable period (change with mouse scroll)
* Move the shown period with dragging
* Click through to get to the job page and build page
* Auto-refesh

## Screenshot on mock projects
![Alt text](https://user-images.githubusercontent.com/29373148/34178037-b3852d50-e4fd-11e7-8b65-15cce0e97dd5.png)

## Setup
* The config file is read from `$WORKING_DIR/config` or `$HOME/.pipeline_board/config`(fallback)
* For trying it out you can download example_config and rename it to config. This will set it up to use public repositories as examples.


### Binary
* Download [latest release](https://github.com/kszilagyi/pipeline-board/releases/latest) (download the zip but not the source)
* Extract
* cd into the directory then run: `bin/pipeline-board [-Dhttp.port=<port>]`. The default port is 9000 if `-Dhttp.port` is not specified. The config has to be present either in that directory or in `$HOME/.pipeline_board/config(fallback)`

### From source
* To run, clone it and run `sbt "server/run [<port>]"` where the `<port>` is optionally the port. If not specified it defaults to 9000.

## Example config with all possible options
```yaml
title: Example projects
groups:
  - groupName: First group
    jenkins:
      jobs:
        - name: ELK
          url: https://ci.eclipse.org/elk/job/ElkMetaNightly/
          accessToken: 'sometoken' # Optional, have to be used together with user
          user: someuser # Optional, have to be used together with accessToken
    teamCity:
      jobs:
        - name: Build Ant using Maven
          url: https://teamcity.jetbrains.com/viewType.html?buildTypeId=ApacheAnt_BuildAntUsingMave
          # accessToken for teamCity is not supported
  - groupName: Second group     
    gitLabCi:
      jobs:
        - name: Some project on GitLab CI
          url: https://gitlab.com/someproject
          jobNameOnGitLab: name-of-job
          accessToken: 'sometoken' # optional

```


