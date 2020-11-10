export JAVA_HOME=$(realpath /usr/bin/javadoc | sed 's@bin/javadoc$@@')

mvn clean -P maven-release -DskipTests deploy
