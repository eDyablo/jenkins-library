package com.e4d.jira

class JiraTool {
  String site
  def pipeline

  JiraTool(pipeline, String site='e4d JIRA') {
    this.site = site
    this.pipeline = pipeline
  }

  // creates a Jira Issue after a successful deployment.
  def createIssue(String jiraKey,
           String reasonForDeploy,
           String deployTarget,
           String deployEnv,
           String releaseVersion) {

    // Only deploy on production environment.
    if (! (deployEnv.toLowerCase().contains("prod".toLowerCase())) ){
      return false
    }

    // Current job information.
    def (userName, userID, jobName, buildNumber) = ['', '', pipeline.env.JOB_NAME, pipeline.env.BUILD_NUMBER]

    // Started by this user.
    pipeline.wrap([$class: 'BuildUser']) {
      userName = pipeline.env.BUILD_USER
      userID = pipeline.env.BUILD_USER_ID
    }

    // Create a new Jira issue.
    final jiraSummary = jobName + ' - ' + userName + ' - ' + deployTarget + ' - Build: # ' + buildNumber
    final jiraFields = [ project: [key: jiraKey],
                   summary: jiraSummary,
                   description: 'Jira deployment notification.',
                   customfield_10226: jobName,
                   customfield_10225: buildNumber,
                   customfield_10227: reasonForDeploy,
                   customfield_10228: deployTarget,
                   customfield_10229: deployEnv,
                   customfield_10232: userID,
                   customfield_10233: releaseVersion,
                   issuetype: [id: '10001']]

    def response = pipeline.jiraNewIssue issue: [fields: jiraFields], site: site
    pipeline.echo "Jira issue created: " + response.successful.toString()
    pipeline.echo "Jira issue information: " + response.data.toString()

    return true
  }

}
