#!groovy

node {
  workspace = pwd()
  properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [
    [ name: 'DEPLOY_TARGET',  $class: 'StringParameterDefinition', defaultValue: 'dev' ],
  ]]])

  stage('Init') {
    /*
    * Checkout code
    */
    checkout scm
    ARTIFACT_ID = readMavenPom().getArtifactId()
    VERSION = readMavenPom().getVersion()
    currentBuild.result = 'SUCCESS'
  }

  stage('Test') {
    /*
    * Run Unit tests
    */
    notifyBuild("Running Tests", "good")

    try {
      withMaven(
        jdk: 'jdk8',
        maven: 'mvn-3-5-0') {
          sh "mvn test"
        }
    }
    catch (e) {
      currentBuild.result = "UNSTABLE"
      notifyBuild("Warning: Tests Failed!", "warning")
    }
  }

  stage('Build') {
    /*
    * Run Unit tests
    */
    notifyBuild("Building", "good")

    try {
      withMaven(
        jdk: 'jdk8',
        maven: 'mvn-3-5-0') {
          sh "mvn clean package -DskipTests"
      }
    }
    catch (e) {
      currentBuild.result = "FAILURE"
      notifyBuild("Warning: Build failed!", "warning")
    }

    stash includes: 'target/dataverse*.war', name: 'dataverse-war'
  }

  stage('Deploy') {
    /*
    * Deploy
    */
    if ("${DEPLOY_TARGET}" != "dev") {
      timeout(time: 2, unit: "HOURS") {
        def DEPLOY_TARGET = input message: 'Deploy to', parameters: [string(defaultValue: "${DEPLOY_TARGET}", description: 'dev, stage, prod', name: 'DEPLOY_TARGET')]
      }
    }

    notifyBuild("Deploying ${ARTIFACT_ID}-${VERSION} to ${DEPLOY_TARGET}", "good")
    unstash 'dataverse-war'
    try {
      sh """
        ssh qdradmin@qdr-${DEPLOY_TARGET}-ec2-01.int.qdr.org \"sudo mkdir -p /srv/dataverse-releases; sudo chown qdradmin /srv/dataverse-releases\"
        rsync -av target/${ARTIFACT_ID}-${VERSION}.war qdradmin@qdr-${DEPLOY_TARGET}-ec2-01.int.qdr.org:/srv/dataverse-releases
        ssh qdradmin@qdr-${DEPLOY_TARGET}-ec2-01.int.qdr.org 'sudo su - glassfish -c \"dv-deploy /srv/dataverse-releases/${ARTIFACT_ID}-${VERSION}.war\"'
      """
      notifyBuild("Success", "good")
      sh "curl -sX POST http://graphite.int.qdr.org:81/events/ -d '{\"what\": \"${ARTIFACT_ID}-${VERSION} to ${DEPLOY_TARGET}\", \"tags\" : \"deployment\"}'"
    }
    catch (e) {
      currentBuild.result = "FAILURE"
      notifyBuild("Failed!", "danger")
      throw e
    }
  }
}

@NonCPS
def notifyBuild(String message, String color) {
  slackSend message: "<$JOB_URL|$JOB_NAME>: ${message}", color: "${color}"
}
