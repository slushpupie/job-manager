
folder("docker")

job("docker/jenkins-slave") {
  scm {
    github("slushpupie/docker-jenkins-slave","ssh")
  }

  triggers {
    githubPush()
  }

  steps {
    shell("""
      docker build -t slushpupie/jenkins-slave -t localhost:6000/slushpupie/jenkins-slave docker-jenkins-slave
      docker push localhost:6000/slushpupie/jenkins-slave
    """)
  }

}

job("docker/bind9") {
  scm { 
    github("slushpupie/docker-bind9","ssh")
  }
