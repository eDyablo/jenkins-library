package com.e4d.job

import groovy.json.JsonSlurperClassic
import com.cloudbees.groovy.cps.NonCPS

class LoadTestingJob extends PipelineJob {
  Boolean generateJobParameters = true
  def targets = []
  int qps = 1
  int p50Limit = 10
  int p95Limit = 50
  int p99Limit = 200
  int concurrency = 1
  int totalRequests = 100
  String interval = '10s'
  String timeout = '10s'
  boolean compress = false
  boolean noreuse = false
  boolean noLatencySummary = false
  String method = 'GET'
  def headers = []
  String data = ''
  Boolean reportOnly = false

  final String targetsFile = 'targets'

  final def httpMethods = ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'COPY',
    'HEAD', 'OPTIONS', 'LINK', 'UNLINK', 'PURGE', 'LOCK', 'UNLOCK',
    'PROPFIND', 'VIEW']

  LoadTestingJob(def pipeline) {
    super(pipeline)
  }

  @NonCPS
  void setTarget(String value) {
    targets = value.split('\n').collect{ it.trim() }.findAll{ it }
  }

  def defineParameters() {
    if (generateJobParameters) {
      return [
        pipeline.text(name: 'target', defaultValue: targets.join('\n'),
          description: 'URL(s)'),
        pipeline.string(name: 'qps', defaultValue: "$qps",
          description: 'query per second to send to backends per request thread'),
        pipeline.booleanParam(name: 'report only', defaultValue: reportOnly,
          description: 'don\'t check required limits'),
        pipeline.string(name: 'p50Limit', defaultValue: "$p50Limit",
          description: 'required p50 minimal response time'),
        pipeline.string(name: 'p95Limit', defaultValue: "$p95Limit",
          description: 'required p95 minimal response time'),
        pipeline.string(name: 'p99Limit', defaultValue: "$p99Limit",
          description: 'required p99 minimal response time'),
        pipeline.choice(name: 'method', defaultValue: method,
          choices: ([method.toUpperCase()] + (httpMethods - method.toUpperCase())).findAll{it}.join('\n'),
          description: 'HTTP method to use'),
        pipeline.text(name: 'header', defaultValue: headers.join('\n'), description: 'HTTP request header'),
        pipeline.text(name: 'data', defaultValue: data, description: 'HTTP request data'),
        pipeline.string(name: 'timeout', defaultValue: timeout,
          description: 'individual request timeout'),
        pipeline.string(name: 'concurrency', defaultValue: "$concurrency",
          description: 'number of request threads'),
        pipeline.string(name: 'totalRequests', defaultValue: "$totalRequests",
          description: 'total number of requests to send before exiting'),
        pipeline.string(name: 'interval', defaultValue: interval,
          description: 'reporting interval'),
        pipeline.booleanParam(name: 'compress', defaultValue: compress,
          description: 'use compression'),
        pipeline.booleanParam(name: 'noreuse', defaultValue: noreuse,
          description: 'don\'t reuse connections'),
        pipeline.booleanParam(name: 'noLatencySummary', defaultValue: noLatencySummary,
          description: 'suppress the final latency summary'),
        ]
    }
    else
      return []
  }

  def loadParameters(def params) {
    targets = params.target?.split('\n')?.findAll{ it } ?: targets
    qps = Integer.parseInt(params.qps ?: "$qps")
    reportOnly = params.'report only' ?: reportOnly
    p50Limit = Integer.parseInt(params.p50Limit ?: "$p50Limit")
    p95Limit = Integer.parseInt(params.p50Limit ?: "$p95Limit")
    p99Limit = Integer.parseInt(params.p50Limit ?: "$p99Limit")
    concurrency = Integer.parseInt(params.concurrency ?: "$concurrency")
    totalRequests = Integer.parseInt(params.totalRequests ?: "$totalRequests")
    interval = params.interval?.trim() ?: interval
    timeout = params.timeout?.trim() ?: timeout
    compress = params.compress ?: compress
    noreuse = params.noreuse ?: noreuse
    noLatencySummary = params.noLatencySummary ?: noLatencySummary
    method = params.method ?: method
    headers = params.header?.split('\n')?.findAll{ it } ?: headers
    data = params.data ?: data
  }
  
  def runSlowCooker(qps, concurrency, interval, totalRequests, timeout, method) {
    return pipeline.sh(script: """\
        #!/usr/bin/env bash
        docker run --rm \
          --network=host \
          --entrypoint "/bin/sh" \
          buoyantio/slow_cooker -c \
          \"\
          echo \'${ targets.join('\n') }\' > ${ targetsFile } \
          && /slow_cooker/slow_cooker \
          -qps $qps \
          -concurrency $concurrency \
          -interval $interval \
          -totalRequests $totalRequests \
          -timeout $timeout \
          -method $method \
          -data \'${ data.stripIndent().replace('\n', '') }\' \
          ${ headers.collect { ['--header', ['"',it,'"'].join('')].join(' ') }.join(' ') } \
          ${ compress ? '-compress' : '' } \
          ${ noreuse ? '-noreuse' : '' } \
          ${ noLatencySummary ? '-noLatencySummary' : '' } \
          \'@${ targetsFile }\' \
          \"
        """.stripIndent().trim(),
        returnStdout: true)
  }

  def run() {
    pipeline.writeFile(
      file: targetsFile,
      encoding: 'utf-8',
      text: targets.join('\n'))
    pipeline.stage('load') {
      final warmUpConcurrency = concurrency == 1 ? 1 : (concurrency / 2).toInteger()
      final warmUpTotalRequests = totalRequests > 2000 ? 1000 : (totalRequests / 2).toInteger()
    
      runSlowCooker(qps, warmUpConcurrency, interval, warmUpTotalRequests, timeout, method)

      final output = runSlowCooker(qps, concurrency, interval, totalRequests, timeout, method)

      pipeline.echo(output)

      if (reportOnly == false) {
        checkExpectations(extractStats(output))
      }
    }
  }
  
  @NonCPS
  def extractStats(text) {
    def lines = text.split('\n')
    def statLines = lines.dropWhile {
      !it.startsWith('{')
    }
    new JsonSlurperClassic().parseText(statLines.join(''))
  }

  void checkExpectations(Map stats) {
    if (stats.p50 > p50Limit || stats.p95 > p95Limit || stats.p99 > p99Limit) {
      pipeline.error('Load tests result does not correspond to required response time. Please take a look into the job log.')
    }
  }
}
