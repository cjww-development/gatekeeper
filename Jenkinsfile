pipeline {
  agent {
    docker {
      image 'cjww-development/scala-toolchain:latest'
      args '--network="host"'
    }
  }
  environment {
    GITHUB_TOKEN = credentials('jenkins-github-packages')
    DOCKER_HOST = 'tcp://127.0.0.1:2375'
    SBT_OPS = '-DMONGO_URI=mongodb://127.0.0.1:27017 -Dsbt.global.base=.sbt -Dsbt.boot.directory=.sbt -Dsbt.ivy.home=.ivy2 -Dlocal=false'
    HOME = "${WORKSPACE}"
  }
  options {
    ansiColor('xterm')
  }
  stages {
    stage('Boot MongoDB') {
      steps {
        script {
          sh 'docker compose -f docker-compose-mongo.yml up -d'
        }
      }
    }
    stage('Run tests') {
      steps {
        script {
          sh 'sbt -D $SBT_OPS clean compile coverage test it:test coverageReport'
        }
      }
    }
    stage("Publish coverage report"){
      steps{
        step([$class: 'ScoveragePublisher', reportDir: './target/scala-2.13/scoverage-report', reportFile: 'scoverage.xml'])
      }
    }
    stage('Build tarball') {
      when {
        buildingTag()
      }
      steps {
        script {
          sh "sbt -D $SBT_OPS -Dversion=${env.TAG_NAME} universal:packageZipTarball"
        }
      }
    }
    stage('Build docker image') {
      when {
        buildingTag()
      }
      steps {
        script {
          sh "docker build . -t cjww-development/gatekeeper:${env.TAG_NAME} --build-arg GK_VERSION=${env.TAG_NAME}"
        }
      }
    }
    stage('Publish to ECR') {
      when {
        buildingTag()
      }
      environment {
        AWS_ACCESS_KEY_ID = credentials('home-server-aws-access-key')
        AWS_SECRET_ACCESS_KEY = credentials('home-server-aws-secret-key')
        AWS_DEFAULT_REGION = 'eu-west-2'
        HS_ACCOUNT_ID = credentials('home-server-aws-account-id')
      }
      steps {
        script {
          sh '''
            aws ecr get-login-password | docker login -u AWS --password-stdin "https://${HS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com";
            docker tag cjww-development/gatekeeper:${TAG_NAME} ${HS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/gatekeeper:${TAG_NAME};
            docker push ${HS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/gatekeeper:${TAG_NAME};
          '''
        }
      }
    }
    stage('Create Elastic Beanstalk application version') {
      when {
        buildingTag()
      }
      environment {
        AWS_ACCESS_KEY_ID = credentials('home-server-aws-access-key')
        AWS_SECRET_ACCESS_KEY = credentials('home-server-aws-secret-key')
        AWS_DEFAULT_REGION = 'eu-west-2'
        HS_ACCOUNT_ID = credentials('home-server-aws-account-id')
      }
      steps {
        script {
          sh '''
            ./build/aws/build-eb-zip.sh ${TAG_NAME} ${HS_ACCOUNT_ID};
            aws s3 cp gatekeeper-${TAG_NAME}.zip s3://elasticbeanstalk-${AWS_DEFAULT_REGION}-${HS_ACCOUNT_ID}/gatekeeper-${TAG_NAME}.zip;
            aws elasticbeanstalk create-application-version \
                --application-name gatekeeper \
                --version-label ${TAG_NAME} \
                --source-bundle S3Bucket="elasticbeanstalk-${AWS_DEFAULT_REGION}-${HS_ACCOUNT_ID}",S3Key="gatekeeper-${TAG_NAME}.zip" \
                --region=${AWS_DEFAULT_REGION};
          '''
        }
      }
    }
    stage('Deploy to environment') {
      when {
        buildingTag()
      }
      environment {
        AWS_ACCESS_KEY_ID = credentials('home-server-aws-access-key')
        AWS_SECRET_ACCESS_KEY = credentials('home-server-aws-secret-key')
        AWS_DEFAULT_REGION = 'eu-west-2'
        HS_ACCOUNT_ID = credentials('home-server-aws-account-id')
      }
      steps {
        script {
          sh '''
            aws elasticbeanstalk update-environment \
                --application-name gatekeeper \
                --environment-name ws-prod \
                --version-label ${TAG_NAME} \
                --region=${AWS_DEFAULT_REGION};
          '''
        }
      }
    }
  }
  post {
    always {
      script {
        sh 'docker compose -f docker-compose-mongo.yml down -v'
        sh "docker image rm cjww-development/gatekeeper:${env.TAG_NAME} || true"
      }
      cleanWs()
    }
  }
}