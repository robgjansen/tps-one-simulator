'''
Copyright 2009 Rob Jansen
Released under GPLv3. See LICENSE.txt for details. 
'''
import sys, gzip

def main():
    basename = "parsed_data_"
    counter = 1
    output = open(basename + str(counter), 'w')
    for line in gzip.open(sys.argv[1]):
        if(line.find("Run ") > -1):
            print line,
            output = open(basename + str(counter), 'w')
            counter += 1
        else:
            print >> output, line,

if __name__ == '__main__':
    main()