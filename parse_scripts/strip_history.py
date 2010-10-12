'''
Copyright 2009 Rob Jansen
Released under GPLv3. See LICENSE.txt for details. 

Strips the history section out of the datafiles. The history is unused as of 
this point, and takes up a lot of space. Datafile input must be in .gz format.
'''

def run():
    import sys, gzip
    if(len(sys.argv) < 2):
        print "wrong number of args (datafilename(str))"
        exit
    infilename = sys.argv[1]
    outfilename = infilename[:infilename.find('.')] + "_history_scrubbed.gz"
    infile = gzip.open(infilename, "rb")
    outfile = gzip.open(outfilename, "wb")
    
    for line in infile:
        if(line[0:1] is not "#" and line.find("node") > -1 and line.find("[") > -1 and line.find("]") > -1):
            line = line[:line.rfind('[')]
            line += "[history scrubbed]\n"
        outfile.write(line)

if __name__ == '__main__':
    run()
