#!/bin/bash
mkdir -p bin
javac -d bin src/*/*.java src_exp/*/*/*.java test_src/*/*/*.java -extdirs libs/
javac -d bin src_exp/*/*/*.java src/*/*.java test_src/*/*/*.java -extdirs libs/

