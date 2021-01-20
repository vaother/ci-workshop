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
                    sed -i 's/#USER#/${USERID}/g' CD-Workshop/rolling-update/deployment.yaml
                    sed -i 's/#APPUSER#/${USERID}-hello:${env.BUILD_ID}/g' CD-Workshop/rolling-update/deployment.yaml
                    sed -i 's/#DOCKER-HUB-USERNAME#/cicdday/g' CD-Workshop/rolling-update/deployment.yaml
                """
                
                step([$class: 'KubernetesEngineBuilder', 
                      projectId: env.PROJECTID, 
                      namespace: env.USERID,
                      clusterName: env.CLUSTERNAME, 
                      location: env.CLUSTERLOCATION, 
                      manifestPattern: 'CD-Workshop/rolling-update/deployment.yaml', 
                      credentialsId: env.CREDENTIALS_ID, 
                      verifyDeployments: true])
            }
        }
    }    
}
