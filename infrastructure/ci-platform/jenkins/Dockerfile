FROM jenkins/jenkins:lts-jdk21

USER root

ARG SONAR_SCANNER_VERSION=7.2.0.5079
ARG KUBECTL_VERSION=v1.30.4
ARG NODE_VERSION=22.22.0

RUN apt-get update 
    && apt-get install -y --no-install-recommends 
        ansible 
        curl 
        docker.io 
        docker-cli 
        gosu 
        jq 
        maven 
        python3-dotenv 
        unzip 
        xz-utils 
        ca-certificates 
    && test -x /usr/bin/docker 
    && /usr/bin/docker --version 
    && curl -fsSLo /usr/local/bin/kubectl 
        "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" 
    && chmod 0755 /usr/local/bin/kubectl 
    && curl -fsSLo /tmp/sonar-scanner.zip 
        "https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux-x64.zip" 
    && unzip -q /tmp/sonar-scanner.zip -d /opt 
    && ln -s "/opt/sonar-scanner-${SONAR_SCANNER_VERSION}-linux-x64/bin/sonar-scanner" /usr/local/bin/sonar-scanner 
    && curl -fsSLo /tmp/node.tar.xz 
        "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.xz" 
    && mkdir -p /opt/node 
    && tar -xJf /tmp/node.tar.xz -C /opt/node --strip-components=1 
    && ln -s /opt/node/bin/node /usr/local/bin/node 
    && ln -s /opt/node/bin/npm /usr/local/bin/npm 
    && ln -s /opt/node/bin/npx /usr/local/bin/npx 
    && node --version 
    && npm --version 
    && rm -f /tmp/sonar-scanner.zip /tmp/node.tar.xz 
    && rm -rf /var/lib/apt/lists/*

COPY ./jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt
COPY ./jenkins/install-plugins.sh /usr/local/bin/install-jenkins-plugins
COPY ./jenkins/entrypoint.sh /usr/local/bin/jenkins-entrypoint

RUN chmod 0755 /usr/local/bin/install-jenkins-plugins 
    && chmod 0755 /usr/local/bin/jenkins-entrypoint

# Run Jenkins jobs as the unprivileged Jenkins user.
USER jenkins

# FROM jenkins/jenkins:lts-jdk21

# USER root
# ARG SONAR_SCANNER_VERSION=7.2.0.5079
# ARG KUBECTL_VERSION=v1.30.4
# RUN apt-get update \
#     && apt-get install -y --no-install-recommends ansible curl docker.io docker-cli gosu jq maven python3-dotenv unzip \
#     && test -x /usr/bin/docker \
#     && /usr/bin/docker --version \
#     && curl -fsSLo /usr/local/bin/kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
#     && chmod 0755 /usr/local/bin/kubectl \
#     && curl -fsSLo /tmp/sonar-scanner.zip "https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux-x64.zip" \
#     && unzip -q /tmp/sonar-scanner.zip -d /opt \
#     && ln -s "/opt/sonar-scanner-${SONAR_SCANNER_VERSION}-linux-x64/bin/sonar-scanner" /usr/local/bin/sonar-scanner \
#     && rm -f /tmp/sonar-scanner.zip \
#     && rm -rf /var/lib/apt/lists/*

# COPY ./jenkins/plugins.txt /usr/share/jenkins/ref/plugins.txt
# COPY ./jenkins/install-plugins.sh /usr/local/bin/install-jenkins-plugins
# COPY ./jenkins/entrypoint.sh /usr/local/bin/jenkins-entrypoint
# RUN chmod 0755 /usr/local/bin/install-jenkins-plugins \
#     && chmod 0755 /usr/local/bin/jenkins-entrypoint

# # Install the required plugins at runtime instead of during image build so the build
# # is not blocked by transient update-center connectivity issues.
# USER jenkins
