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
          sh "docker build . -t cjww-development/gatekeeper:${env.TAG_NAME} --build-arg VERSION=${env.TAG_NAME}"
        }
      }
    }
    stage('Publish to ECR') {
      when {
        buildingTag()
      }
      environment {
        AWS_ACCESS_KEY_ID = credentials('aws-access-key')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-key')
        AWS_DEFAULT_REGION = 'eu-west-2'
        ROLE_ARN = credentials('home-server-role-arn')
      }
      steps {
        script {
          sh '''
            ./build/aws/assume-role.sh;
            aws ecr get-login-password | docker login -u AWS --password-stdin "https://$(aws sts get-caller-identity --query 'Account' --output text).dkr.ecr.$(aws configure get region).amazonaws.com"
            docker tag cjww-development/gatekeeper:${env.TAG_NAME} $(aws sts get-caller-identity --query 'Account' --output text).dkr.ecr.$(aws configure get region).amazonaws.com/gatekeeper:${env.TAG_NAME}
            docker push $(aws sts get-caller-identity --query 'Account' --output text).dkr.ecr.$(aws configure get region).amazonaws.com/gatekeeper:${env.TAG_NAME}
          '''
        }
      }
    }
  }
  post {
    always {
      cleanWs()
      script {
        sh 'docker compose -f docker-compose-mongo.yml down -v'
        sh "docker image rm cjww-development/gatekeeper:${env.TAG_NAME}"
      }
    }
  }
}