import groovy.json.*

@NonCPS
def add_build_config(ci_builds_list){
    def slurper = new groovy.json.JsonSlurperClassic()
    def json = slurper.parseText(ci_builds_list)
    def builder = new groovy.json.JsonBuilder()
    def newBuild = builder.build_config {
        repo_name  "${RepositoryName}"
        days_to_keep       "${DaysToKeep}"
        max_builds_num "${MaxBuildsNum}"
    }
    json.("${JobPath}") = newBuild.build_config

    Map ci_list = slurper.parseText(new groovy.json.JsonBuilder(json).toPrettyString())
    return new groovy.json.JsonBuilder(ci_list.sort()).toPrettyString()
}

def check_permissions() {
    can_clone = sh (
        script: "git ls-remote --exit-code https://github.com/cbdr/${RepositoryName}",
        returnStatus: true
    ) == 0
    if (!can_clone) {
        def msg =   ''' Jenkins does not have adequate permissions for the repository given.
        Please give write access to the Github service account "CloudOpsSvc" and try again.
        This can be done by adding the service account to a team that has write access to the repository or adding the service account directly as a collaborator.
        '''
        error(msg)
    }
}

timestamps {
    node('master') {
        wrap([$class: 'BuildUser']) {
            stage('Checkout') {
                deleteDir()
                checkout scm
                check_permissions()
            }
            stage('Add To Config') {
                if (!JobPath.endsWith('/Build')) {
                    JobPath = JobPath + '/Build'
                }
                def ci_builds_list = readFile('Jenkins/Config/CI.json')
                writeFile file: 'Jenkins/Config/CI.json', text: add_build_config(ci_builds_list)
            }
            stage('Verify Folders') {
                dir('Jenkins/Pipelines/Shared/CloudOps') {
                    sh '''#!/bin/bash -l
                    groovy FoldersTest.gvy --auto-fix
                    '''
                }
            }
            stage('Commit & Push') {
                sh """#!/bin/bash -l
                git config user.name "azeemsfa"
                git config user.email "sam.hima@gmail.com"
                git checkout -B ${RepositoryName}-${env.BUILD_DISPLAY_NAME}
                git add -A
                git commit -m "Submitted by: ${env.BUILD_USER_ID}."
                git push origin ${RepositoryName}-${env.BUILD_DISPLAY_NAME}
                """
            }
            stage('Submit a PR') {
                withCredentials([usernamePassword(credentialsId: 'gitub', passwordVariable: 'PATOKEN',
                    usernameVariable: 'GHUSERNAME')]) {
                    sh """#!/bin/bash -l
                    curl -u ${GHUSERNAME}:${PATOKEN} \
                    -Ss \
                    --data '{"title": "auto-ci", "head": "${RepositoryName}-${env.BUILD_DISPLAY_NAME}", "base": "master", "body": "Request a review from 'cbdr/cloudops' and your PR will be auto-merged."' \
                    https://api.github.com/repos/azeemsfa/Jenkins_automation/pulls
                    """
                }
            }
        }
    }
}
