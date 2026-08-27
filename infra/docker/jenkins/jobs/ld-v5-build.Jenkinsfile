pipeline {
  agent any
  parameters {
    string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Versao sem prefixo v')
    string(name: 'TAG_NAME', defaultValue: '', description: 'Tag git (ex: V5.4.3) ou branch release/v5.4.1')
    string(name: 'GIT_REF', defaultValue: '', description: 'refs/tags/... ou refs/heads/...')
    string(name: 'PORTAL_PRODUCT_SIGLA', defaultValue: 'LD')
    string(name: 'ORIGEM', defaultValue: '')
  }
  options {
    timestamps()
    timeout(time: 40, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '20'))
  }
  tools {
    maven 'maven-3.9'
    jdk 'jdk-21'
  }
  stages {
    stage('Checkout') {
      steps {
        script {
          def tag = (params.TAG_NAME ?: '').toString().trim()
          def gitRef = (params.GIT_REF ?: '').toString().trim()
          if (!tag && !gitRef) {
            error 'Informe TAG_NAME ou GIT_REF'
          }
          if (!gitRef) {
            gitRef = tag.startsWith('release/') ? "refs/heads/${tag}" : "refs/tags/${tag}"
          }
          echo "LD V5 origem=${params.ORIGEM} tag=${tag} ref=${gitRef}"
          deleteDir()
          checkout([
            $class: 'GitSCM',
            branches: [[name: gitRef]],
            extensions: [[$class: 'CloneOption', shallow: false, noTags: false]],
            userRemoteConfigs: [[
              url: 'https://github.com/softonsi/dtec-ld.git',
              credentialsId: 'github-user-pat'
            ]]
          ])
        }
      }
    }
    stage('Build frontend') {
      steps { dir('frontend') { sh 'mvn -B -ntp clean package -DskipTests' } }
    }
    stage('Build backend') {
      steps { dir('backend') { sh 'mvn -B -ntp clean package -DskipTests' } }
    }
  }
  post {
    success {
      archiveArtifacts artifacts: 'backend/target/ldv4.war, frontend/target/ldv4-frontend.war',
                       fingerprint: true
    }
  }
}
