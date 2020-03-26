package com.e4d.build

import com.cloudbees.groovy.cps.NonCPS

class CommandLineBuilder {
  def String argumentSeparator = ' '
  def String optionPrefix = '--'
  def String shortOptionPrefix = '-'
  def String optionKeyValueSeparator = ' '
  def String switchPrefix = '--'
  def String shortSwitchPrefix = '-'

  final def hasValue = { it.value }
  final def notNullOrEmpty = { it }
  final def isSpace = { it == ' ' }
  final def isSwitch = { it?.value instanceof Boolean }
  final def isOption = { it?.value instanceof Boolean == false }
  final def isOn = { it?.value == true }

  String buildCommand(List verbs, List arguments = [], Map options = [:])
  {
    buildCommand(verbs, arguments,
      options.findAll(isOption),
      options.findAll(isSwitch).findAll(isOn).collect {
        "$it.key"
      }
    )
  }

  String buildCommand(Map options, List verbs, List arguments) {
    buildCommand(verbs, arguments, options)
  }

  String buildCommand(List verbs, List arguments, Map options, List switches) {
    [
      verbs.collect {
        buildArgument(it)
      }.join(argumentSeparator),
      options.findAll(hasValue).collect {
        buildOption(it)
      }.join(argumentSeparator),
      switches.findAll(notNullOrEmpty).collect {
        buildSwitch(it)
      }.join(argumentSeparator),
      arguments.collect {
        buildArgument(it)
      }.join(argumentSeparator)
    ].findAll(notNullOrEmpty)
    .join(argumentSeparator)
  }

  String buildArgument(argument) {
    argument.any { it == ' ' }
      ? "'${ argument }'"
      : argument
  }
  
  String buildSwitch(aSwitch) {
    aSwitch.length() > 1
      ? "$switchPrefix${ aSwitch.replace('_', '-') }"
      : "$shortSwitchPrefix$aSwitch"
  }
  
  String buildOption(option) {
    def type = option.value.getClass()
    if ([Collection, Object[]].any { it.isAssignableFrom(type) }) {
      buildMultiOption(option)
    }
    else {
      buildSingleOption(option)
    }
  }
  
  String buildSingleOption(option) {
    [
      buildOptionKey(option.key),
      buildArgument(option.value)
    ].join(optionKeyValueSeparator)
  }

  String buildMultiOption(option) {
    option.value.collect {
      buildSingleOption([key: option.key, value: it])
    }.join(argumentSeparator)
  }
  
  String buildOptionKey(key) {
    key.length() > 1
      ? "$optionPrefix${ key.replace('_', '-') }"
      : "$shortOptionPrefix$key"
  }
}
