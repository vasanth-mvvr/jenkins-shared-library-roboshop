def call(Map configMap){
    pipeline{
        agent{
            label: 'AGENT-1'
        }
        options{
            timeout(time: 30,unit: 'MINUTES')
            disableConcurrentBuilds()
            ansiColor('xterm')
        }
        parameters{
            booleanParam(name: 'deploy', defaultValue: false, description : 'select to deploy or not')
        }
        environment{
            APP_VERSION = ''   // this can be used for global
            environment = pipelineGlobals.environment()
            region = pipelineGlobals.region()
            accont_id = pipelineGlobals.account_ID()
            project = configMap.get("project")
            component = configMap.get("component")
        }
        stages{
            stage('Read the Version'){
                steps{
                    script{
                        def pom = readMavenPom file: 'pom.xml'
                         APP_VERSION = pom.version
                        echo "App Version : ${APP_VERSION} "
                    }
                }
            }
            stage('Install Dependencies'){
                steps{
                    sh """
                        pip3.11 install -r requirements.txt
                    """
                }
            }
            stage('Docker Build'){
                steps{
                    withAWS(region: 'us-east-1',credentials: "aws-cred-${environment}"){
                        sh """
                            aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.${region}.amazonaws.com

                            docker build -t ${account_id}.dkr.ecr.${region}.amazonaws.com/${project}-${component}:${APP_VERSION}

                            docker images

                            docker push ${account_id}.dkr.ecr.${region}.amazonaws.com/${project}-${component}:${APP_VERSION}
                        """
                    }
                }
            }
            stage('Deploy'){
                when{
                    expression {params.deploy}
                }
                steps{
                    def params[
                        string(name: 'version',value: "${APP_VERSION}"),
                        string(name: 'ENVIRONMENT',value: "${environment}"),
                    ]
                    build job: "../${component}-cd",parameters:params,wait:true 
                }
            }
        }
        post{
            always{
                echo "This section runs always"
                deleteDir()
            }
            success{
                echo "This pileline is success"
            }
            failure{
                echo "This section is failure"
            }
        }
    }
}