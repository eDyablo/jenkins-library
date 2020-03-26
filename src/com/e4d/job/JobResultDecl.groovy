package com.e4d.job

class JobResultDecl extends JobDeclaration {
  def publishers

  JobResultDecl(script, publishers) {
    super(script)
    this.publishers = publishers
  }

  def slack(Map options) {
    def declaration = new SlackPublisherDecl(script, options.url)
    publishers << declaration.createPublisher(options)
  }

  def mail(Map options) {
    def declaration = new MailPublisherDecl(script)
    publishers << declaration.createPublisher(options)
  }

  def leadTimeTracker(Map options) {
    def declaration = new LeadTimeTrackerPublisherDecl(script)
    publishers << declaration.createPublisher(options)
  }

  def getLeadTimeTracker() {
    leadTimeTracker([:])
  }
}
