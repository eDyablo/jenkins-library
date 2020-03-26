import com.e4d.slack.SlackAppClient

def call(Map kwargs) {
  withCredentials([string(credentialsId: kwargs.credsId, variable: 'url')]) {
    return new SlackAppClient(env.url)
  }
}
