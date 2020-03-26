package com.e4d.curl

import com.e4d.file.FileHub
import com.e4d.shell.Shell

class CurlFileHub implements FileHub {
  final Shell shell

  CurlFileHub(Shell shell) {
    this.shell = shell
  }

  void uploadFile(Map options) {
    options = checkedOptions(options)
    final args = ['--fail']
    final envs = []
    args << '--upload-file' << options.file
    if (options.user && options.password) {
      envs << "user=${ [options.user, options.password].join(':') }".toString()
      args << '--user' << '"${user}"'
    }
    args << options.destination
    shell.execute(envs, script: "curl ${ args.join(' ') }".toString())
  }

  def checkedOptions(options) {
    if (!options?.file?.trim()) {
      throw new Exception('File is not specified')
    }
    return options
  }

  void downloadFile(Map options) {
    options = checkedOptions(options)
    final args = ['--fail']
    final envs = []
    if (options.user && options.password) {
      envs << "user=${ [options.user, options.password].join(':') }".toString()
      args << '--user' << '"${user}"'
    }
    if (options.destination?.trim()) {
      args << '--output' << options.destination
    }
    args << options.file
    shell.execute(envs, script: "curl ${ args.join(' ') }".toString())
  }
}
