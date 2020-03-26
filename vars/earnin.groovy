import com.e4d.job.BootstrapJob
import com.e4d.job.SetupServiceJob
import static com.e4d.job.JobRunner.run

void bootstrap(Map options=[:], Closure code) {
  run(new BootstrapJob(this))
}

void setupService(Map options=[:], Closure code) {
  run(new SetupServiceJob(this))
}

return this
