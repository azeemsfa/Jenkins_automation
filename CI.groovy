if (System.getenv()['isgradle']) {
  return
}

import groovy.json.JsonSlurper

def slurper = new groovy.json.JsonSlurper()
slurper.parseText(readFileFromWorkspace('Jenkins/Config/CI.json')).each {
    path = it.getKey()
    repo_name = it.getValue().repo_name
    days_to_keep = it.getValue().days_to_keep.toInteger()
    max_builds_num = it.getValue().max_builds_num.toInteger()
    multibranchPipelineJob(path) {
        branchSources {
            github {
                scanCredentialsId('github')
                repoOwner('azeemsfa')
                repository(repo_name)
                id('123213')
            }
        }
        orphanedItemStrategy {
            discardOldItems {
                daysToKeep(days_to_keep)
                numToKeep(max_builds_num)
            }
        }
    }
}
