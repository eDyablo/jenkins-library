package com.e4d.job

import com.e4d.track.LeadTimeTrackerPublisher

class LeadTimeTrackerPublisherDecl extends JobDeclaration {
  LeadTimeTrackerPublisherDecl(pipeline) {
    super(pipeline)
  }

  def createPublisher(Map options) {
    new LeadTimeTrackerPublisher(options)
  }
}
