#!/usr/bin/env groovy
import hudson.model.*

pipeline {
  agent any
  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '3'))
  }
  environment {
    version = "${getVersion()}"
    BINTRAY_USER = getParam('BINTRAY_USER')
    BINTRAY_API_KEY = getParam('BINTRAY_API_KEY')
    artifactHost = getParam('bigbang_host')
    artifactPath = getParam('bigbang_path')
  }
  stages {

    stage('Checkout') {
      steps {
        this.notifyBuild('STARTED', version)
        git credentialsId: 'github', poll: false, url: 'git@github.com:cityzendata/warp10-quantum.git'
        echo "Building ${version}"
      }
    }

    stage('Build') {
      steps {
        echo '${STAGE_NAME}'
        sh "rm -fr ${env.WORKSPACE}/build"
        sh "rm -fr ${env.WORKSPACE}/bower_components"
        sh 'bower install'
        sh 'bower prune'
        sh 'sed -i -e "s/super();/super();\\n        this.configuredBackends=[\\{\'id\':\'dist\',\'label\':\'Distributed Warp\',\'url\':\'https:\\/\\/warp.cityzendata.net\\/api\\/v0\',\'execEndpoint\':\'\\/exec\',\'findEndpoint\':\'\\/find\',\'fetchEndpoint\':\'\\/fetch\',\'updateEndpoint\':\'\\/update\',\'deleteEndpoint\':\'\\/delete\',\'headerName\':\'X-Warp10\'\\}];/" ./src/quantum-app.html'
        sh "sed -i -e \"s/<title>Warp10 Quantum/<title>Warp10 Quantum - ${version}/\" ./index.html"
        sh 'sed -i -e "s/<quantum-app debug>/<quantum-app>/" ./index.html'
        sh '/usr/local/bin/polymer build'
      }
    }

    stage('Package') {
      steps {
        sh "mkdir -p ${env.WORKSPACE}/build/artifacts"
        sh "tar -czf ${env.WORKSPACE}/build/artifacts/quantum-${version}.tar.gz -C  ${env.WORKSPACE}/build/bundled ."
        sh "(cd ${env.WORKSPACE}/server/ && ./gradlew shadowJar)"
        archiveArtifacts "build/artifacts/*.tar.gz"
        archiveArtifacts "server/build/libs/*.jar"
      }
    }

    stage('To Bigbang') {
      when {
        expression { return isItATagCommit() }
      }
      steps {
        sshagent(credentials: ['github']) {
          sh "ssh -o StrictHostKeyChecking=no root@${artifactHost} mkdir -p ${artifactPath}/quantum/${version}"
          sh "scp -p ${env.WORKSPACE}/build/artifacts/quantum-${version}.tar.gz ansible-staging@${artifactHost}:${artifactPath}/quantum/${version}/quantum.tar.gz"
        }
      }
    }

    stage('Deploy') {
      when {
        expression { return isItATagCommit() }
      }
      parallel {
        stage('Deploy to Bintray') {
          options {
            timeout(time: 2, unit: 'HOURS')
          }
          input {
            message 'Should we deploy to Bintray?'
          }
          steps {
            sh 'cd server && ./gradlew bintray -x test'
            this.notifyBuild('PUBLISHED', version)
          }
        }
      }
    }
  }
  post {
    success {
      this.notifyBuild('SUCCESSFUL', version)
    }
    failure {
      this.notifyBuild('FAILURE', version)
    }
    aborted {
      this.notifyBuild('ABORTED', version)
    }
    unstable {
      this.notifyBuild('UNSTABLE', version)
    }
  }
}

void notifyBuild(String buildStatus, String version) {
  // build status of null means successful
  buildStatus = buildStatus ?: 'SUCCESSFUL'
  String subject = "${buildStatus}: Job ${env.JOB_NAME} [${env.BUILD_DISPLAY_NAME}] | ${version}" as String
  String summary = "${subject} (${env.BUILD_URL})" as String
  // Override default values based on build status
  if (buildStatus == 'STARTED') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    color = 'GREEN'
    colorCode = '#00FF00'
  } else if (buildStatus == 'PUBLISHED') {
    color = 'BLUE'
    colorCode = '#0000FF'
  } else {
    color = 'RED'
    colorCode = '#FF0000'
  }

  // Send notifications
  this.notifySlack(colorCode, summary, buildStatus)
}

void notifySlack(String color, String message, String buildStatus) {
  String slackURL = getParam('slackUrl')
  String payload = "{\"username\": \"${env.JOB_NAME}\",\"attachments\":[{\"title\": \"${env.JOB_NAME} ${buildStatus}\",\"color\": \"${color}\",\"text\": \"${message}\"}]}" as String
  sh "curl -X POST -H 'Content-type: application/json' --data '${payload}' ${slackURL}" as String
}

String getParam(String key) {
  return params.get(key)
}

String getVersion() {
  return sh(returnStdout: true, script: 'git describe --abbrev=0 --tags').trim()
}

boolean isItATagCommit() {
  String lastCommit = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
  String tag = sh(returnStdout: true, script: "git show-ref --tags -d | grep ^${lastCommit} | sed -e 's,.* refs/tags/,,' -e 's/\\^{}//'").trim()
  return tag != ''
}
