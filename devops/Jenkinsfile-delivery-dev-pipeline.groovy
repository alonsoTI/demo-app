/* Project 4 letters. */ 
def project                 = 'BMDL'
def deploymentEnvironment   = 'dev'
def appName                 = "databricks-cli";
def appVersion              = "1.0";
def AzureResourceName       = "prueba";
def azureWebApp             = "demo-app-inct";
def dockerRegistryUrl       = "vlliuyadesa.azurecr.io";
def imageTag                = "${dockerRegistryUrl}/${appName}:${appVersion}";
def databricksHost          = "https://eastus2.azuredatabricks.net";
def databricksContainer     = "";

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
        //steps.sh "docker build -t ${imageTag} ."
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
      [$class: "StringBinding", credentialsId: "pushId", variable: "pushId" ],
      [$class: "StringBinding", credentialsId: "pushPassword", variable: "pushPassword" ]
    ]){
      try{
        //Inicio de sesión en acr
        
        steps.sh """
          set +x
          docker login ${dockerRegistryUrl} --username ${env.pushId} --password ${env.pushPassword}  
        """
        /*
        steps.sh "docker push ${imageTag}" //Subo imagen
        steps.sh "docker logout ${dockerRegistryUrl}" //Cierro sesión del acr
        steps.sh "docker rmi ${imageTag}" //Elimino la imagen creada
        */
      }catch(Exception e){
        throw e;
      }
    }
  
    steps.withCredentials([
            [$class: "StringBinding", credentialsId: "pullId", variable: "pullId" ],
            [$class: "StringBinding", credentialsId: "pullPassword", variable: "pullPassword" ],
            [$class: "StringBinding", credentialsId: "webappId", variable: "webappId" ],
            [$class: "StringBinding", credentialsId: "webappPassword", variable: "webappPassword" ],
            [$class: "StringBinding", credentialsId: "tenantId", variable: "tenantId" ],
            [$class: "StringBinding", credentialsId: "	databricksToken", variable: "	databricksToken" ]
          ]){
            try{
              
              /*
              steps.echo """
                ******** LOGIN WEBAPP APP ********
              """
              steps.sh "az login --service-principal --username ${env.webappId} --password ${env.webappPassword} --tenant ${env.tenantId}"


              steps.echo """
                ******** CONFIGURING WEBAPP CONTAINER SETTINGS ********
              """
              steps.sh "az webapp config container set -n ${azureWebApp} -g ${AzureResourceName} -c ${imageTag} -r ${dockerRegistryUrl} -u ${env.pullId} -p ${env.pullPassword}"

              steps.echo """
                ******** DEPLOY CONTAINER ON AZURE WEBAPP ********
              """
              steps.sh "az webapp restart -g ${AzureResourceName} -n ${azureWebApp}"
              */
              databricksContainer = steps.sh(script:"docker run -d -it ${imageTag}",returnStdout:true).trim();
              steps.sh "docker exec ${databricksContainer} -e ${databricksHost} -${env.databricksToken} jobs list";

            }catch(Exception e){
              throw e;
            }
          }
      }

      stage('Post Execution') {
         steps.echo """
                URL DEPLOY : http://demo-app-inct.azurewebsites.net/web/  ********
        """
      }
   }
} catch(Exception e) {
   node {
    throw e
   }
}
