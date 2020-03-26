package com.e4d.job

import com.e4d.mail.MailPublisher

class MailPublisherDecl extends JobDeclaration {
  MailPublisherDecl(script) {
    super(script)
  }

  def createPublisher(Map options) {
    def publisher = new MailPublisher(script)
    publisher.to = options.to
    return publisher
  }
}
