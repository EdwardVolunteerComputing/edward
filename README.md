# Edward

The project is a part of my master's thesis: Volunteer-computing platform utilising web browsers. Work is still in progress and the project is not finished yet.

## Description 

Edward is a volunteer-computing platform. It connects people that need computing resources to process data with volunteers that want to commit unused computing power of their computers.
Jobs for volunteers need to be provided as javascript functions that take single argument and return single result. Task is a JSON object that will be provided as an argument to that function. 
The result will be computed using volunteer's computing power. Tasks and jobs can be added using the webapplication or by HTTP API for which Java wrapper library (`RestClient`) is provided. 

User can become a vounteer by simply opening volunteer page in his browser. The computation will be done in the background using webworkers. Volunteer script has been tested with modern Firefox and Chrome non-mobile browsers. 

UI is written in react.js but it was my first attempt to use react and the code is a real mess at the moment. Shell client is not up to date and currently doesn't work. The platform itself should work as well as volunteer code. Tagged released versions from git are the most stable and tested. 


## Starting Edward 

Distribution zip is generated in `edward-communication/target` and contains:
* `edward.war` - webapplication archive that can be used in Tomcat 
* `edward.mv.db` - H2 database file with initialized schema
* `createSchema.sql` - SQL script that generates database schema
* `defaultConfig.propertie` - properties file with default values 

By default edward tries to connect to the H2 database using url: `jdbc:h2:tcp://localhost/~/edward` 
with `admin/admin` credentials. That's why scripts below use user home directory to store database file with created schema. The url and credentials can be changed by setting `edward.config` java environment variable to the location of properties file with new values. Exemplary configuration file as well as a database file with initialized schema is provided in the distribution zip. To change environment variable in Tomcat `edward.config=pathToFile` should be added to
`$TOMCAT_DIR/conf/catalina.properties`. 


### Windows + Jetty 

Running on Windows using embedded jetty. Assuming java8, maven, git are installed and available in `%PATH%` . Maven should be configured to use java8. H2 console (http://www.h2database.com/html/download.html) needs to be running as it is used as the database server. 
```
git clone https://github.com/greenjoe/edward.git
cd edward
mvn install -Dmaven.test.skip
COPY edward-persistence\target\schemaForJooq.mv.db %userprofile%\edward.mv.db 
mvn jetty:run -pl edward-communication
```
Server webpage address: `http://localhost:8080/` 



### Ubuntu + Jetty 
Running on ubuntu using embedded jetty. Assuming java8, maven, git are installed and available in `$PATH`. Maven should be configured to use java8 (`$JAVA_HOME`).

```bash
cd ~
git clone https://github.com/greenjoe/edward.git
wget http://www.h2database.com/h2-2015-04-10.zip
unzip h2-2015-04-10.zip
chmod +x h2/bin/h2.sh
~/h2/bin/h2.sh &
cd edward 
mvn package -D maven.test.skip
cp edward-persistence/target/schemaForJooq.mv.db ~/edward.mv.db
mvn jetty:run -pl edward-communication
```
Server webpage address: `http://localhost:8080/` 



### Ubuntu (fresh install) + Tomcat 

Script to run the platform using Tomcat on a fresh ubuntu instance from Amazon: 
```bash
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
sudo add-apt-repository -y ppa:webupd8team/java
sudo apt-get update
sudo apt-get -y install oracle-java8-installer
sudo apt-get -y install maven tomcat7 git
echo "JAVA_HOME=/usr/lib/jvm/java-8-oracle/" >> tomcat7
cat /etc/default/tomcat7 >> tomcat7
sudo mv tomcat7 /etc/default/tomcat7
wget http://www.h2database.com/h2-2015-04-10.zip
unzip *.zip
chmod +x h2/bin/h2.sh
sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
sudo service tomcat7 stop
cd ~
git clone https://github.com/greenjoe/edward.git
cd edward
mvn package
sudo cp edward-communication/target/*.war /var/lib/tomcat7/webapps/edward.war
sudo cp edward-persistence/target/schemaForJooq.mv.db ~/edward.mv.db
sudo chown ubuntu:ubuntu ~/edward.mv.db 
```
Server webpage address: `http://localhost:8080/edward/`  (final slash is unfortunately important, without it styles and scripts don't load). 
