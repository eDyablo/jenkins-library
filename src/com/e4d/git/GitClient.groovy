package com.e4d.git

class GitClient {
  def script
  def checkout

  GitClient(script, checkout) {
    this.script = script
    this.checkout = checkout
  }

  def getTags() {
    script.dir(checkout.dir) {
      def output = script.sh(script: 'git tag -l',
        returnStdout: true,
        encoding: 'utf-8').trim()
      return output.split('\n')
    }
  }

  void setTag(String tag) {
    script.dir(checkout.dir) {
      def pushURL = checkout.url.replace('://', "://${checkout.user}:${checkout.password}@")   
      def output = script.sh(script: """
        git config user.email 'ci@e4d.com'
        git config user.name 'Jenkins'
        git tag -a ${tag} -m 'Jenkins: Microservice image version [ci skip] .'
        git push ${pushURL} --tags
      """,
        returnStdout: true,
        encoding: 'utf-8').trim()
      return output.split('\n')
    }
  }
}
