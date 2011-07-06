#!/usr/bin/python
import sys
if (len(sys.argv) > 1):
        num=int(sys.argv[1])
else:
        num=int(raw_input("How many nodes are in your cluster? "))
for i in range(0, num):
        print 'node %d: %d' % (i, (i*(2**127)/num))