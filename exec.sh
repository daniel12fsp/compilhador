#!/bin/bash
rm *.class
cd java_code/
rm *.class
echo $(pwd)
java -jar ../cocor/Coco.jar -frames ../cocor/ ../gramatica_compilhador.atg 
mv ../*.java .
javac *.java
java Compile ../codigo_simples
java VMRun *.obj
