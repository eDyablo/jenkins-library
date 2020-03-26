package com.e4d.mail

class MailPublisher {
  def pipeline
  String to

  MailPublisher(pipeline) {
    this.pipeline = pipeline
  }

  void publish(notification) {
    pipeline.mail(
      to: to,
      subject: ("Job '${ notification.job.baseName }' is " <<
        "${ notification.status == 'success' ? 'succeeded' : 'failed' }").toString(),
      body: ("Job '${ notification.job.name }' is completed " <<
        "with status '${ notification.status }'.\n" <<
        "Please go to ${ notification.build.url }.").toString()
    )
  }
}
