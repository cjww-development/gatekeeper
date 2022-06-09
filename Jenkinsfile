def gitVersion
pipeline {
  agent {
    docker {
      image 'cjww-development/scala-toolchain:latest'
      args '--network jenkins'
    }
  }
  environment {
    GITHUB_TOKEN = credentials('sbt-publisher-token')
    GH_TOKEN = credentials('github-api')
    SBT_OPS = '-DMONGO_URI=mongodb://jenkins-mongo:27017 -Dsbt.global.base=.sbt -Dsbt.boot.directory=.sbt -Dsbt.ivy.home=.ivy2 -Dlocal=false'
  }
  options {
    ansiColor('xterm')
  }
  stages {
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
  }
  post {
    always {
      cleanWs()
    }
  }
}