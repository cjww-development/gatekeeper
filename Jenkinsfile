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
    TAG_VERSION = "NEEDS TO BE FOUND FROM PIPELINE EVENT"
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
    stage('Teardown MongoDB') {
      steps {
        script {
          sh 'docker compose -f docker-compose-mongo.yml down -v'
        }
      }
    }
//     stage('Build tarball') {
//       steps {
//         script {
//           sh 'sbt -D $SBT_OPS -Dversion=$TAG_VERSION universal:packageZipTarball'
//         }
//       }
//     }
    stage('Build tarball') {
      when {
        buildingTag()
      }
      steps {
        script {
          sh 'echo "Building tag"'
        }
      }
    }
//     stage('Build docker image') {
//       steps {
//         script {
//           sh 'docker build . -t cjww-development/gatekeeper:$TAG_VERSION --build-arg VERSION=$TAG_VERSION'
//         }
//       }
//     }
//     stage('Publish to ECR') {
//       steps {
//         script {
//           sh 'echo "Publishing to ECR"'
//         }
//       }
//     }
  }
  post {
    always {
      cleanWs()
    }
  }
}