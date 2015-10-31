#!/usr/bin/python

import os
import sys



if (not os.path.exists("./build")):
	print "ERROR: compiler not built!, run build.py"
elif (len(sys.argv) < 2):
	print "usage: sjavac <sjava file>"
else:
    os.system("java -cp build sjc " + sys.argv[1]);
