import org.kohsuke.github.*

def jobParent = this

DESCRIPTION_FOOTER = """
    '''Do not edit this job through the web.'''
    See the job manager.
""".stripIndent()

baseFolder = 'autoclone'


apiKey = "${GHE_TOKEN}"
apiUrl = "https://api.github.com/"
dest = "autoclone"
defaultCred = 'github' // Credentials Plugin id for remote/upstream
localCred = 'github' // Credentials Plugin id for local/repo


repos = [
//[ name: 'foo',     // name of the repo and the job
//  repo: 'git@foo', // Source repo; assume Jenkins has creds to get there
//  refspec: '+refs/heads/master:refs/remotes/origin/master', // optional: A collection of respecs to push. Defaults to all branches and tags
//  cred: 'foo',     // optional: Credential to use instad of the default
//  dest: 'newfoo',  // optional: repo name at new location. Defaults to $name
//  update: true ],  // optional: auto update. Defaults to true
  [ name: 'lvm', repo: 'git@github.com:chef-cookbooks/lvm.git' ],
  [ name: 'lvm', dest: 'lvm2', repo: 'git@github.com:chef-cookbooks/lvm.git' ],
  [ name: 'windows', repo: 'git@github.com:chef-cookbooks/windows.git'],
  [ name: 'jenkins', dest: 'jenkins-cookbook', repo: 'git@github.com:chef-cookbooks/jenkins.git'],
  [ name: 'iptables', repo: 'git@github.com:chef-cookbooks/iptables.git'],
  [ name: 'rsyslog', repo: 'git@github.com:chef-cookbooks/rsyslog.git', refspec: '+refs/heads/master:refs/heads/master' ],
  [ name: 'logwatch', repo: 'git@github.com:chef-cookbooks/logwatch.git', update: false, cred: 'e79c6312-fd9b-4c60-98d7-82618cc4d2b0'],
  [ name: 'httpd', repo: 'git@github.com:chef-cookbooks/httpd.git', update: false, refspec: ['+refs/tags/v0.3.0:refs/heads/master',
                                                                                             '+refs/tags/v0.3.0:refs/tags/v0.3.0' ,
                                                                                             '+refs/tags/v0.2.19:refs/tags/v0.2.19' ]],
]


GitHub github = GitHub.connectToEnterprise(apiUrl,apiKey)
destOrg = github.getOrganization(dest)

boolean isCollectionOrArray(object) {    
  [Collection, Object[]].any { it.isAssignableFrom(object.getClass()) }
}


folder("${baseFolder}")

repos.each { repo ->
  dest = repo.get('dest',repo.name)
  update = repo.get('update',true)
  cred = repo.get('cred',defaultCred)
  
 jobName = "${baseFolder}/${repo.name}"

  found = false 
  ghrepo = null
  destOrg.listRepositories(100).find { r ->
    println "repo: ${r.getName()}"
    if (r.getName() == repo.name) {
      ghrepo = r
      println "match!" 
      found = true
      return true
    }
    return false
  }

  if (!found) {
      println "Creating ${repo.name}"
      ghrepo = destOrg.createRepository(repo.name, "Cloned from $repo.repo", null, "owners", true)
  }

  job(jobName) {
    description """
      Clone ${repo.name} from ${repo.repo} to ${dest}/${repo.name}
    """.stripIndent().trim() + DESCRIPTION_FOOTER


    if (update) {
      triggers { 
        // Uses hashes, so not all jobs run at the same time
        scm('@daily')      
      }
    }
 
    wrappers {
      if (localCred) {
        sshAgent localCred
      }
    }

    scm {
      git {
        remote{
          url(repo.repo)
          if (cred) {
            credentials(cred)
          }
        }
        relativeTargetDir('ignored')
      }
    }

    steps {
      pushCmd = "git push --all origin\ngit push --tags origin"
      if (repo.refspec) {
        puschCmd = ""
        repo.refspec.each { ref ->
          pushCmd += "git push ${ref}\n"
        }
      }
      shell("""\
        #!/bin/bash -lx
        rm -rf ignored
        git clone --origin upstream ${repo.repo} ${repo.name}
        cd ${repo.name}
        for branch in `git branch -a | grep remotes | grep -v HEAD | grep -v master`; do
          git branch --track \${branch##*/} \$branch
        done
        git remote add origin ${ghrepo.getSshUrl()}
        ${pushCmd}
      """.stripIndent().trim())
    }
  }
}
