#!/usr/bin/python

import os
import sys

if (len(sys.argv) < 2):
    print "usage: sjavac <sjava file>"
else:
    os.system("java -cp build sjc " + sys.argv[1]);
