FROM openjdk:8

RUN \
  curl -L -o sbt-1.3.8.deb http://dl.bintray.com/sbt/debian/sbt-1.3.8.deb && \
  dpkg -i sbt-1.3.8.deb && \
  rm sbt-1.3.8.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt sbtVersion

WORKDIR /app

ADD . /app

CMD sbt run