[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

gatekeeper
==========

Implementation of an OAuth 2 provider for the hub platform.

How to run
==========

```sbtshell
sbt run
```

This will start the application on port *5678*

Running tests
=============

```````````
sbt compile scalastyle coverage test it:test coverageReport
```````````

You can set an alias for this in **.bashrc** (ubuntu) or **.bash_profile** (mac)

License
=======
This code is open sourced licensed under the Apache 2.0 License
