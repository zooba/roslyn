// Groovy Script: http://www.groovy-lang.org/syntax.html
// Jenkins DSL: https://github.com/jenkinsci/job-dsl-plugin/wiki

import jobs.generation.*;

// The input project name (e.g. dotnet/corefx)
def projectName = GithubProject
// The input branch name (e.g. master)
def branchName = GithubBranchName

def isPr = false;
def jobName = Utilities.getFullJobName(projectName, "perf_run", isPr)
def myJob = job(jobName) {
    description('perf run')
    label('windows-roslyn')
    steps {
        batchFile("""cibuild.cmd /release /test64""")
        batchFile("""Binaries\\Release\\Roslyn.Test.Performance.Runner.exe --no-trace-upload""")
    }
}

Utilities.addGithubPushTrigger(myJob)
