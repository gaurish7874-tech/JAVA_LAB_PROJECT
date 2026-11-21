#!/usr/bin/env bash
# Plain javac build for machines without Maven.
# Put dependency jars in ./lib first (see README):
#   sqlite-jdbc, javafx-base, javafx-graphics, javafx-controls (matching your OS)
set -euo pipefail
mkdir -p out
CP=$(ls lib/*.jar | tr '\n' ':')
find src/main/java -name '*.java' > out/sources.txt
javac -cp "$CP" -d out @out/sources.txt
echo "Compiled to ./out"
