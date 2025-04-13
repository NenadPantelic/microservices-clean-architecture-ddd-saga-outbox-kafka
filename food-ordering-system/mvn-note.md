To see the dependency visualization:
1. install graphviz - `sudo apt install graphviz` -> under the hood, Maven plugin will use this lib to visualize these
dependencies
2. run [depgraph](https://github.com/ferstl/depgraph-maven-plugin) Maven plugin - `/home/nenad/Downloads/apache-maven-3.8.8/bin/mvn com.github.ferstl:depgraph-maven-plugin:aggregate -DcreateImage=true -DreduceEdges=false -Dscope=compile "-Dincludes=com.food.ordering.system*:*"`
