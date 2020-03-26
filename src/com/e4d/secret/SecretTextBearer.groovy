package com.e4d.secret

class SecretTextBearer implements PipelineSecret {
  final String secretId

  SecretTextBearer(String secretId) {
    this.secretId = secretId
  }

  def declassify(pipeline) {
    final creds = [
      pipeline.string(credentialsId: secretId, variable: 'text')
    ]
    pipeline.withCredentials(creds) {
      pipeline.env.text
    }
  }
}
