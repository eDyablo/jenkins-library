package com.e4d.track

import java.net.URL
import java.net.HttpURLConnection

class LeadTimeTrackerPublisher {
  final String url
  final String service

  final static String defaultUrl = 'http://svchub.k8s.us-west-2.dev.e4d.com/deployment'

  LeadTimeTrackerPublisher(Map options=[:]) {
    url = options.url ?: defaultUrl
    service = options.service
  }

  void publish(notification) {
    post(notification.build.url)
  }

  String post(String request) {
    def http = new URL("${ url }/${ service }").openConnection() as HttpURLConnection
    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("Content-Type", 'application/json')
    http.setRequestProperty("charset", "utf-8")
    http.setRequestProperty("Content-Length", "${ request.length() }")
    http.outputStream.write(request.getBytes("UTF-8"))
    return http.inputStream.getText('UTF-8')
  }
}
