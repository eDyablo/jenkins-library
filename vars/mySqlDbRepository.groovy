import com.e4d.mysql.MySqlDbRepository

def call(Map kwargs) {
  def client = kwargs.client ?: mySqlClient(kwargs)
  return new MySqlDbRepository(this, client)
}
