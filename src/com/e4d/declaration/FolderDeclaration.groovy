package com.e4d.declaration

import com.cloudbees.hudson.plugins.folder.Folder
import org.jenkinsci.plugins.workflow.job.WorkflowJob

class FolderDeclaration extends AbstractItemDeclaration {
  final Folder folder

  FolderDeclaration(Folder folder) {
    super(folder)
    this.folder = folder
  }

  void folder(String name, Closure definition) {
    final newFolder = folder.allItems(Folder).find {
      it.name == name
    } as Folder ?: folder.createProject(Folder, name) as Folder
    define(FolderDeclaration.newInstance(newFolder), definition)
  }

  void job(String name, Closure definition) {
    final job = folder.allItems(WorkflowJob).find {
      it.name == name
    } as WorkflowJob ?: folder.createProject(WorkflowJob, name) as WorkflowJob
    define(JobDeclaration.newInstance(job), definition)
  }
}
