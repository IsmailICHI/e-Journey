pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
    }

    environment {
        SONAR_URL = 'http://sonarqube:9000'
        K8S_NAMESPACE = 'travel-plan'
        IMAGE_TAG = 'dev'
    }

    stages {
        stage('Validate repository') {
            steps {
                sh '''
                    set -eu
                    docker version
                    test -f sonar-project.properties
                    test -f infrastructure/ansible/playbooks/deploy-ci.yml
                    test -f k8s/overlays/dev/kustomization.yaml
                    test -f services/gateway/pom.xml
                    test -f services/user-service/pom.xml
                    test -f services/travel-service/pom.xml
                    kubectl kustomize k8s/overlays/dev > "$WORKSPACE/travel-plan-rendered.yaml"
                '''
            }
        }

        stage('Test backend services') {
            steps {
                sh '''
                    set -eu
                    for service in gateway user-service travel-service; do
                        mvn -B -f "services/$service/pom.xml" clean verify
                    done
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'services/*/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build application images') {
            steps {
                sh '''
                    set -eu
                    mkdir -p .ci-images
                                        docker build --file services/client/Dockerfile \
                                            --tag "travelplan/frontend:$IMAGE_TAG" .
                                        docker build --file services/gateway/Dockerfile \
                                            --tag "travelplan/gateway:$IMAGE_TAG" .
                                        docker build --file services/user-service/Dockerfile \
                                            --tag "travelplan/user-service:$IMAGE_TAG" .
                                        docker build --file services/travel-service/DockerFile \
                                            --tag "travelplan/travel-service:$IMAGE_TAG" .
                    for image in frontend gateway user-service travel-service; do
                                                docker save --output ".ci-images/$image.tar" \
                                                    "travelplan/$image:$IMAGE_TAG"
                    done
                '''
            }
        }

        stage('SonarQube analysis and quality gate') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        set +x
                        sonar-scanner \\
                          -Dproject.settings=sonar-project.properties \\
                          -Dsonar.host.url="$SONAR_URL" \\
                          -Dsonar.token="$SONAR_TOKEN"
                    '''
                    sh '''
                        set +x
                        set -eu
                        task_url="$(sed -n 's/^ceTaskUrl=//p' .scannerwork/report-task.txt)"
                        test -n "$task_url"
                        analysis_id=
                        task_status=
                        for attempt in $(seq 1 36); do
                            task_json="$(curl --fail --silent --show-error --user "$SONAR_TOKEN:" "$task_url")"
                            task_status="$(printf '%s' "$task_json" | jq -r '.task.status')"
                            analysis_id="$(printf '%s' "$task_json" | jq -r '.task.analysisId // empty')"
                            if [ "$task_status" = SUCCESS ]; then
                                break
                            fi
                            if [ "$task_status" = FAILED ] || [ "$task_status" = CANCELED ]; then
                                echo "SonarQube background task ended with status: $task_status" >&2
                                exit 1
                            fi
                            sleep 5
                        done
                        test "$task_status" = SUCCESS
                        test -n "$analysis_id"
                        gate_json="$(curl --fail --silent --show-error --user "$SONAR_TOKEN:" \
                          "$SONAR_URL/api/qualitygates/project_status?analysisId=$analysis_id")"
                        gate_status="$(printf '%s' "$gate_json" | jq -r '.projectStatus.status')"
                        if [ "$gate_status" != OK ]; then
                            echo "SonarQube quality gate failed: $gate_status" >&2
                            exit 1
                        fi
                    '''
                }
            }
        }

        stage('Deploy to k3s VM') {
            when {
                beforeAgent true
                expression {
                    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH
                    return branch == 'infra' || branch == 'origin/infra'
                }
            }
            steps {
                withCredentials([file(credentialsId: 'travel-plan-env-file', variable: 'ENV_FILE')]) {
                    sh '''
                    set -eu
                    cp "$ENV_FILE" .env
                    echo "================================================================================================"
                    echo "===============================.ENV HAS BEEN COPIED SUCCESSFULLY ==============================="
                    echo "================================================================================================"
                    cat .env
                    echo "================================================================================================"

                    test -r /run/jenkins-project.env
                    test -r /run/jenkins-k3s-key
                    install -m 0600 /run/jenkins-project.env .env
                    echo "================================================================================================"
                    echo "===============================.ENV TEST 2 ==============================="
                    echo "================================================================================================"
                    cat /run/jenkins-project.env
                    export ANSIBLE_CONFIG="$WORKSPACE/infrastructure/ansible/ansible.cfg"
                    export ANSIBLE_ROLES_PATH="$WORKSPACE/infrastructure/ansible/roles"
                    export ANSIBLE_HOST_KEY_CHECKING=True
                    
                    echo "================================================================================================"
                    python3 - <<'PY'
                    from dotenv import dotenv_values
                    from pathlib import Path
                    
                    path = Path(".env").resolve()
                    values = dotenv_values(path)
                    
                    print("File:", path)
                    print("File exists:", path.is_file())
                    print("REDIS_PASSWORD key exists:", "REDIS_PASSWORD" in values)
                    print("Parsed value is None:", values.get("REDIS_PASSWORD") is None)
                    print("Parsed value is empty:", values.get("REDIS_PASSWORD") == "")
                    print("Parsed value length:", len(values.get("REDIS_PASSWORD") or ""))
                    PY

                    python3 infrastructure/ci-platform/scripts/with-dotenv.py .env \
                      ansible-playbook \
                      -i infrastructure/ansible/inventories/dev/hosts.yml \
                      infrastructure/ansible/playbooks/deploy-ci.yml \
                                            --extra-vars "deployment_source=$WORKSPACE \
                                                ci_image_archive_dir=$WORKSPACE/.ci-images \
                                                ansible_host=host.docker.internal \
                                                ansible_port=22 \
                                                ansible_user=vagrant \
                                                ansible_ssh_private_key_file=/run/jenkins-k3s-key"
                '''
                }
            }
        }
    }

    post {
        always {
            sh 'rm -f .env; rm -rf .ci-images .scannerwork'
            archiveArtifacts allowEmptyArchive: true, artifacts: 'travel-plan-rendered.yaml'
        }
        failure {
            echo 'The pipeline failed. Review the test, SonarQube, or k3s deployment stage output.'
        }
    }
}
