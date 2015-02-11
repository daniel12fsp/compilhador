#!/bin/bash
cd java_code/
java -jar ../cocor/Coco.jar -frames ../cocor/ ../gramatica_compilhador.atg 
javac *.java
java Compile ../codigo_portugol
