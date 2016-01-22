# Edward

The project is a part of my master's thesis: Volunteer-computing platform utilising web browsers. Work is still in progress and the project is not finished yet.

## Description 

Edward is a volunteer-computing platform. It connects people that need computing resources to process data with volunteers that want to commit unused computing power of their computers.
Jobs for volunteers need to be provided as javascript functions that take single argument and return single result. Task is a JSON object that will be provided as an argument to that function. 
The result will be computed using volunteer's computing power. Tasks and jobs can be added using the webapplication or by HTTP API for which Java wrapper library (`RestClient`) is provided. 

User can become a vounteer by simply opening volunteer page in his browser. The computation will be done in the background using webworkers. Volunteer script has been tested with modern Firefox and Chrome non-mobile browsers. 

UI is written in react.js but it was my first attempt to use react and the code is a real mess at the moment. Shell client is not up to date and currently doesn't work. The platform itself should work as well as volunteer code. Tagged released versions from git are the most stable and tested. 


## Building Edward 
```bash
git clone https://github.com/greenjoe/edward.git
cd edward
mvn package
```
The executable JAR file will be generated in edward/edward-communication/edward-executable-${VERSION}.jar.

## Starting Edward 

Starting with default options is really simple:
```bash
java -jar edward-executable-VERSION.jar
```
It creates a file-based H2 database in the current working directory and starts the web server at http://localhost:8008. 
Configuration can be passed using Java properties, for example to change the database file location to /tmp/edward.mv.db:
```bash
java -Djdbc.url="jdbc:h2:/tmp/edward" -jar edward-communication/target/edward-executable-${VERSION}.jar 
```
Full list of configurable properties can be found in the
[Parameter.java](edward-core/src/main/java/pl/joegreen/edward/core/configuration/Parameter.java) class. 

### Building and running on a fresh install of Amazon EC2 Ubuntu

```bash
export VERSION=0.7-SNAPSHOT
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
sudo add-apt-repository -y ppa:webupd8team/java
sudo apt-get update
sudo apt-get -y install oracle-java8-installer
sudo apt-get -y install maven git
sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
cd ~
git clone https://github.com/greenjoe/edward.git
cd edward
mvn package
java -jar edward-communication/target/edward-executable-${VERSION}.jar
```
