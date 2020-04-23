package com.e4d.declaration

import com.cloudbees.hudson.plugins.folder.Folder
import jenkins.model.Jenkins
import org.jenkinsci.plugins.workflow.job.WorkflowJob

class JenkinsDeclaration extends Declaration {
  Jenkins getJenkins() {
    Jenkins.get()
  }

  void folder(String name, Closure definition) {
    final folder = jenkins.allItems(Folder).find {
      it.name == name
    } as Folder ?: jenkins.createProject(Folder, name) as Folder
    define(FolderDeclaration.newInstance(folder), definition)
  }

  void job(String name, Closure definition) {
    final job = jenkins.allItems(WorkflowJob).find {
      it.name == name
    } as WorkflowJob ?: parent.createProject(WorkflowJob, name) as WorkflowJob
    define(JobDeclaration.newInstance(job), definition)
  }
}
