#!groovy

node {
  workspace = pwd()
  properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [
    [ name: 'DEPLOY_TARGET',  $class: 'StringParameterDefinition', defaultValue: 'qdr-dev' ],
  ]]])

  stage('Init') {
    /*
    * Checkout code
    */
    checkout scm
    ARTIFACT_ID = readMavenPom().getArtifactId()
    VERSION = readMavenPom(file: 'modules/dataverse-parent/pom.xml').getVersion()
    currentBuild.result = 'SUCCESS'
  }

  stage('Test') {
    /*
    * Run Unit tests
    */
    notifyBuild("Running Tests", "good")

    try {
      withMaven(
        //jdk: 'jdk11',
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
        //jdk: 'jdk11',
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
    timeout(time: 2, unit: "HOURS") {
      def DEPLOY_TARGET = input message: 'Deploy to', parameters: [string(defaultValue: "${DEPLOY_TARGET}", description: 'qdr-dev, qdr-stage, qdr-prod, qdr-prod-2', name: 'DEPLOY_TARGET')]
    }

    notifyBuild("Deploying ${ARTIFACT_ID}-${VERSION} to ${DEPLOY_TARGET}", "good")
    unstash 'dataverse-war'
    try {
      sh """
        ssh qdradmin@${DEPLOY_TARGET}.int.qdr.org \"sudo mkdir -p /srv/dataverse-releases; sudo chown qdradmin /srv/dataverse-releases\"
        rsync -av target/${ARTIFACT_ID}-${VERSION}.war qdradmin@${DEPLOY_TARGET}.int.qdr.org:/srv/dataverse-releases
        ssh qdradmin@${DEPLOY_TARGET}.int.qdr.org ' sudo chmod 644 /srv/dataverse-releases/${ARTIFACT_ID}-${VERSION}.war; sudo su - glassfish -c \"dv-deploy /srv/dataverse-releases/${ARTIFACT_ID}-${VERSION}.war\"'
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
