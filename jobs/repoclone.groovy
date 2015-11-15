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
//[ repo: remoteRepo, ref: remoteRef, dst: localDest ]
  [ name: 'lvm', repo: 'git@github.com:chef-cookbooks/lvm.git', tags: ['v1.3.7','v1.3.6'] ],
  [ name: 'windows', repo: 'git@github.com:chef-cookbooks/windows.git'],
]


folder("${parent_dir}autoclone")

repos.each { repo ->
  branches = repo.get('branches',['master'])
  tags = repo.get('tags',[])
  tags.each { tag ->
    if (branches.size == 1 && branches[0] == 'master') {
      branches = [ "tags/${tag}" ]
    } else {
      branches = branches + [ "tags/${tag}" ]
    }
  }

  branches.each { branch ->
    jobName = "${parent_dir}autoclone/${repo.name}-branch-${branch}"
    if (branch.startsWith('tags/')) {
      jobName = "${parent_dir}autoclone/${repo.name}-tag-${branch.substring(5)}"
    }

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

      wrappers {
        sshAgent 'github'
      }

      scm {
        git {
          remote{
            name('upstream')
            url(repo.repo)
            credentials('github')
          }
          branches(branch)
          localBranch(branch)
        }
      }

      steps {
        shell("""\
          #!/bin/bash -lx
          git remote add origin ${ghrepo.getSshUrl()}
          git push -u origin ${branch}
        """.stripIndent().trim())
      }
    }
  }
}
