FROM ubuntu:14.04

RUN \
  apt-get update && apt-get install -y --no-install-recommends software-properties-common \
  && add-apt-repository ppa:webupd8team/java \
  && gpg --keyserver hkp://keys.gnupg.net --recv-keys 409B6B1796C275462A1703113804BB82D39DC0E3 \
  && apt-get update \
  && echo debconf shared/accepted-oracle-license-v1-1 select true |  debconf-set-selections \
  && echo debconf shared/accepted-oracle-license-v1-1 seen true |  debconf-set-selections \
  && apt-get install -y --no-install-recommends oracle-java8-installer

  EXPOSE 18000

  ADD target/restfire-*.jar restfire.jar
  ADD target/config/local.yml config.yml

  CMD sh -c "java -jar -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl -Xms${JAVA_PROCESS_MIN_HEAP} -Xmx${JAVA_PROCESS_MAX_HEAP} -XX:+UseG1GC restfire.jar server config.yml"