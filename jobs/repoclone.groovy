import org.kohsuke.github.*

def jobParent = this

DESCRIPTION_FOOTER = """
    '''Do not edit this job through the web.'''
    See the job manager.
""".stripIndent()

parent_dir = ''

String apiKey = "${GHE_TOKEN}"
String apiUrl = "https://api.github.com/"
String dest = "autoclone"

GitHub github = GitHub.connectToEnterprise(apiUrl,apiKey)
destOrg = github.getOrganization(dest)

repos = [
//[ name: 'foo',     // name of the repo and the job
//  repo: 'git@foo', // Source repo; assume Jenkins has creds to get there
//  dest: 'newfoo',  // optional: repo name at new location. Defaults to $name
//  update: true ],  // optional: auto update. Defaults to true
  [ name: 'lvm', repo: 'git@github.com:chef-cookbooks/lvm.git' ],
  [ name: 'lvm', dest: 'lvm2', repo: 'git@github.com:chef-cookbooks/lvm.git' ],
  [ name: 'windows', repo: 'git@github.com:chef-cookbooks/windows.git'],
  [ name: 'jenkins', dest: 'jenkins-cookbook', repo: 'git@github.com:chef-cookbooks/jenkins.git'],
  [ name: 'iptables', repo: 'git@github.com:chef-cookbooks/jenkins.git'],
  [ name: 'logwatch', repo: 'git@github.com:chef-cookbooks/logwatch.git', update: false],
]


folder("${parent_dir}autoclone")

repos.each { repo ->
  dest = repo.get('dest',repo.name)
  update = repo.get('update',true)
  
 jobName = "${parent_dir}autoclone/${repo.name}"

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
      sshAgent 'github'
    }

    scm {
      git {
        remote{
          url(repo.repo)
          credentials('github')
        }
        relativeTargetDir('ignored')
      }
    }

    steps {
      shell("""\
        #!/bin/bash -lx
        rm -rf ignored
        git clone ${repo.repo} ${repo.name}
        cd ${repo.name}
        for branch in `git branch -a | grep remotes | grep -v HEAD | grep -v master`; do
          git branch --track \${branch##*/} \$branch
        done
        git remote add dest ${ghrepo.getSshUrl()}
        git push --all -u dest
        git push --tags -u dest
      """.stripIndent().trim())
    }
  }
}
