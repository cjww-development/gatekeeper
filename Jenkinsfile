def gitVersion
pipeline {
  agent {
    docker {
      image 'hseeberger/scala-sbt:11.0.11_1.5.3_2.13.6'
      args '--network jenkins'
    }
  }
  environment {
    GITHUB_TOKEN = credentials('sbt-publisher-token')
    GH_TOKEN = credentials('github-api')
    SBT_OPS = '-DMONGO_URI=mongodb://jenkins-mongo:27017 -Dsbt.global.base=.sbt -Dsbt.boot.directory=.sbt -Dsbt.ivy.home=.ivy2 -Dlocal=false'
  }
  parameters {
    choice(
      name: 'VersionType',
      choices: "minor\nmajor\nhotfix",
      description: 'What type of version to build.'
    )
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
    stage('Version project') {
      when {
        branch 'master'
      }
      steps {
        script {
          build job: 'operations/create-a-release', parameters: [string(name: 'Project', value: 'gatekeeper'), string(name: 'Type', value: params.VersionType)]
          gitVersion = sh(
            script: '''
              JQ=./jq
              curl https://stedolan.github.io/jq/download/linux64/jq > $JQ && chmod +x $JQ
              curl -H "Accept: application/vnd.github.manifold-preview" -H 'Authorization: token '$GH_TOKEN'' -s 'https://api.github.com/repos/cjww-development/gatekeeper/releases/latest' | ./jq -r '.tag_name'
            ''',
            returnStdout: true
          ).trim()
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