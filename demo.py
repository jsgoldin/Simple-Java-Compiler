#!/usr/bin/python

import os
import shutil

MARSPATH = "./tools/Mars4_5.jar"

if (not os.path.exists("./build")):
    print "ERROR: compiler not built!, run build.py"
else:    
	# load in test files
	testFiles = []
	for dirpath, dirs, files in os.walk("test_files"):
		for f in files:
		    if f.endswith(".sjava"):
		        testFiles += [[f, dirpath]]


	print "Select a simple java program to test:"

	count = 0
	for f in testFiles:
		print str(count) + ". " + f[0]
		count += 1


	selection = input(": ")


	if (os.path.isdir("test_output")):
		shutil.rmtree("test_output")

	os.makedirs("test_output")


	os.system("java -cp build sjc " + testFiles[selection][1] + "/" + 
		       testFiles[selection][0] + " test_output");


	os.system("java -jar " + MARSPATH + " test_output/" + testFiles[selection][0] + ".s");
