package com.e4d.docker

import com.e4d.shell.Shell
import com.e4d.shell.ShellImpl
import java.security.MessageDigest
import java.time.Duration

/**
 * The implementation of {@link Shell} interface that allows to execute a script
 * inside a docker container either specified or created from a provided image.
 */
class DockerContainerShell extends ShellImpl {
  /**
   * Determines how long the container that is being created will run.
   */
  final Duration lifetime

  /**
   * Determines how long to wait for container start.
   */
  final Duration timeout = Duration.ofSeconds(10)

  /**
   * A set of labels that will be attached to the container is being created.
   */
  final Map<String, String> labels

  /**
   * Username and password for the docker image repository.
   */
  final Map<String, String> registryCreds

  /**
   * A reference to a shell object used to run scripts for managing the
   * container.
   */
  final Shell hostShell

  /**
   * Name of a docker command line tool.
   */
  final String dockerProgram

  /**
   * Directory path in the container being used where the shell will keep
   * scripts.
   */
  final String guestScriptDir

  /**
   * Directory path in the host environment where the shell will keep scripts.
   */
  final String hostScriptDir

  /**
   * Name of an image to create a container from.
   */
  final String image

  /**
   * Specifies kind of a network for the container being created.
   */
  final String network

  /**
   * URL for a docker image registry.
   */
  final String registry

  /**
   * Name of shell command line tool used in a container.
   */
  final String shellProgram

  /**
   * Holds identifier of a container to run scripts on or
   * a container being created from an image. 
   */
  String containerId

  DockerContainerShell(Map options) {
    containerId = options.containerId
    dockerProgram = options.dockerProgram ?: 'docker'
    guestScriptDir = options.guestScriptDir ?: '/tmp'
    hostScriptDir = options.hostScriptDir ?: '.'
    hostShell = options.hostShell
    image = options.image
    labels = options.labels ?: [:]
    lifetime = options.lifetime ?: Duration.ofMinutes(10)
    network = options.network
    registry = options.registry
    registryCreds = options.registryCreds ?: [:]
    shellProgram = options.shellProgram ?: 'bash'
  }

  /**
   * Executes a script.
   *
   * @param args              A map of arbitrary options
   * @param args.script       A String contains script to be executed
   * @param args.returnStdout A boolean value that controls result of the call
   * @param env               A list of environment variables in form of
   *                          <code>'name=value'</code>
   * @return                  Object contains output of the script when
   *                          <code>args.returnStdout</code> is <code>true</code>
   *                          or <code>null</code> otherwise
   */
  def execute(Map args, List env) {
    final scriptName = getScriptName(args.script)
    final scriptGuestPath = [guestScriptDir, scriptName].join('/')
    final scriptHostPath = [hostScriptDir, scriptName].join('/')
    final commandEnvs = env.collect { "--env='${ it }'" }
    final commands = []
    if (!containerId) {
      containerId = getContainerName(image)
      commands.add(commandToLoginToRegistry)
      commands.add(commandToCreateContainer)
    }
    commands.add(commandToStartContainer)
    commands.add(commandToWaitForContainerStart)
    commands.add(commandToUploadFile(scriptHostPath, scriptGuestPath))
    commands.add([
      dockerProgram, 'exec',
      commandEnvs.join(' '), containerId,
      shellProgram, "'${ scriptGuestPath }'"
    ].findAll().join(' '))
    hostShell.writeFile(file: scriptHostPath, text: args.script)
    hostShell.execute(hostEnvs,
      script: commands.findAll().join('\n'),
      returnStdout: args.returnStdout ?: false,
    )
  }

  def readFile(Map args) {
  }

  def writeFile(Map args) {
  }

  /**
   * Downloads a file from the container created for the shell.
   *
   * @param source      A path to a file inside the container
   * @param destination A path to a directory where to copy the file into
   *
   * @return            Path to copied file
   */
  String downloadFile(String source, String destination) {
    final commands = [
      commandToDownloadFile(source, destination)
    ]
    hostShell.execute([],
      script: commands.findAll().join('\n')
    )
    [
      destination,
      source.split('/').last(),
    ].join('/')
  }

  /**
   * Uploads a file to the container created for the shell.
   *
   * @param source      A path to a file
   * @param destination A path to a directory where to copy the file into
   *
   * @return            Path to copied file inside the container
   */
  String uploadFile(String source, String destination) {
    final commands = []
    if (!containerId) {
      containerId = getContainerName(image)
      commands.add(commandToLoginToRegistry)
      commands.add(commandToCreateContainer)
    }
    commands.add(commandToUploadFile(source, destination))
    hostShell.execute([],
      script: commands.findAll().join('\n')
    )
    [
      destination,
      source.split('/').last(),
    ].join('/')
  }

  /**
   * Exits the shell.
   * <p>
   * It kills and removes docker container created by the shell.
   */
  void exit() {
    if (ownsContainer) {
      hostShell.execute([], script: [dockerProgram, 'rm --force', containerId].join(' '))
      containerId = null
    }
  }

  /**
   * Return <code>true</code> when the shell owns the container.
   * <p>
   * It owns the container when the shell has been created with image
   * and no container was specified.
   */
  boolean getOwnsContainer() {
    image && containerId
  }

  /**
   * Returns name for the script that is being executed.
   * <p>
   * The name is used for temporary file contains the script.
   * <p>
   * The name depends on the content of the script.
   *
   * @return String contains name of the script.
   */
  String getScriptName(String script) {
    final digest = MessageDigest.getInstance('MD5').digest(script.bytes).encodeHex()
    [this.class.simpleName, digest].join('-')
  }

  /**
   * Returns name for a container that is being created for the docker image.
   * <p>
   * For the same docker image the name is different for each instance of the shell.
   *
   * @return String contains name of the container.
   */
  String getContainerName(String image) {
    [this.class.simpleName, hashCode()].join('-')
  }

  private String getCommandToCreateContainer() {
    final options = [
      '--rm',
      ['--name', containerId].join('='),
      ['--entrypoint', shellProgram].join('='),
    ]
    if (network) {
      options.add(['--network', network].join('='))
    }
    labels.collect(options) { name, value ->
      "--label='${ name }=${ value }'".toString()
    }
    [
      dockerProgram, 'create',
      options.join(' '),
      image,
      '-c',
      "'sleep ${ lifetime.seconds }'",
    ].join(' ')
  }

  private String getCommandToLoginToRegistry() {
    if (registry) {
      final options = []
      if (registryCreds.username) {
        options.add('--username="${registry_username}"')
      }
      if (registryCreds.password) {
        options.add('--password="${registry_password}"')
      }
      [
        dockerProgram, 'login', options.join(' '), registry
      ].findAll().join(' ')
    }
  }

  private List<String> getHostEnvs() {
    final envs = []
    if (registryCreds.username) {
      envs.add(['registry_username', registryCreds.username].join('='))
    }
    if (registryCreds.password) {
      envs.add(['registry_password', registryCreds.password].join('='))
    }
    envs
  }

  private String getCommandToStartContainer() {
    [dockerProgram, 'start', containerId].join(' ')
  }

  private String getCommandToWaitForContainerStart() {
    """
    stime=\$(date +%s)
    while [ -z "\$(${ dockerProgram } ps --all --quiet --filter='name=${ containerId }' --filter='status=running')" ]
    do
      if [ \$((\$(date +%s) - \${stime})) -ge ${ timeout.seconds } ]
      then
        echo container ${ containerId } has not started within ${ timeout.seconds } seconds
        break
      else
        sleep 0.1
      fi
    done
    """.stripIndent().trim()
  }

  private String commandToDownloadFile(String source, String destination) {
    [
      dockerProgram,
      'cp',
      [containerId, qw(source)].join(':'),
      qw(destination),
    ].join(' ')
  }

  private String commandToUploadFile(String source, String destination) {
    [
      dockerProgram,
      'cp',
      qw(source),
      [containerId, qw(destination)].join(':'),
    ].join(' ')
  }

  private String qw(String original) {
    "'${ original }'"
  }
}
