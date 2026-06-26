# Jenkins LTS com JDK 21 + plugins essenciais do Softon Portal pré-instalados.
# Construído pelo docker-compose.yml em ../docker-compose.yml.
# Spec: docs/jornadas/10-fluxo-local-completo-com-jenkins.md

FROM jenkins/jenkins:lts-jdk21

USER root

# gh CLI no agente (Jenkinsfile template usa pra release upload).
# Se a instalação falhar (mirror offline), o Jenkinsfile cai para curl direto.
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl ca-certificates; \
    install -d -m 0755 /usr/share/keyrings; \
    curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
      | tee /usr/share/keyrings/githubcli-archive-keyring.gpg > /dev/null; \
    chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg; \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
      > /etc/apt/sources.list.d/github-cli.list; \
    apt-get update; \
    apt-get install -y --no-install-recommends gh; \
    rm -rf /var/lib/apt/lists/*

USER jenkins

# Skip setup wizard — JCasC cria admin + tools + credentials no boot.
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
# IMPORTANTE: caminho FORA de /var/jenkins_home — o volume Docker mascara
# qualquer coisa que copiarmos pra lá no build, então o casc.yaml fica
# em /usr/share/jenkins/casc/ que pertence à imagem (não ao volume).
ENV CASC_JENKINS_CONFIG=/usr/share/jenkins/casc/casc.yaml

# Instala plugins reproduzíveis a partir do plugins.txt.
COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# Copia a configuração JCasC para fora do volume.
COPY --chown=jenkins:jenkins casc.yaml /usr/share/jenkins/casc/casc.yaml
