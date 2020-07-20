/* Project 4 letters. */ 
def project                 = 'BMDL'
def deploymentEnvironment   = 'dev'
def appName                 = "demo";
def appVersion              = "1.1";
def AzureResourceName       = "prueba";
def azureWebApp             = "demo-app-inct";
def dockerRegistryUrl       = "vlliuyadesa.azurecr.io";
def imageTag                = "${dockerRegistryUrl}/${appName}:${appVersion}";
def pushId                  = "${demo-acr-push-sp-id}";
def pushPassword            = "${demo-acr-push-sp-password}";

/* Mail configuration*/
// If recipients is null the mail is sent to the person who start the job
// The mails should be separated by commas(',')
//def recipients

try {
   node { 
      stage('Preparation') {
        checkout([$class: 'GitSCM', branches: [[name: '*/master']], 
        doGenerateSubmoduleConfigurations: false, 
        extensions: [], 
        submoduleCfg: [], 
        userRemoteConfigs: [[credentialsId: 'github-jenkins', 
        url: 'https://github.com/alonsoTI/demo-app.git']]])
      }

      stage('Build & UT') {
        steps.echo """
        ******** BUILDING DOCKER IMAGE ********
        """
        steps.sh "docker build -t ${imageTag} ."
      }

      stage('QA Analisys') {
        steps.echo """
        ******** QA Analysis ********
        """
      }
     
      stage('Deploy Artifact') {
        steps.echo """
        ******** Deploy Artifact ********
        """
      }

      stage("Deploy to " +deploymentEnvironment){

        steps.echo """
        ******** PUSHING DOCKER IMAGE ********
        """
    /**
      * Docker login
      * Docker push
      * Docker logout
    */
    steps.withCredentials([
      [$class: "StringBinding", credentialsId: "${pushId}", variable: "pushId" ],
      [$class: "StringBinding", credentialsId: "${pushPassword}", variable: "pushPassword" ]
    ]){
      try{
        //Inicio de sesión en acr
        steps.sh """
          set +x
          docker login ${dockerRegistryUrl} --username ${script.env.pushId} --password ${script.env.pushPassword}  
        """
        steps.sh "docker push ${imageTag}" //Subo imagen
        steps.sh "docker logout ${dockerRegistryUrl}" //Cierro sesión del acr
        steps.sh "docker rmi ${imageTag}" //Elimino la imagen creada

      }catch(Exception e){
        throw e;
      }
    }
  
    steps.withCredentials([
            [$class: "StringBinding", credentialsId: "${demo-acr-pull-sp-id}", variable: "pullId" ],
            [$class: "StringBinding", credentialsId: "${demo-acr-pull-sp-password}", variable: "pullPassword" ],
            [$class: "StringBinding", credentialsId: "${demo-inct-webapp-id}", variable: "webappId" ],
            [$class: "StringBinding", credentialsId: "${demo-inct-webapp-password}", variable: "webappPassword" ],
            [$class: "StringBinding", credentialsId: "${demo-inct-webapp-tenant}", variable: "tenantId" ]
          ]){
            try{
              steps.echo """
                ******** LOGIN WEBAPP APP ********
              """
              steps.sh "az login --service-principal --username ${script.env.webappId} --password ${script.env.webappPassword} --tenant ${script.env.tenantId}"


              steps.echo """
                ******** CONFIGURING WEBAPP CONTAINER SETTINGS ********
              """
              steps.sh "az webapp config container set -n ${azureWebApp} -g ${AzureResourceName} -c ${imageTag} -r ${dockerRegistryUrl} -u ${script.env.pullId} -p ${script.env.pullPassword}"

              steps.echo """
                ******** DEPLOY CONTAINER ON AZURE WEBAPP ********
              """
              steps.sh "az webapp restart -g ${AzureResourceName} -n ${azureWebApp}"
            }catch(Exception e){
              throw e;
            }
          }
      }

      stage('Results') {
         utils.saveResultNode("tgz")
      }

      stage('Post Execution') {
         
      }
   }
} catch(Exception e) {
   node {
    throw e
   }
}