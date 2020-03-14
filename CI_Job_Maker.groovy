pipelineJob('Shared/CIJobMaker') {
    description('Automatically creates a PR for a CI Job in the CloudOps repository.')
    logRotator {
        numToKeep(10)
        artifactNumToKeep(1)
        daysToKeep(10)
        artifactDaysToKeep(10)
    }
    parameters {
        stringParam(
            'JobPath',
            '',
            'Full path of CI job, usually follows $TeamName/$RepoName/Build. Ex. DataScience/BlankSpace/Build')
        stringParam(
            'RepositoryName',
            '',
            'Name of the repository in CBDR. Ex. BlankSpace')
        stringParam(
            'DaysToKeep',
            '5',
            'Days to keep deleted branches')
        stringParam(
            'MaxBuildsNum',
            '10',
            'Max Number of builds to keep for active branches')
    }
    definition {
        cpsScm {
            scm{
                git {
                    remote {
                        github('azeemsfa/jenkins_automation', 'https')
                        credentials('github')
                        branches('master')
                    }
                }
            }
            scriptPath('Jenkins/Pipelines/Shared/CI_Job_Maker.Jenkinsfile')
        }
    }
}
