'''
Copyright 2009 Rob Jansen
Released under GPLv3. See LICENSE.txt for details. 
'''
dist_d = {}

def run():
    global dist_d
    import sys
    if(len(sys.argv) < 1):
        print "wrong number of args:"
        print "[string] - filename of msg log"
        exit()
    filename = sys.argv[1]

    import gzip
    file = gzip.open(filename)
    for line in file:
        if line.find("MSGINFO") > -1 and line.find("euclidean=") > -1:
            msg = line[line.find(":") + 2: line.find(",")]
            line = line[line.find(",") + 2:]
            action_key = line[:line.find("=")]
            action_val = float(line[line.find("=") + 1:])
            dist_d[msg] = action_val

    x = []
    y = []
    x.append(0.0)
    y.append(0.0)
    count = 0.0
    for key in dist_d:
        count += 1.0 / len(dist_d)
        x.append(dist_d[key])
        y.append(count)
    x.sort()
    y.sort()
    import matplotlib.pyplot as pp
    pp.figure(1)
    pp.plot(x, y, lw=2.0, ls='-')
    ax = pp.axis()
    pp.axis([ax[0], ax[1], 0, 1])
    pp.xlabel("Euclidean Distance (meters)")
    pp.ylabel("Cumulative Fraction")
    pp.show()

if __name__ == '__main__':
    run()
    pass # useful for breakpoint
