#!/usr/bin/python

import os
import shutil



javaCCFiles = ["TokenMgrError.java", "ParseException.java", "Token.java", "SimpleCharStream.java"]



# remove old javacc java files if they exist
for f in javaCCFiles:
    filePath = "src/" + f
    if (os.path.isfile(filePath)):
        os.remove(filePath)






os.system(" java -cp ./tools/javacc.jar javacc -OUTPUT_DIRECTORY=src src/*.jj")

if (os.path.isdir("build")):
    shutil.rmtree("build")

os.makedirs("build")

os.system("javac -d build src/*.java")
