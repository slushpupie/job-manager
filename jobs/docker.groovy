
folder("docker")

def images = ["jenkins-slave", "bind9", "docker-proxy"]

images.each {

  job("docker/${it}") {
    scm {
      github("slushpupie/docker-${it}",null,"ssh")
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

