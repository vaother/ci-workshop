pipeline {
    agent any
    environment {
        // แก้ไขเป็นของผู่เรียน
        USERID = 'demo'
        USERGITHUB = 'peerapach'
    }
    stages {
        stage("Checkout code") {
            steps {
                git branch: 'master',
                    url: "https://github.com/${env.USERGITHUB}/ci-workshop.git"
            }
        }
        stage("Unit test") {
            steps {
                withDockerContainer("python:3.6") {
                    sh """
                        export PYTHONUSERBASE=/tmp
                        cd src
                        pip install --no-cache-dir -r requirements.txt
                        python -m unittest
                    """
                }
            }
        }        
        stage("Build image") {
            steps {
                sh """
                    sed -i 's/USERID/${USERID}/g' Dockerfile
                    sed -i 's/K8S_API_SERVER/${K8S_API_SERVER}/g' Dockerfile
                """
                script {
                    myapp = docker.build("cicdday/${USERID}-hello:${env.BUILD_ID}")
                }
            }
        }
        stage("Push image") {
            steps {
                script {
                    docker.withRegistry('', 'dockerhub') {
                        myapp.push("${env.BUILD_ID}")
                    }
                }
            }
        }        
        stage('Deploy to GKE') {
            steps{
                sh """
                    echo "Deploy to GKE"
                """
            }
        }
    }    
}
