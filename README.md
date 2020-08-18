[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

gatekeeper
==========

Gatekeeper is an OAuth2 and OIDC implementation.

#### Feature list
- Client registration
- User registration
- Developer registration
- Grant types
	- authorization_code
	- client_credentials


Prerequisites
==========
- Scala 2.13.3
- SBT 1.3.13
- MongoDB 4.2

We recommend managing Scala and SBT via SDKMan. SDKMan installation notes can be found [here](https://sdkman.io/install).

With SDKMan install Scala and SBT can be installed with

```
sdk install scala 2.13.3
sdk install sbt 1.3.13
```

For MongoDB, we recommend running MongoDB in docker. To boot a MongoDB image in docker, run

```
docker run -d -p 27017:27017 -v ~/data:/data/db mongo
```

How to run
==========
```
sbt run
```

This will start the application on port *5678*

Running tests
=============
```
sbt compile test it:test
```
To the run the integration tests under `it:test` you will need MongoDB running. Refer to the prerequesites section to understand how to boot MongoDB in docker.


License
=======
This code is open sourced licensed under the Apache 2.0 License
