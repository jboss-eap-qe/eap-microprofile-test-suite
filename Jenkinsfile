pipeline {
    agent { label 'linux' }
    tools {
        jdk 'oracle-java-11'
    }
    parameters {
        string(name: 'wfRepo', description: 'WildFly repository', defaultValue: "https://github.com/wildfly/wildfly.git" )
        string(name: 'wfBranch', description: 'WildFly branch', defaultValue: "master" )
        string(name: 'tsRepo', description: 'WildFly repository', defaultValue: "https://github.com/jboss-eap-qe/eap-microprofile-test-suite.git" )
        string(name: 'tsBranch', description: 'WildFly branch', defaultValue: "master" )
    }
    options {
        timestamps()
        ansiColor("xterm")
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }
    stages {
        stage('Checkout') {
            steps {
                echo "${params.wfBranch} - ${params.wfRepo}"
                dir('wildfly') {
                    git branch: "${params.wfBranch}", url: "${params.wfRepo}"
                }
                dir('eap-microprofile-test-suite') {
                    git branch: "${params.tsBranch}", url: "${params.tsRepo}"
                }
            }
        }
        stage ('Path / Java info') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    which java
                ''' 
            }
        }
        stage('Build WildFly') {
            steps {
                sh '''
                    cd wildfly
                    ./mvnw -B clean install -DskipTests -Denforcer.skip=true -Dcheckstyle.skip=true
                '''
            }
        }
        stage('Run MP TS') {
            steps {
                sh '''
                    WF_RELATIVE_PATH=`find wildfly/dist/target -mindepth 1 -maxdepth 1 -type d | grep "target/wildfly"`
                    WF_HOME=${PWD}/${WF_RELATIVE_PATH}
                    cd eap-microprofile-test-suite
                    ./mvnw -B clean verify -Denforcer.skip=true -Djboss.dist=$WF_HOME
                '''
            }
        }        
        stage('Reports') {
            parallel {
                stage('Disk usage') {
                    steps {
                        sh 'du -cskh */*'
                    }
                }
                stage('Archive artifacts') {
                    steps {
                        archiveArtifacts artifacts: '**/target/*-reports/TEST*.xml', fingerprint: false
                    }
                }
                stage("Build desctiption") {
                    steps {
                        script {
                            currentBuild.description = "${params.wfRepo} - ${params.wfBranch}\n${params.tsRepo} - ${params.tsBranch}"
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            deleteDir()
        }
    }
}