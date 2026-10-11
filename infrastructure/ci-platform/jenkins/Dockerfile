FROM jenkins/jenkins:lts-jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io \
    && rm -rf /var/lib/apt/lists/*

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
COPY install-plugins.sh /usr/local/bin/install-jenkins-plugins
RUN chmod 0755 /usr/local/bin/install-jenkins-plugins \
    && /usr/local/bin/install-jenkins-plugins

USER jenkins
