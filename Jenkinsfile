pipeline {
    agent {
        docker {
            image 'maven:3.9.9-eclipse-temurin-21'
            args '--network travelplan-ci'
            reuseNode true
        }
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Build and test backend services') {
            steps {
                sh '''
                    set -eu
                    for service in gateway user-service travel-service; do
                        mvn -B -f "services/${service}/pom.xml" clean verify
                    done
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'services/*/target/surefire-reports/*.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'services/*/target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('SonarQube analysis') {
            steps {
                script {
                    def scannerHome = tool 'SonarScanner'
                    withSonarQubeEnv('SonarQube') {
                        sh "\"${scannerHome}/bin/sonar-scanner\" -Dproject.settings=infrastructure/ci-platform/sonar-project.properties"
                    }
                }
            }
        }
    }
}
