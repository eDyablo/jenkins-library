import com.e4d.mysql.MySqlClient

def call(Map kwargs) {
  withCredentials([usernamePassword(credentialsId: kwargs.credsId,
    usernameVariable: 'username', passwordVariable: 'password')]) {
    return new MySqlClient(this, kwargs.host, env.username, env.password)
  }
}
