
folder("docker")

def images = ["jenkins-slave", "bind9", "docker-proxy", "artifactory"]

images.each { image ->

  job("docker/${image}") {

    wrappers {
        sshAgent('github')
    }
    label("walnut")

    scm {
      git {
        remote {
          github("slushpupie/docker-${image}","ssh")
          credentials('github')
        }
      }
    }

    triggers {
      githubPush()
    }

    steps {
      shell("""
        docker build -t slushpupie/${image} -t localhost:6000/slushpupie/${image} .
        docker push localhost:6000/slushpupie/${image}
      """)
    }

    publishers{
      mailer('jay@slushpupie.com', true, true)
    }
  }
}


folder('docker/updates')

["jenkins", "ubuntu", "nginx", "elasticsearch", "logstash", "kibana",
 "larsks/crashplan", "evarga/jenkins-slave", "mhimmer/dropbox",
 "sameersbn/squid", "andyshinn/dnsmasq", "progrium/consul", "cgswong/vault",
 "tomcat:7-jre7", "tomcat:7-jre8","jenkinsci/jenkins"].each { image ->

   imagename = image
   parts = image.split('/')
   if (parts.length == 1) {
     repo = 'library'
     parts2 = imagename.split(':')
     imagename = parts2[0]
     jobname = "docker/updates/${imagename}"
   } else if (parts.length == 2) {
     repo = parts[0]
     imagename = parts[1]
     parts2 = imagename.split(':')
     imagename = parts2[0]
     jobname = "docker/updates/${repo}-${imagename}"
   }

   trigger_url = "https://index.docker.io/v1/repositories/${repo}/${imagename}/tags"
   job(jobname) {
     label("walnut")
     triggers {
       urlTrigger {
         cron("H * * * *")
         url(trigger_url) {
           inspection('change')
           timeout(4000)
         }
       }
     }
     steps {
       shell("""
         docker pull ${image}
      """)
     }
     publishers{
       mailer('jay@slushpupie.com', true, true)
     }
   }
}
