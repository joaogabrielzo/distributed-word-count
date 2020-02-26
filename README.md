# Akka Distributed Word Counter

This respository imitates the job of MapReduce in a distributed environment.

### Installation

1 - Clone the repository:
```bash
git clone git@github.com:joaogabrielzo/distributed-word-count.git
```

2 - Navigate into the repository's folder and build the Docker image:
```bash
docker build . -t distributed-word-count
```

3 - Run the Docker container:
```bash
docker run distributed-word-count
```
