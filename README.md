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
    - Create clients from presets
- User and developer registration
    - Update email and password
    - Email verification via
	    - AWS SES
	    - Mailgun
	    - More offerings to come
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

| Env Var          | Default                          | Description                                                                                                                                         |
|------------------|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| VERSION          | dev                              | The version of Gatekeeper you're running. Appears at the bottom of pages                                                                            |
| EMAIL_FROM       | test@email.com                   | The email address used to send emails from Gatekeeper                                                                                               |
| MONGO_URI        | mongodb://mongo.local            | Where MongoDB lives. The database that backs Gatekeeper                                                                                             |
| APP_SECRET       | 23817cc7d0e6460e9c1515aa4047b29b | The app secret scala play uses to sign session cookies and CSRF tokens. Should be changed to run in prod                                            |
| ENC_KEY          | 23817cc7d0e6460e9c1515aa4047b29b | The key used to secure data. Should be changed to run in prod                                                                                       |
| MFA_ISSUER       | Gatekeeper (docker)              | The string used to describe the TOTP Code in apps like Google Authenticator                                                                         |
| SMS_SENDER_ID    | SmsVerify                        | The string used to say where SMS messages have come from                                                                                            |
| EMAIL_PROVIDER   | n/a                              | Used to determine what email provider to use. Valid options are ses or mail-gun                                                                     |
| AWS_REGION       | n/a                              | Should only be set if EMAIL_PROVIDER is ses. Should match the AWS region you're running SES from                                                    |
| AWS_IDENTITY_ARN | n/a                              | Should only be set if EMAIL_PROVIDER is ses. Should the arn of the SES identity you're sending via if the SES identity lives in another AWS account |
| MAILGUN_API_KEY  | n/a                              | Should only be set if EMAIL_PROVIDER is mail-gun. Obtained from the mailgun console after account creation                                          |
| MAILGUN_URL      | n/a                              | Should only be set if EMAIL_PROVIDER is mail-gun. Obtained from the mailgun console after account creation                                          |
      
## Choosing an email provider
Gatekeeper currently sends emails via AWS SES or Mailgun. Both support sending emails from a proper address, or some address on a verified domain. On their respective free tiers you can only send to email addresses you've verified in SES or Mailgun.
To lift that limitation you need to be on a paid plan. However, AWS SES lets you send 62000 emails a month for free forever, but you need to be in their production zone on SES.
Mailgun, on their flex plan, allows 5000 emails a month for 3 months, and then you move to pay as you go ($0.80 / 1000 emails).

## SES cross account authorisation
AWS SES allows AWS accounts to send email via verified identities from other accounts. More information can be found [here](https://docs.aws.amazon.com/ses/latest/DeveloperGuide/sending-authorization.html).
Gatekeeper supports this if SES is your email provider. Ensure that the `AWS_IDENTITY_ARN` env var is set to the arn you're wanting to send via.

### What if I don't want to use AWS SES or Mailgun?
That's a fair question. Neither may suit you. If you're technically minded, look at [the adding more email providers section](#Adding-further-email-providers) to find out more about adding your preferred provided (dev work required).
If you're not technically minded, no fear, get started with a [feature request](https://github.com/cjww-development/gatekeeper/issues/new?assignees=&labels=&template=feature_request.md&title=) and we can try to make your request a reality.

Find more information about each here
- [Mailgun](https://www.mailgun.com/)
- [AWS SES](https://aws.amazon.com/ses/)

## Adding further client presets
Gatekeeper supports creating client presets for 
* Jenkins
* Grafana
* Portainer
* OpenId connect playground

OpenId connect playground has its domain configured as it's a managed service. Jenkins, grafana and portainer do not as they can be hosted anywhere.

```hocon
well-known-services = [
  {
    name = "jenkins"
    desc = "Build great things at any scale"
    icon = "jenkins.svg"
    redirect = "/securityRealm/finishLogin"
  },
  {
    name = "openid connect playground"
    desc = "For testing OIDC configurations"
    icon = "openid.png"
    domain = "https://openidconnect.net"
    redirect = "/callback"
  }
]
```

In `application.conf` the well known services block defines the presets. You can extend the preset services by creating PR's that add to this list. The domain should be specified if the service is hosted in one place.

For the time being, the service's icon needs to be hosted in Gatekeeper, find a suitable image for the service and place the image in `public/images/services`. In the config block enter the name of the file under icon.

Once a service has been added to this list, it will be available for creation in the frontend. 

## Adding further email providers
Right now Gatekeeper can send emails via AWS SES or Mailgun, however these providers can be extended. Below details how you can go about adding new email providers.

1. Clone the repository and branch off as `feature/email-provider/new-provider-name`


2. Add the new email providers config into `conf/application.conf` 
```hocon
email-service {
    selected-provider = ${?EMAIL_PROVIDER}
    message-settings {
      from = ${?EMAIL_FROM}
      verification-subject = "Verifying your account"
    }

    ses {
      region = ${?AWS_REGION}
      cross-account-identity-arn = ${?AWS_IDENTITY_ARN}
    }

    mail-gun {
      api-key = ${?MAILGUN_API_KEY}
      url = ${?MAILGUN_URL}
    }
    
    new-provider-name {
        ...whatever config the new provider needs. API Key, endpoints etc
    }
}
```

3. Create a new default class and trait inside of `app/services/comms/email`. Traits in this package should all extend the `EmailService`. Ensure appropriate tests are included.

```scala
import database.VerificationStore
import models.{EmailResponse, Verification}
import play.api.mvc.Request
import services.comms.email.EmailService

import scala.concurrent.{ExecutionContext, Future}

class DefaultNewProvider @Inject()(val config: Configuration,
                                   val verificationStore: VerificationStore) extends NewProvider {
  override val emailSenderAddress: String = config.get[String]("email-service.message-settings.from")
  override val verificationSubjectLine: String = config.get[String]("email-service.message-settings.verification-subject")
  override val valueFromConfigNeededForNewProvider: String = config.get[String]("email-service.new-provider-name.whatever-required-config")
}

trait NewProvider extends EmailService {

  val valueFromConfigNeededForNewProvider: String

  override def sendEmailVerificationMessage(to: String, record: Verification)(implicit req: Request[_], ec: ExecutionContext): Future[EmailResponse] = {
    //What ever logic is needed to send an email via the new provider
    // Should return a Future EmailResponse
    Future.successful(EmailResponse(
      provider = "new-provider-name",
      userId = record.userId,
      messageId = "some email message id returned from the new email provider"
    ))
  }
}
```

4. Add new class to Gatekeepers Service bindings in `app/global/ServiceBindings`
```scala
private def emailService(config: Configuration): Seq[Binding[_]] = {
  config.get[String]("email-service.selected-provider") match {
    case "ses"               => Seq(bind[EmailService].to[DefaultSesService].eagerly())
    case "mailgun"           => Seq(bind[EmailService].to[DefaultMailgunService].eagerly())
    case "new-provider-name" => Seq(bind[EmailService].to[DefaultNewProvider].eagerly())
    case _                   => throw new RuntimeException("Invalid email provider")
  }
}
```

5. Create a pull request

License
=======
This code is open sourced licensed under the Apache 2.0 License
