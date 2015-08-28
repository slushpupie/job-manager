
folder("docker")

def images = ["jenkins-slave", "bind9", "docker-proxy"]

images.each { image ->

  job("docker/${image}") {

    wrappers {
        sshAgent('github')
    }

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

