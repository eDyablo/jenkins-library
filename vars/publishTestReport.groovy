def call(testResults) {
  step([$class: 'MSTestPublisher',
    testResultsFile: testResults.filePattern,
    failOnError: true, keepLongStdio: true])
}
