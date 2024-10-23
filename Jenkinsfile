GIT_USER_NAME = "jenkinsci"
GIT_USER_EMAIL = "jenkinsci@icm.edu.pl"
SOLR_CONTAINER_ALIAS="dataverse-solr-ittest"
POSTGRES_CONTAINER_ALIAS="dataverse-postgres-ittest"
MAIN_BRANCH="develop"
RELEASE_BRANCH_PREFIX="release/"

pipeline {
    agent {
        dockerfile {
            dir 'conf/docker/jenkins-build-dockercli-image'
            additionalBuildArgs '-t drodb-dockercli'
        }
    }

    parameters {
        string(name: 'branch', defaultValue: params.branch ?: MAIN_BRANCH, description: 'Branch to build', trim: true)
        booleanParam(name: 'doRelease', defaultValue: params.doRelease ?: false, description: 'Set to true to perform a release of the current SNAPSHOT version')
        booleanParam(name: 'nextMajor', defaultValue: false, description: "Set to true if the next dev version should be a major increment (Only effective with doRelease==true and on ${MAIN_BRANCH} branch).")
    }

    options {
        buildDiscarder logRotator(artifactDaysToKeepStr: '3', artifactNumToKeepStr: '3', daysToKeepStr: '3', numToKeepStr: '3')
        disableConcurrentBuilds()
        timeout(activity: true, time: 10)
    }

    environment {
        ARTIFACTORY_DEPLOY=credentials('ICM_ARTIFACTORY_JENKINSCI')
        DOCKER_HOST_EXT = sh(script: 'docker context ls --format "{{- if .Current -}} {{- .DockerEndpoint -}} {{- end -}}"', returnStdout: true)
                            .trim().replaceAll('tcp', 'https')
        DOCKER_CERT_EXT = '/home/jenkins/.docker'
        MAVEN_OPTS = "-Dmaven.repo.local=/home/jenkins/.m2/repository"
        GIT_SSH_COMMAND = "ssh -o StrictHostKeyChecking=no"
    }

    stages {

        stage('Prepare') {
            agent {
                dockerfile {
                    dir 'conf/docker/jenkins-build-image'
                    additionalBuildArgs '-t drodb-build'
                    reuseNode true
                }
            }

            steps {
               echo 'Preparing build.'
            }
        }

        stage('Build') {
            agent {
                docker {
                    image 'drodb-build:latest'
                    reuseNode true
                }
            }

            steps {
               echo 'Building dataverse.'
               sh './mvnw package -DskipTests'
            }

            post {
                always {
                    recordIssues(tools: [mavenConsole(), java()])
            	}
            }
        }

        stage('Unit tests') {
            agent {
                docker {
                    image 'drodb-build:latest'
                    reuseNode true
                }
            }

            steps {
               echo 'Executing unit tests.'
               sh './mvnw test'
            }

            post {
                always {
                    junit skipPublishingChecks: true, testResults: '**/target/surefire-reports/*.xml'
            		jacoco()
            	}
            }
        }

        stage('Integration tests') {
            steps {
                sh 'docker ps'
                script {
                    withinContainer {
                        IT_TEST_OPTS="-P integration-tests-only,ci-jenkins -Dtest.network.name=${env.DOCKER_NETWORK_NAME} -Ddocker.host=${env.DOCKER_HOST_EXT} -Ddocker.certPath=${env.DOCKER_CERT_EXT}"

                        echo 'Starting containers.'
                        sh "./mvnw docker:start -pl dataverse-webapp ${IT_TEST_OPTS}"

                        echo 'Executing integration tests.'
                        sh "./mvnw verify -Ddocker.skip ${IT_TEST_OPTS}"
                    }
                }
            }

            post {
                always {
                    junit skipPublishingChecks: true, testResults: '**/target/failsafe-reports/*.xml'
            	}
            }
        }

        stage('Deploy') {
            when {
                expression { params.branch == MAIN_BRANCH || params.branch.startsWith(RELEASE_BRANCH_PREFIX) }
            }

            agent {
                docker {
                    image 'drodb-build:latest'
                    reuseNode true
                }
            }

            steps {
               echo 'Deploying artifacts.'
               sh './mvnw deploy -Pdeploy -s settings.xml'
            }
        }

        stage('Release') {
            when {
                triggeredBy 'UserIdCause'
                expression { params.doRelease == true && (params.branch == MAIN_BRANCH || params.branch.startsWith(RELEASE_BRANCH_PREFIX)) }
            }

            agent {
                docker {
                    image 'drodb-build:latest'
                    reuseNode true
                }
            }

            steps {
                script {
                    sshagent(['DATAVERSE_GORGONA_GITHUB_DEPLOY_KEY']) {
                        echo "Performing the release of current SNAPSHOT version."
                        sh "git config user.email ${GIT_USER_EMAIL}"
                        sh "git config user.name ${GIT_USER_NAME}"
                        sh "./release.sh ${nextDevVersion(params.branch, params.nextMajor)}"
                    }
                }
            }
        }

    }
}

void withinContainer(body) {
    try {
        networkId = UUID.randomUUID().toString()
        sh "docker network inspect ${networkId} >/dev/null 2>&1 || docker network create --driver bridge ${networkId}"
        env.DOCKER_NETWORK_NAME = "${networkId}"

        docker.image('drodb-build:latest').inside("--network ${networkId}") { c ->
            body()
        }
    } finally {
        sh "docker ps -q --filter 'name=${SOLR_CONTAINER_ALIAS}|${POSTGRES_CONTAINER_ALIAS}' | xargs -r docker stop"
        sh "docker ps -q -a --filter 'name=${SOLR_CONTAINER_ALIAS}|${POSTGRES_CONTAINER_ALIAS}' | xargs -r docker rm"
        sh "docker network rm -f ${networkId}"
    }
}

void nextDevVersion(branch, nextMajor) {
    if (branch == MAIN_BRANCH) {
        if (nextMajor) {
            return 'major'
        }
        return 'minor'
    } else {
        return 'patch'
    }
}
