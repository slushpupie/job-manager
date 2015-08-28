
folder("docker")

def images = ["jenkins-slave", "bind9", "docker-proxy"]

images.each {

  job("docker/jenkins-slave") {
    scm {
      github("slushpupie/docker-${it}","ssh")
    }

    triggers {
      scm("H * * * *")
    }

    steps {
      shell("""
        docker build -t slushpupie/${it} -t localhost:6000/slushpupie/${it} ${it}
        docker push localhost:6000/slushpupie/${it}
      """)
    }
  }
}

