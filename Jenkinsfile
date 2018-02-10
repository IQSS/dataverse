#!groovy

node {
  workspace = pwd()
  properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [
    [ name: 'app',        $class: 'StringParameterDefinition', defaultValue: "dataverse" ],
    [ name: 'branch',     $class: 'StringParameterDefinition', defaultValue: "${env.JOB_BASE_NAME}" ],
    [ name: 'deployenv',  $class: 'StringParameterDefinition', defaultValue: 'dev-aws' ],
    [ name: 'deployuser', $class: 'StringParameterDefinition', defaultValue: 'jenkins' ]
  ]]])

  wrap([$class: 'BuildUser']) {
    def BUILD_USER_ID = env.BUILD_USER_ID
  }

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
    unstash 'dataverse-war'

    timeout(time: 2, unit: "HOURS") {
      def DEPLOY_TARGET = input message: 'Deploy to', parameters: [string(defaultValue: 'dev', description: 'dev, stage, prod', name: 'DEPLOY_TARGET')]
      try {
        notifyBuild("${BUILD_USER_ID} is deploying ${ARTIFACT_ID}-${VERSION}.war to ${DEPLOY_TARGET}", "good")
        sh """
          ssh qdradmin@qdr-${DEPLOY_TARGET}-ec2-01.int.qdr.org \"sudo mkdir -p /srv/dataverse-releases; sudo chown qdradmin /srv/dataverse-releases\"
          rsync -av target/${ARTIFACT_ID}-${VERSION}.war qdradmin@qdr-${DEPLOY_TARGET}-ec2-01.int.qdr.org:/srv/dataverse-releases
          ssh qdradmin@qdr-${DEPLOY_TARGET}-ec2-01.int.qdr.org \"sudo dv-deploy /srv/dataverse-releases/${ARTIFACT_ID}-${VERSION}.war\"
        """
        notifyBuild("Success", "good")
        sh "curl -X POST http://graphite.int.qdr.org:81/events/ -d '{\"what\": \"${ARTIFACT_ID}-${VERSION}.war to ${DEPLOY_TARGET} from ${app}/${branch}\", \"tags\" : \"deployment\"}"
      }
      catch (e) {
        currentBuild.result = "FAILURE"
        notifyBuild("Failed!", "danger")
        throw e
      }
    }
  }
}


@NonCPS
def notifyBuild(String message, String color) {
  slackSend message: "<$JOB_URL|$JOB_NAME>: ${message}", color: "${color}"
}
