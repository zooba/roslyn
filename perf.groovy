// Groovy Script: http://www.groovy-lang.org/syntax.html
// Jenkins DSL: https://github.com/jenkinsci/job-dsl-plugin/wiki

import jobs.generation.*;

// The input project name (e.g. dotnet/corefx)
def projectName = GithubProject
// The input branch name (e.g. master)
def branchName = GithubBranchName
def defaultBranch = "*/${branchName}"

def isPr = false;

def jobName = Utilities.getFullJobName(projectName, "perf_run", isPr)
def myJob = job(jobName) {
    description('perf run')

    steps {
        powerShell("""
            Invoke-WebRequest -Uri http://dotnetci.blob.core.windows.net/roslyn/cpc.zip -OutFile cpc.zip
            [Reflection.Assembly]::LoadWithPartialName('System.IO.Compression.FileSystem') | Out-Null
            If (Test-Path /CPC) {
                Remove-Item -Recurse -Force /CPC
            }
            [IO.Compression.ZipFile]::ExtractToDirectory('cpc.zip', '/CPC/')
            """)
      batchFile(""".\\cibuild.cmd /testPerfRun""")
    }

    publishers {
        postBuildScripts {
            steps {
                powerShell("""
                    # If the test runner crashes and doesn't shut down CPC, CPC could fill
                    # the entire disk with ETL traces.
                    try {
                        taskkill /F /IM CPC.exe | Out-Null
                    } 

                    # Move all etl files to the a folder for archiving
                    mkdir ToArchive
                    mv /CPC/DataBackup* ToArchive

                    # Clean CPC out of the machine
                    If (Test-Path /CPC) {
                        echo "removing /CPC ..."
                        Remove-Item -Recurse -Force /CPC
                        echo "done."
                    }

                    If (Test-Path /CPCTraces) {
                        echo "removing /CPCTraces"
                        Remove-Item -Recurse -Force /CPCTraces
                        echo "done."
                    }
                    If (Test-Path /PerfLogs) {
                        echo "removing /PerfLogs"
                        Remove-Item -Recurse -Force /PerfLogs
                        echo "done."
                    }
                    If (Test-Path /PerfTemp) {
                        echo "removing /PerfTemp"
                        Remove-Item -Recurse -Force /PerfTemp
                        echo "done."
                    }
                    exit 0
                    """)
            }
        }
    }
}

Utilities.addArchival(myJob, "ToArchive")
Utilities.standardJobSetup(myJob, projectName, isPr, defaultBranch)
Utilities.setMachineAffinity(myJob, 'Windows_NT', 'latest-or-auto-elevated')
Utilities.addGithubPushTrigger(myJob)
