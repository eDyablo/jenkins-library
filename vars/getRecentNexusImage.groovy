import com.e4d.nexus.NexusClient
import com.e4d.service.ServiceImageRepository

def call(Map kwargs) {
  def nexusBaseUrl = kwargs.nexusBaseUrl ?: params.NEXUS_BASE_URL
  def nexusCredsId = kwargs.nexusCredsId ?: params.NEXUS_CREDS_ID
  withCredentials([usernamePassword(credentialsId: nexusCredsId,
  usernameVariable: 'username', passwordVariable: 'password')]) {
    def nexus = new NexusClient(this, nexusBaseUrl, env.username, env.password)
    def repository = new ServiceImageRepository(nexus)
    repository.getRecentImage(kwargs.serviceName)
  }
}
