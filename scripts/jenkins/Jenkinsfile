pipeline {
  agent any

  stages {

    stage('Build') {
      steps {
	// clean plate
        rm -rf target/
	// package warfile
	mvn -DcompilerArgument=-Xlint:unchecked test -P all-unit-tests package
	// generate HTML SureFire report
	mvn surefire-report:report
      }
    }

    stage('Test') {
      steps {
	curl -O https://raw.githubusercontent.com/IQSS/dataverse-ansible/master/ec2/ec2-create-instance.sh
	./ec2-create-instance.sh -b develop -r https://github.com/IQSS/dataverse.git -t jenkins_delete_me -l target -g groupvars.yml
      }
    }

    post {
      always {
	junit 'target/surefire-reports/*.xml'
      }
    }
  }
}
