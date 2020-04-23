import com.e4d.job.BootstrapJob
import com.e4d.job.DraftGitRepositoryReleaseJob
import com.e4d.job.FindAndReplaceScriptJob
import com.e4d.job.FindWorkflowJobJob
import com.e4d.job.SetupDockerImageJob
import com.e4d.job.SetupHelmChartJob
import com.e4d.job.SetupNugetJob
import com.e4d.job.SetupServiceJob
import static com.e4d.job.JobRunner.run

void bootstrap(Map options=[:], Closure definition) {
  run(new BootstrapJob(this))
}

void draftGitRepositoryRelease(Map options=[:], Closure definition) {
  run(new DraftGitRepositoryReleaseJob(this))
}

void setupDockerImage(Map options=[:], Closure definition) {
  run(new SetupDockerImageJob(this))
}

void setupHelmChart(Map options=[:], Closure definition) {
  run(new SetupHelmChartJob(this))
}

void setupNuget(Map options=[:], Closure definition) {
  run(new SetupNugetJob(this))
}

void setupService(Map options=[:], Closure definition) {
  run(new SetupServiceJob(this))
}

void findWorkflowJob(Map options=[:], Closure definition) {
  run(new FindWorkflowJobJob(this))
}

void findAndReplaceScript(Map options=[:], Closure definition) {
  run(new FindAndReplaceScriptJob(this))
}

return this
