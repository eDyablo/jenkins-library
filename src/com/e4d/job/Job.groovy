package com.e4d.job

/**
 * Defines interface of a job.
 *
 * @see JobRunner
 */
interface Job {
  /**
   * Returns full name of the job.
   *
   * @return  the string contains the full name
   */
  String getFullName()

  /**
   * Returns parameter definitions for the job.
   *
   * @return the list of parameter definition objects.
   * @see <a href="https://javadoc.jenkins.io/hudson/model/ParameterDefinition.html">hudson.model.ParameterDefinition</a>
   */
  def getParameterDefinitions()

  /**
   * Gets called by the job runner to load parameters into the job.
   */
  void loadParameters()

  /**
   * Gets called by the job runner to initialize the job.
   */
  void initialize()

  /**
   * Contains job's logic.
   * Gets called by the job runner.
   */
  void run()
}
