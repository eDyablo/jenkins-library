package com.e4d.build

public void defaultTemplate(Map options, Closure body) {
  podTemplate(
    cloud: "kubernetes",
    label: options?.podLabel ?: 'default',
    annotations:  [
      podAnnotation(key: "iam.amazonaws.com/role", value: "RoleCiSlave")
    ],
    namespace: 'jenkins',
    imagePullSecrets: [ 'regsecret' ],
    nodeUsageMode: 'EXCLUSIVE',
    nodeSelector: 'nodetype=slave',
    containers: [
      containerTemplate(
        name: 'jnlp',
        image: 'artifacts.k8s.us-west-2.dev.e4d.com:8082/jnlp-slave:v1.5.0',
        args: '${computer.jnlpmac} ${computer.name}',
        ttyEnabled: true,
        privileged: false,
        resourceRequestCpu: '0.25',
        resourceLimitCpu: '0.25',
        resourceRequestMemory: '400Mi',
        resourceLimitMemory: '400Mi',
      )
    ],
    envVars: options.envVars +
      envVar(key: 'JNLP_PROTOCOL_OPTS',
        value: '-Dorg.jenkinsci.remoting.engine.JnlpProtocol3.disabled=true'),
    volumes: [
      hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
    ] + options.volumes
    ) {
    body()
  }
}

public void clientgenTemplate(Map options, Closure body) {
  podTemplate(
    cloud: "kubernetes",
    label: options?.podLabel ?: 'clientgen',
    annotations:  [
      podAnnotation(key: "iam.amazonaws.com/role", value: "RoleCiSlave")
    ],
    namespace: 'jenkins',
    imagePullSecrets: [ 'regsecret' ],
    nodeUsageMode: 'EXCLUSIVE',
    nodeSelector: 'nodetype=slave',
    containers: [
      containerTemplate(
        name: 'jnlp',
        image: 'artifacts.k8s.us-west-2.dev.e4d.com:8082/clientgen-slave:0.1.1',
        args: '${computer.jnlpmac} ${computer.name}',
        ttyEnabled: true,
        privileged: false,
        resourceRequestCpu: '0.25',
        resourceLimitCpu: '0.25',
        resourceRequestMemory: '400Mi',
        resourceLimitMemory: '400Mi',
      )
    ],
    envVars: options.envVars,
    volumes: [
      hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
    ]
    ) {
    body()
  }
}

public void dockerTemplate(Map options, Closure body) {
  podTemplate(
    cloud: "kubernetes",
    label: options?.podLabel ?: 'docker',
    annotations:  [
      podAnnotation(key: "iam.amazonaws.com/role", value: "RoleCiSlave")
    ],
    namespace: 'jenkins',
    imagePullSecrets: [ 'regsecret' ],
    nodeUsageMode: 'EXCLUSIVE',
    nodeSelector: 'nodetype=slave',
    containers: [
      containerTemplate(
        name: 'jnlp',
        image: 'docker:stable-dind',
        ttyEnabled: true,
        privileged: true,
        resourceRequestCpu: '0.25',
        resourceLimitCpu: '0.25',
        resourceRequestMemory: '400Mi',
        resourceLimitMemory: '400Mi',
      )
    ],
    envVars: options.envVars,
    volumes: [
      hostPathVolume(hostPath: '/etc/docker/daemon.json', mountPath: '/etc/docker/daemon.json'),
      hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
    ]
    ) {
    body()
  }
}

public void locustTemplate(Map options, Closure body) {
  podTemplate(
    cloud: "kubernetes",
    label: options?.podLabel ?: 'locust',
    annotations:  [
      podAnnotation(key: "iam.amazonaws.com/role", value: "RoleCiSlave")
    ],
    namespace: 'default',
    imagePullSecrets: [ 'regsecret' ],
    nodeUsageMode: 'EXCLUSIVE',
    nodeSelector: 'nodetype=slave',
    containers: [
      containerTemplate(
        name: 'jnlp',
        image: 'artifacts.k8s.us-west-2.dev.e4d.com:8082/locust-jnlp-slave:latest',
        args: '${computer.jnlpmac} ${computer.name}',
        ttyEnabled: true,
        privileged: false,
        resourceRequestCpu: '0.5',
        resourceLimitCpu: '0.5',
        resourceRequestMemory: '2.5Gi',
        resourceLimitMemory: '2.5Gi',
        //command: '/usr/bin/env/sh -c'
      )
    ],
    envVars: options.envVars,
    volumes: [
      hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
    ]
  ) {
    body()
  }
}

public void systemtestTemplate(Map options, Closure body) {
  podTemplate(
    cloud: "kubernetes",
    label: options?.podLabel ?: 'systemtest',
    annotations:  [
      podAnnotation(key: "iam.amazonaws.com/role", value: "RoleCiSlave")
    ],
    namespace: 'default',
    imagePullSecrets: [ 'regsecret' ],
    nodeUsageMode: 'EXCLUSIVE',
    nodeSelector: 'nodetype=slave',
    containers: [
      containerTemplate(
        name: 'jnlp',
        image: 'artifacts.k8s.us-west-2.dev.e4d.com:8082/locust-jnlp-slave:latest',
        args: '${computer.jnlpmac} ${computer.name}',
        ttyEnabled: true,
        privileged: false,
        resourceRequestCpu: '0.40',
        resourceLimitCpu: '0.40',
        resourceRequestMemory: '3Gi',
        resourceLimitMemory: '3Gi',
        //command: '/usr/bin/env/sh -c'
      )
    ],
    envVars: options.envVars,
    volumes: [
      hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')
    ]
  ) {
    body()
  }
}

return this
