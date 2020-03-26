package com.e4d.shell

interface Shell {
  def execute(Map args, List env)

  def readFile(Map args)

  def writeFile(Map args)

  void exit()
}
