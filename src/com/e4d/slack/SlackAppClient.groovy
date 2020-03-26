package com.e4d.slack

import java.net.URL
import java.net.HttpURLConnection

class SlackAppClient {
  String url

  SlackAppClient(String url) {
    this.url = url
  }

  void publish(notification) {
    def text = ''
    if (notification.status == 'success') {
      text = "Job <${ notification.build.url }|${ getPrettyName(notification.job.name) }> is succeeded"
    }
    else {
      text = "Job <${ notification.build.url }|${ getPrettyName(notification.job.name) }> is " +
          "*<${ notification.build.url }console|FAILED>*"
    }
    post("{\"text\":\"${ text }\"}")
  }

  String post(String request) {
    def http = new URL(url).openConnection() as HttpURLConnection
    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("Content-Type", 'application/json')
    http.setRequestProperty("charset", "utf-8")
    http.setRequestProperty("Content-Length", "${ request.length() }")
    http.outputStream.write(request.getBytes("UTF-8"))
    return http.inputStream.getText('UTF-8')
  }

  private def getPrettyName(String text) {
    def tokens = text.tokenize('/')
    if (tokens.size() > 3) {
      tokens[-3..-1].join('/')
    } 
    else {
      text
    }
  }
}
