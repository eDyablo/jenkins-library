package com.e4d.job

import hudson.model.ParametersDefinitionProperty
import jenkins.model.Jenkins

/**
 * Runs a job by executing defined set of actions.
 */
class JobRunner {
  private final Job job

  /**
   * Runs a job.
   *
   * @param job the reference to an object that implements Job interface
   */
  static void run(Job job) {
    new JobRunner(job).run()
  }

  /**
   * Creates new runner bound to the specified job.
   *
   * @param job the reference to an object that implements Job interface
   */
  JobRunner(Job job) {
    this.job = job
  }

  /**
   * Returns reference to Jenkins.
   */
  Jenkins getJenkins() {
    Jenkins.instanceOrNull
  }

  /**
   * Runs job specified when the runner has been created.
   */
  void run() {
    runJob()
    updateJobParameters()
  }

  private void runJob() {
    job.loadParameters()
    job.initialize()
    job.run()
  }
  
  private void updateJobParameters() {
    final jobItem = jenkins?.getItemByFullName(job.fullName, hudson.model.Job)
    if (jobItem) {
      final definitions = mergeParameterDefinitions(
        jobItem.getProperty(ParametersDefinitionProperty)?.parameterDefinitions,
        job.parameterDefinitions)
      if (definitions) {
        jobItem.removeProperty(ParametersDefinitionProperty)
        jobItem.addProperty(new ParametersDefinitionProperty(definitions))
        jobItem.save()
      }
    }
  }

  private def mergeParameterDefinitions(first, second) {
    final collector = { map, item -> map.put(item.name, item); map }
    final firstMap = first.inject([:], collector)
    final secondMap = second.inject([:], collector)
    (firstMap + secondMap).values().toList()
  }
}
