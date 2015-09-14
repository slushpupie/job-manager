
folder("docker")

def images = ["jenkins-slave", "bind9", "docker-proxy"]

images.each { image ->

  job("docker/${image}") {

    wrappers {
        sshAgent('github')
    }
    label("walnut")

    scm {
      github("slushpupie/docker-${image}",null,"ssh")
    }

    triggers {
      scm("H * * * *")
    }

    steps {
      shell("""
        docker build -t slushpupie/${image} -t localhost:6000/slushpupie/${image} .
        docker push localhost:6000/slushpupie/${image}
      """)
    }
  }
}

job("docker/update") {
  
    wrappers {
       sshAgent('github')
    }
    label("walnut")

    triggers { 
      scm("0 H * * *")
    }

    steps {
        ["jenkins", "ubuntu", "nginx", "elasticsearch", "logstash", "kibana", "larsks/crashplan", "evarga/jenkins-slave", "mhimmer/dropbox", "sameersbn/squid", "bind9"].each { image ->
          shell("""
            docker pull ${image}
          """)
        }
    }
}

