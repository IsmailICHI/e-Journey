pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
    }

    environment {
        REGISTRY_URL = 'ghcr.io/replace-with-your-org'
        IMAGE_TAG = "${GIT_COMMIT.take(12)}"
        K8S_NAMESPACE = 'travel-plan'
    }

    stages {
        stage('Validate repository layout') {
            steps {
                sh '''
                    set -eu
                    test -f services/gateway/pom.xml
                    test -f services/user-service/pom.xml
                    test -f services/travel-service/pom.xml
                    test -f k8s/overlays/dev/kustomization.yaml
                    if grep -R "payment-service" -n k8s >/dev/null 2>&1; then
                        echo "Payment service is still present in the active k8s manifests." >&2
                        exit 1
                    fi
                '''
            }
        }

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

        stage('Build container images') {
            steps {
                sh '''
                    set -eu
                    for service in gateway user-service travel-service; do
                        docker build --file "services/${service}/Dockerfile" --tag "${REGISTRY_URL}/travel-plan-${service}:${IMAGE_TAG}" .
                    done
                '''
            }
        }

        stage('Push images') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'container-registry',
                    usernameVariable: 'REGISTRY_USER',
                    passwordVariable: 'REGISTRY_TOKEN'
                )]) {
                    sh '''
                        set -eu
                        echo "$REGISTRY_TOKEN" | docker login ghcr.io --username "$REGISTRY_USER" --password-stdin
                        for service in gateway user-service travel-service; do
                            docker push "${REGISTRY_URL}/travel-plan-${service}:${IMAGE_TAG}"
                        done
                        docker logout ghcr.io
                    '''
                }
            }
        }

        stage('Validate Kubernetes manifests') {
            steps {
                sh '''
                    set -eu
                    kubectl kustomize k8s/overlays/dev > "$WORKSPACE/travel-plan-rendered.yaml"
                    kubectl apply --dry-run=client -k k8s/overlays/dev
                '''
            }
        }

        stage('Deploy to k3s development cluster') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG_FILE')]) {
                    sh '''
                        set -eu
                        export KUBECONFIG="$WORKSPACE/.kubeconfig"
                        cp "$KUBECONFIG_FILE" "$KUBECONFIG"
                        kubectl apply -k k8s/overlays/dev
                        kubectl -n "$K8S_NAMESPACE" rollout status deployment/gateway --timeout=180s
                        kubectl -n "$K8S_NAMESPACE" rollout status deployment/user-service --timeout=180s
                        kubectl -n "$K8S_NAMESPACE" rollout status deployment/travel-service --timeout=180s
                    '''
                }
            }
        }

        stage('Verify rollout health') {
            when {
                branch 'main'
            }
            steps {
                sh '''
                    set -eu
                    kubectl -n "$K8S_NAMESPACE" get pods
                    kubectl -n "$K8S_NAMESPACE" get ingress
                    kubectl -n "$K8S_NAMESPACE" get hpa
                '''
            }
        }

        stage('Approve staging deployment') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Approve deployment to staging or higher environment?', ok: 'Proceed'
            }
        }
    }

    post {
        always {
            archiveArtifacts allowEmptyArchive: true, artifacts: 'travel-plan-rendered.yaml'
        }
        failure {
            echo 'The deployment pipeline failed. Check the build, static analysis, or rollout status above.'
        }
    }
}
