# Pipeline board
Simple dashboard to aggregate results of multiple jobs from multiple heterogeneous CI servers.

Currently supports the following CI servers:
* Jenkins
* GitLab CI
* TeamCity

Features:
* Configurable from yaml
* Show the builds for a customizable period (change with mouse scroll)
* Move the shown period with dragging
* Click through to get to the job page and build page
* Auto-refesh

Setup:
* The config file is read from $WORKING_DIR/config or $HOME/.pipeline_board/config(fallback)
* For trying it out you can download example_config and rename it to config.
This will set it up to use public repositories as examples.

Screenshot on mock projects:
![Alt text](https://user-images.githubusercontent.com/29373148/34178037-b3852d50-e4fd-11e7-8b65-15cce0e97dd5.png)
