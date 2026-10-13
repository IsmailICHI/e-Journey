FROM jenkins/jenkins:lts-jdk21

USER root
ARG SONAR_SCANNER_VERSION=7.2.0.5079
ARG KUBECTL_VERSION=v1.30.4
RUN apt-get update \
    && apt-get install -y --no-install-recommends ansible curl docker.io gosu jq maven python3-dotenv unzip \
    && curl -fsSLo /usr/local/bin/kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
    && chmod 0755 /usr/local/bin/kubectl \
    && curl -fsSLo /tmp/sonar-scanner.zip "https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux-x64.zip" \
    && unzip -q /tmp/sonar-scanner.zip -d /opt \
    && ln -s "/opt/sonar-scanner-${SONAR_SCANNER_VERSION}-linux-x64/bin/sonar-scanner" /usr/local/bin/sonar-scanner \
    && rm -f /tmp/sonar-scanner.zip \
    && rm -rf /var/lib/apt/lists/*

COPY ./jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt
COPY ./jenkins/install-plugins.sh /usr/local/bin/install-jenkins-plugins
COPY ./jenkins/entrypoint.sh /usr/local/bin/jenkins-entrypoint
RUN chmod 0755 /usr/local/bin/install-jenkins-plugins \
    && chmod 0755 /usr/local/bin/jenkins-entrypoint

# Install the required plugins at runtime instead of during image build so the build
# is not blocked by transient update-center connectivity issues.
USER jenkins
