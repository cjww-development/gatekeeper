[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

gatekeeper
==========

Gatekeeper is an OIDC & OAuth2 server written in scala. This OAuth2 server is designed for the self-hosting community who are looking for an auth solution to get single sign-on on their self-hosted services.

Gatekeeper has been explicitly tested with the following services

* Jenkins (via the [OpenId Connect Authentication Plugin](https://plugins.jenkins.io/oic-auth/))
* Portainer
* Gitea

However, if a service offers integration with a third party OAuth2 provider, there is a good chance Gatekeeper will work with it. If it doesn't, please raise an issue.


## Features
- Client registration
    - Update Redirects
    - Limit scopes
    - Limit available OAuth2 flows
    - Modify token validity
    - Regenerate client Id and secret
- User and developer registration
    - Update email and password
    - Email verification (via AWS SES)
    - TOTP MFA
    - See list of apps accessing your account
    - Revoke sessions for apps accessing your account
    - Completely revoke access for apps accessing your account
- Grant types
	- authorization_code
	- client_credentials
	- refresh_token



## Prerequisites
- Java 11
- Scala 2.13.3
- SBT 1.3.13
- MongoDB 4.2
- AWS credentials that have access to
	- SES (Simple email service)
	- SNS (Simple notification service)

We recommend managing Java, Scala and SBT via SDKMan. SDKMan installation notes can be found [here](https://sdkman.io/install).

Java, Scala and SBT can be installed with

```
sdk install java 11.0.11.hs-adpt (or whichever java 11 release is available at the time)
sdk install scala 2.13.6
sdk install sbt 1.5.3
```

For MongoDB, we recommend running MongoDB in docker. To boot a MongoDB image in docker, run

```
docker run -d -p 27017:27017 -v ~/data:/data/db mongo
```

To allow gatekeeper to send emails and sms messages you need an [AWS account](https://aws.amazon.com/) that has an IAM user or role available. AWS recommends creating IAM users and roles with the minimum set of privileges it needs to operate. 

Create a policy with the following permissions and attach the policy to the IAM user or role.

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "GatekeeperMinPermissions",
            "Effect": "Allow",
            "Action": [
                "ses:SendEmail",
                "sns:Publish"
            ],
            "Resource": "*"
        }
    ]
}
```

On a new AWS Account, SES provides sandbox access. This means that you can only send emails to the Amazon SES mailbox simulator and verified email addresses or domains. You can request that AWS moves your account out of the SES sandbox, more details [here](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/request-production-access.html?icmpid=docs_ses_console). 


## How to run
```
sbt -Demail.from=test@email.com -Dplay.http.router=testing.Routes run
```

test@email.com should be replaced with a email address or domain that's been verified in AWS SES

This will start the application on port *5678*

## Running tests 
```
sbt compile test it:test
```
To the run the integration tests under `it:test` you will need MongoDB running. Refer to the prerequesites section to understand how to boot MongoDB in docker.

## Booting in docker
The `docker-boot.sh` file packages gatekeeper into a tgz to be used by the Dockerfile. Docker image is then built from the file and subsequently booted along with MongoDB from the docker-compose file.
  
Run `./docker-boot.sh` to run this process.

## Docker compose variables
The following table describes what each of the gatekeeper envs means in the docker compose file.

| Env Var       | Default                          | Description                                                                                              |
|---------------|----------------------------------|----------------------------------------------------------------------------------------------------------|
| VERSION       | dev                              | The version of Gatekeeper you're running. Appears at the bottom of pages                                 |
| EMAIL_FROM    | test@email.com                   | The email address used to send emails from Gatekeeper                                                    |
| MONGO_URI     | mongodb://mongo.local            | Where MongoDB lives. The database that backs Gatekeeper                                                  |
| APP_SECRET    | 23817cc7d0e6460e9c1515aa4047b29b | The app secret scala play uses to sign session cookies and CSRF tokens. Should be changed to run in prod |
| ENC_KEY       | 23817cc7d0e6460e9c1515aa4047b29b | The key used to secure data. Should be changed to run in prod                                            |
| MFA_ISSUER    | Gatekeeper (docker)              | The string used to describe the TOTP Code in apps like Google Authenticator                              |
| SMS_SENDER_ID | SmsVerify                        | The string used to say where SMS messages have come from                                                 |

License
=======
This code is open sourced licensed under the Apache 2.0 License
