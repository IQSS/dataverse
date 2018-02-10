#!groovy

node {
  workspace = pwd()
  properties([[$class: 'ParametersDefinitionProperty', parameterDefinitions: [
    [ name: 'app',        $class: 'StringParameterDefinition', defaultValue: "dataverse" ],
    [ name: 'branch',     $class: 'StringParameterDefinition', defaultValue: "${env.JOB_BASE_NAME}" ],
    [ name: 'deployenv',  $class: 'StringParameterDefinition', defaultValue: 'dev-aws' ],
    [ name: 'deployuser', $class: 'StringParameterDefinition', defaultValue: 'jenkins' ]
  ]]])

  stage('Init') {
    /*
    * Checkout code
    */
    checkout scm
    ARTIFACT_ID = readMavenPom().getArtifactId()
    VERSION = readMavenPom().getVersion()
    currentBuild.result = 'SUCCESS'
    //sh(script:"curl -X POST http://graphite.int.qdr.org:81/events/ -d '{\"what\": \"deploy ${app}/${branch} to ${deployenv}\", \"tags\" : \"deployment\"}'")
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
      def userInput = input message: 'Deploy to', parameters: [string(defaultValue: 'dev', description: '', name: 'deploy-to')]
      try {
        sh """
          ssh qdradmin@qdr-${userInput}-ec2-01.int.qdr.org \"sudo mkdir -p /srv/dataverse-releases; sudo chown qdradmin /srv/dataverse-releases\"
          rsync -av target/${ARTIFACT_ID}-${VERSION}.war qdradmin@qdr-${userInput}-ec2-01.int.qdr.org:/srv/dataverse-releases
          ssh qdradmin@qdr-${userInput}-ec2-01.int.qdr.org \"sudo dv-deploy /srv/dataverse-releases/${ARTIFACT_ID}-${VERSION}.war\"
        """
      }
      catch (e) {
        currentBuild.result = "FAILURE"
        notifyBuild("Deploying ${app} to ${deploy-to} Failed! <$BUILD_URL/console|(See Logs)>", "danger")
        throw e
      }
    }
  }
}


@NonCPS
def notifyBuild(String message, String color) {
  slackSend message: "<$JOB_URL|$JOB_NAME>: ${message}", color: "${color}"
}
