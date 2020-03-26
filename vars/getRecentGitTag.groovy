import com.e4d.git.GitClient
import com.e4d.service.ServiceSourceRepository

def call(Map kwargs) {
  def checkout = kwargs.checkout
  def client = new GitClient(this, checkout)
  def repository = new ServiceSourceRepository(client)
  return repository.getRecentTag()
}
