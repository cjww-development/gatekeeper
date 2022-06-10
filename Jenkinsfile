pipeline {
  agent {
    docker {
      image 'cjww-development/scala-toolchain:latest'
    }
  }
  environment {
    GITHUB_TOKEN = credentials('jenkins-github-packages')
    DOCKER_HOST = 'tcp://127.0.0.1:2375'
    SBT_OPS = '-DMONGO_URI=mongodb://mongo.local:27017 -Dsbt.global.base=.sbt -Dsbt.boot.directory=.sbt -Dsbt.ivy.home=.ivy2 -Dlocal=false'
  }
  options {
    ansiColor('xterm')
  }
  stages {
    stage('Boot MongoDB') {
      steps {
        script {
          sh 'docker compose up -f docker-compose-mongo.yml up -d'
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
          sh 'docker compose up -f docker-compose-mongo.yml down -v'
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}