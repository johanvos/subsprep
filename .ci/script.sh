#!/usr/bin/env bash
echo "hello, ci script"
# get Graal from https://download2.gluonhq.com/substrate/graalvm/graalvm-unknown-java11-19.3.0-dev-gvm-1-darwin-amd64.zip or graalvm-unknown-java11-19.3.0-dev-gvm-1-linux-x86_64.zip
# compile HelloWorld
# java -cp substrate-0.0.1-SNAPSHOT.jar -Dimagecp=path/to/HelloWorld.class -Dgraalvm=path/to/graalinstall -Dmainclass=HelloWorld com.gluonhq.substrate.SubstrateDispatcher 
