import com.e4d.nexus.NexusClient
import com.e4d.nexus.NexusConfig

def call(NexusConfig config) {
  withCredentials([usernamePassword(credentialsId: config.credsId,
      usernameVariable: 'username', passwordVariable: 'password')]) {
    return new NexusClient(this, config.baseUrl, env.username, env.password)
  }
}
