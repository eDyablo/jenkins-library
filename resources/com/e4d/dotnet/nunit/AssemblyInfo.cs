using NUnit.Framework;
[assembly: Parallelizable(ParallelScope.Fixtures)]
[assembly:LevelOfParallelism(${ TEST_WORKERS_NUMBER ?: numberOfTestWorkers })]
