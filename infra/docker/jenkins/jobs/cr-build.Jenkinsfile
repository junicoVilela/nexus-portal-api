pipeline {
  agent any
  parameters {
    string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Versao sem prefixo v')
    string(name: 'TAG_NAME', defaultValue: '', description: 'Tag git (ex: v2.3.18), branch main-coso ou release/v2.2.31')
    string(name: 'GIT_REF', defaultValue: '', description: 'refs/tags/... ou refs/heads/...')
    string(name: 'PORTAL_PRODUCT_SIGLA', defaultValue: 'CR')
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
            if (tag == 'main-coso' || tag.startsWith('release/')) {
              gitRef = "refs/heads/${tag}"
            } else {
              gitRef = "refs/tags/${tag}"
            }
          }
          echo "CR origem=${params.ORIGEM} tag=${tag} ref=${gitRef}"
          deleteDir()
          checkout([
            $class: 'GitSCM',
            branches: [[name: gitRef]],
            extensions: [[$class: 'CloneOption', shallow: false, noTags: false]],
            userRemoteConfigs: [[
              url: 'https://github.com/softonsi/dtec-risco.git',
              credentialsId: 'github-user-pat'
            ]]
          ])
        }
      }
    }
    stage('Build') {
      steps { sh 'mvn -B -ntp clean package -DskipTests' }
    }
  }
  post {
    success {
      archiveArtifacts artifacts: 'target/dtec-class-risco.war', fingerprint: true
    }
  }
}
