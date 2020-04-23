package com.e4d.pipeline

/**
 * A dummy pipeline used in tests.
 */
class DummyPipeline {
  def getParams() {
    [:]
  }

  def getEnv() {
    [:]
  }

  def dir(path, Closure code) {
    code()
  }

  def pwd() {
  }

  def checkout(options) {
  }

  def withEnv(envs, Closure code) {
    code()
  }

  def libraryResource(path) {
    path
  }

  def sh(options) {
  }

  def usernamePassword(options) {
  }

  def withCredentials(creds, Closure code) {
    code()
  }

  def stage(name, Closure code) {
    code()
  }

  def fileExists(String path) {
  }

  def readFile(String path) {
  }

  def unstable(String message) {
  }
  
  def echo(String message) {
  }

  def secretVolume(Map options) {
  }

  def envVar(Map options) {
  }

  def secretEnvVar(Map options) {
  }

  def writeFile(Map options) {
  }

  def error(String message) {
  }
  
  def writeYaml(Map options) {
  }

  def build(Map options) {
  }

  def string(Map options) {
  }

  def step(Map options) {
  }
  
  def node(Closure code) {
    code()
  }
}
