#!/usr/bin/env bash
echo "hello, ci script"
wget https://download2.gluonhq.com/substrate/graalvm/graalvm-unknown-java11-19.3.0-dev-gvm-1-linux-x86_64.zip
unzip graalvm-unknown-java11-19.3.0-dev-gvm-1-linux-x86_64.zip
echo "public class HelloWorld { public static void main(String[] args) { System.out.println(\"Hello World\"); } }" > HelloWorld.java
javac HelloWorld.java
java -cp build/lib/substrate-0.0.1-SNAPSHOT.jar -Dimagecp=. -Dgraalvm=. -Dmainclass=HelloWorld com.gluonhq.substrate.SubstrateDispatcher 
# get Graal from https://download2.gluonhq.com/substrate/graalvm/graalvm-unknown-java11-19.3.0-dev-gvm-1-darwin-amd64.zip or graalvm-unknown-java11-19.3.0-dev-gvm-1-linux-x86_64.zip
# compile HelloWorld
# java -cp substrate-0.0.1-SNAPSHOT.jar -Dimagecp=path/to/HelloWorld.class -Dgraalvm=path/to/graalinstall -Dmainclass=HelloWorld com.gluonhq.substrate.SubstrateDispatcher 
