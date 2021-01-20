pipeline {
    agent any
    environment {
        // แก้ไขเป็นของผู่เรียน
        USERID = 'user39'
        USERGITHUB = 'ezylinux'
        // ไม่ต้องแก้ไข
        CREDENTIALS_ID = 'gke'
        PROJECTID='fluid-analogy-267415'
        CLUSTERNAME='cluster-1'
        CLUSTERLOCATION='asia-southeast1-c'
    }
    stages {
        stage("Checkout code") {
            steps {
                deleteDir();
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
                script {
                    RESP = sh(script: "curl --write-out %{http_code} --silent -m 5 --output /dev/null ${USERID}-green.workshop.ezylinux.com",
                                       returnStdout: true).trim()
                    println(RESP)
                    sh "[ -d "deployment" ] && mkdir -p deployment"
                    if (RESP == "200") {
                        sh """
                            cp -f CD-Workshop/blue-green/deployment-blue.yaml deployment/deployment.yaml
                            cp -f CD-Workshop/blue-green/deployment-green.yaml deployment/deployment-scale-down.yaml
                            cp -f CD-Workshop/blue-green/ingress-blue-testing.yaml deployment/ingress-testing.yaml
                            cp -f CD-Workshop/blue-green/ingress-blue.yaml deployment/ingress.yaml
                        """
                        deploySide = "blue"
                    } else {
                       sh """
                            cp -f CD-Workshop/blue-green/deployment-green.yaml deployment/deployment.yaml
                            cp -f CD-Workshop/blue-green/deployment-blue.yaml deployment/deployment-scale-down.yaml
                            cp -f CD-Workshop/blue-green/ingress-green-testing.yaml deployment/ingress-testing.yaml
                            cp -f CD-Workshop/blue-green/ingress-green.yaml deployment/ingress.yaml
                        """
                        deploySide = "green"                            
                        
                    }
                    println deploySide
                }
                sh """
                    sed -i 's/#USER#/${USERID}/g' deployment/deployment.yaml
                    sed -i 's/#APPUSER#/${USERID}-hello:${env.BUILD_ID}/g' deployment/deployment.yaml
                    sed -i 's/#DOCKER-HUB-USERNAME#/cicdday/g' deployment/deployment.yaml
                    
                    sed -i 's/#USER#/${USERID}/g' deployment/deployment-scale-down.yaml
                    sed -i 's/replicas: 1/replicas: 0/g' deployment/deployment-scale-down.yaml 
                    sed -i 's/#APPUSER#/${USERID}-hello:${env.BUILD_ID}/g' deployment/deployment-scale-down.yaml
                    sed -i 's/#DOCKER-HUB-USERNAME#/cicdday/g' deployment/deployment-scale-down.yaml     
                    
                    sed -i 's/#USER#/${USERID}/g' deployment/ingress-testing.yaml
                    sed -i 's/#USER#/${USERID}/g' deployment/ingress.yaml

                """

                step([$class: 'KubernetesEngineBuilder', 
                      projectId: env.PROJECTID, 
                      namespace: env.USERID,
                      clusterName: env.CLUSTERNAME, 
                      location: env.CLUSTERLOCATION, 
                      manifestPattern: 'deployment/deployment.yaml', 
                      credentialsId: env.CREDENTIALS_ID, 
                      verifyDeployments: true])

                step([$class: 'KubernetesEngineBuilder', 
                      projectId: env.PROJECTID, 
                      namespace: env.USERID,
                      clusterName: env.CLUSTERNAME, 
                      location: env.CLUSTERLOCATION, 
                      manifestPattern: 'deployment/ingress-testing.yaml', 
                      credentialsId: env.CREDENTIALS_ID, 
                      verifyDeployments: false])                   
                      
                input 'Should we continue?'
                
                step([$class: 'KubernetesEngineBuilder', 
                      projectId: env.PROJECTID, 
                      namespace: env.USERID,
                      clusterName: env.CLUSTERNAME, 
                      location: env.CLUSTERLOCATION, 
                      manifestPattern: 'deployment/ingress.yaml', 
                      credentialsId: env.CREDENTIALS_ID, 
                      verifyDeployments: false])                 
                      
                step([$class: 'KubernetesEngineBuilder', 
                      projectId: env.PROJECTID, 
                      namespace: env.USERID,
                      clusterName: env.CLUSTERNAME, 
                      location: env.CLUSTERLOCATION, 
                      manifestPattern: 'deployment/deployment-scale-down.yaml', 
                      credentialsId: env.CREDENTIALS_ID, 
                      verifyDeployments: false])                        
            }
        }
    }    
}        
