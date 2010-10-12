'''
Copyright 2009 Rob Jansen
Released under GPLv3. See LICENSE.txt for details. 
'''

# will contain sim_num as keys and list of times for CDF as values
cdf_d = {}
settings_d = {}
# aggregate info about messages
msg_d = {}
# should the graphs we generate be auto-saved
do_save = False

def get_plots():
    '''
    This is specific to a 20 run experiment with run indices as follows:
    Anonymity.numberOfAnonymityGroups = [2; 4; 6; 8; 10]
    Group.movementModel = [RandomWaypoint; MapBasedMovement; ShortestPathMapBasedMovement; MapRouteMovement]
    
    Each key represents a line in the cdf_d. The keys specified in the plots list
    will determine which lines are drawn. Each key represents a line.
    
    MESSAGES - 
    Messages include an extra value: 
    d-time message spent decrypted
    e-time message spent encrypted
    t-total time from creation to delivery
    For example, we have the following keys:
    2:rw:d    2:mb:d    2:sp:d    2:mr:d
    2:rw:e    2:mb:e    2:sp:e    2:mr:e
    2:rw:t    2:mb:t    2:sp:t    2:mr:t
    4:mb:d    4:sp:d    4:mr:d    4:rw:d
    etc...
    '''
    # messages
    plots = [
              ["RandomWaypoint, Overhead", ["4:rw:o", "8:rw:o", "16:rw:o", "32:rw:o"]],
              ["MapBased, Overhead", ["4:mb:o", "8:mb:o", "16:mb:o", "32:rw:o"]],
#              ["RandomWaypoint, Overhead", ["2:rw:o", "6:rw:o", "10:rw:o"]],
#              ["MapBased, Overhead", ["2:mb:o", "6:mb:o", "10:mb:o"]],
#              ["ShortestPathMapBased, Overhead", ["2:sp:o", "6:sp:o", "10:sp:o"]],
#              ["MapRoute, Overhead", ["2:mr:o", "6:mr:o", "10:mr:o"]],
#              ["RandomWaypoint, Normal", ["1:rw:d", "1:rw:e", "1:rw:t"]],
#              ["MapBased, Normal", ["1:mb:d", "1:mb:e", "1:mb:t"]],
#              ["ShortestPathMapBased, Normal", ["1:sp:d", "1:sp:e", "1:sp:t"]],
#              ["MapRoute, Normal", ["1:mr:d", "1:mr:e", "1:mr:t"]],
#              ["2Groups, RandomWaypoint, AllTimes", ["2:rw:d", "2:rw:e", "2:rw:t"]],
#              ["2Groups, MapBased, AllTimes", ["2:mb:d", "2:mb:e", "2:mb:t"]],
#              ["2Groups, ShortestPathMapBased, AllTimes", ["2:sp:d", "2:sp:e", "2:sp:t"]],
#              ["2Groups, MapRoute, AllTimes", ["2:mr:d", "2:mr:e", "2:mr:t"]],
#              ["4Groups, RandomWaypoint, AllTimes", ["4:rw:d", "4:rw:e", "4:rw:t"]],
#              ["4Groups, MapBased, AllTimes", ["4:mb:d", "4:mb:e", "4:mb:t"]],
#              ["4Groups, ShortestPathMapBased, AllTimes", ["4:sp:d", "4:sp:e", "4:sp:t"]],
#              ["4Groups, MapRoute, AllTimes", ["4:mr:d", "4:mr:e", "4:mr:t"]],
#              ["6Groups, RandomWaypoint, AllTimes", ["6:rw:d", "6:rw:e", "6:rw:t"]],
#              ["6Groups, MapBased, AllTimes", ["6:mb:d", "6:mb:e", "6:mb:t"]],
#              ["6Groups, ShortestPathMapBased, AllTimes", ["6:sp:d", "6:sp:e", "6:sp:t"]],
#              ["6Groups, MapRoute, AllTimes", ["6:mr:d", "6:mr:e", "6:mr:t"]],
#              ["8Groups, RandomWaypoint, AllTimes", ["8:rw:d", "8:rw:e", "8:rw:t"]],
#              ["8Groups, MapBased, AllTimes", ["8:mb:d", "8:mb:e", "8:mb:t"]],
#              ["8Groups, ShortestPathMapBased, AllTimes", ["8:sp:d", "8:sp:e", "8:sp:t"]],
#              ["8Groups, MapRoute, AllTimes", ["8:mr:d", "8:mr:e", "8:mr:t"]],
#              ["10Groups, RandomWaypoint, AllTimes", ["10:rw:d", "10:rw:e", "10:rw:t"]],
#              ["10Groups, MapBased, AllTimes", ["10:mb:d", "10:mb:e", "10:mb:t"]],
#              ["10Groups, ShortestPathMapBased, AllTimes", ["10:sp:d", "10:sp:e", "10:sp:t"]],
#              ["10Groups, MapRoute, AllTimes", ["10:mr:d", "10:mr:e", "10:mr:t"]]
            ]
    return plots

def run():
    filename = parse_input_arg()
    parse_file(filename)
    compute_overhead()
    draw_graphs(get_plots())

def parse_input_arg():
    '''
    grabs the filename from argv
    @return: the corresponsing file handle
    '''
    global do_save
    import sys
    if(len(sys.argv) < 2):
        print "wrong number of args:"
        print "[string] - filename of msg log"
        print "[True|False] - should we save images?"
        exit()
    filename = sys.argv[1]
    do_save = str(sys.argv[2]).strip().lower() == "true"
    return filename

def parse_file(filename):
    '''
    Go through the file given as input one line at a time and parse
    the times recorded when a node found all groups
    '''
    import gzip
    file = gzip.open(filename)
    current_key = ""
    for line in file:
        if(line[0:1] is "#"):
            parse_other_line(line)
        elif(line.find("Running simulation '") > -1):
            # get the new key
            current_key = parse_sim_line(line)
        elif(line.find("MSGINFO:") > -1):
            parse_msg_line(line)
        elif(line.find("Simulation done in") > -1):
            sim_transition(current_key)
        else:
            parse_other_line(line)
    file.close()

def parse_sim_line(line):
    '''
    parse a line of format "Running simulation 'rgroup_scenario: seed=1; anongroups=10; movement=MapBasedMovement; hosts=10;'"
    Put data in the data dict keyed at the current run
    '''
    seed = line[line.find("=") + 1:line.find(";")]
    line = line[line.find(";") + 2:]
    anon_groups = line[line.find("=") + 1:line.find(";")]
    line = line[line.find(";") + 2:]
    movement = line[line.find("=") + 1:line.find(";")]
    line = line[line.find(";") + 2:]
    hosts = line[line.find("=") + 1:line.find(";")]

    current_key = anon_groups + ":" + get_shortened_model(movement)
    settings_d[current_key] = {'seed': seed, 'anon_groups': anon_groups, 'movement': movement, 'hosts': hosts}
    return current_key

def get_shortened_model(movement):
    result = ""
    if movement == "RandomWaypoint":
        result = "rw"
    elif movement == "MapBasedMovement":
        result = "mb"
    elif movement == "ShortestPathMapBasedMovement":
        result = "sp"
    elif movement == "MapRouteMovement":
        result = "mr"
    return result

def parse_msg_line(line):
    '''
    parse MSGINFO line
        MSGINFO: M3, created=84.0
        MSGINFO: M3, decrypted=85.0
        MSGINFO: M3, delivered=87.89999999999928
    '''
    global msg_d
    if line.find("euclidean") > -1:
        return
    msg = line[line.find(":") + 2: line.find(",")]
    line = line[line.find(",") + 2:]
    action_key = line[:line.find("=")]
    action_val = float(line[line.find("=") + 1:])

    # store the action
    if(msg not in msg_d):
        action_d = {}
        msg_d[msg] = action_d
    msg_d[msg][action_key] = action_val

def sim_transition(current_key):
    global msg_d
    # moving to next sim, analyze msgs if any exist
    if(len(msg_d) > 0):
        # use current data to build cdf structure
        construct_msg_cdf_data(current_key)
        # save all msg data
        # reset msg_d for next sim
        msg_d = {}

def construct_msg_cdf_data(k):
    global cdf_d
    cre_key = "created"
    dec_key = "decrypted"
    del_key = "delivered"

    for msg in msg_d:
        action_d = msg_d[msg]
        if(cre_key in action_d):
            # msg created
            if(dec_key in action_d):
                # msg created and decrypted - (must be anon mode)
                # want time spent encrypted = time decrypted - time created
                if k + ":e" not in cdf_d: cdf_d[k + ":e"] = {}
                cdf_d[k + ":e"][msg] = action_d[dec_key] - action_d[cre_key]
                if(del_key in action_d):
                    # msg created, decrypted, and delivered
                    # want decrypted time = time delivered - time decrypted
                    if k + ":d" not in cdf_d: cdf_d[k + ":d"] = {}
                    cdf_d[k + ":d"][msg] = action_d[del_key] - action_d[dec_key]
                    # want total time = time delivered - time created
                    if k + ":t" not in cdf_d: cdf_d[k + ":t"] = {}
                    cdf_d[k + ":t"][msg] = action_d[del_key] - action_d[cre_key]
            elif(del_key in action_d):
                # msg created and delivered - (must be normal mode)
                # want total time = time delivered - time created
                if k + ":t" not in cdf_d: cdf_d[k + ":t"] = {}
                cdf_d[k + ":t"][msg] = action_d[del_key] - action_d[cre_key]

def parse_other_line(line):
    '''
    parse an unhandled line
    currently the line is ignored
    '''
    print "Not consumed:", line,

def compute_overhead():
    global cdf_d
    # overhead is normal msg total time - anon msg decrypted time
    # keys of normal msgs total times
    normal_keys = []
    # keys of anon msg decrypted times
    anon_keys = []
    # populate lists
    for key in cdf_d:
        if key.find("1:") > -1 and key.find(":t") > -1:
            normal_keys.append(key)
        elif key.find("1:") < 0 and key.find(":d") > -1:
            anon_keys.append(key)
    # we will be creating new "overhead" lines in the cdf_d
    for key in anon_keys:
        # the new overhead key for cdf_d
        new_key = key[:key.rfind(":")+1] + "o"
        cdf_d[new_key] = {}
        # the search key so we know what normal key to use for total times
        search_key = key[key.find(":") + 1:key.rfind(":")]
        for key2 in normal_keys:
            if key2.find(search_key) > -1:
                # do the actual overhead computation
                not_found = 0
                for msg in cdf_d[key]:
                    # normal total - anon decrypted
                    if msg in cdf_d[key2]:
                        overhead = cdf_d[key2][msg] - cdf_d[key][msg]
                        cdf_d[new_key][msg] = overhead
                    else:
                        not_found += 1
                for msg in cdf_d[key2]:
                    if msg not in cdf_d[key]:
                        not_found -= 1
                print not_found, "total +- msgs delivered in anon mode but not in normal"

def draw_graphs(plots=None):
    '''
    the cdf_d was filled after each sim
    '''
    import matplotlib.pyplot as pp

    # if no plots are specified, plot each key as separate graph
    # key in cdf_d and settings_d should be identical
    if plots is None:
        plots = []
        for key in cdf_d:
            plots.append(["", [key]])

    for plot in plots:
        fig_title = plot[0]
        line_data_list = plot[1]
        pp.figure()

        plot_lines(line_data_list)

        #axis() returns [xmin, xmax, ymin, ymax]
        ax = pp.axis()
        #pp.axis([ax[0], ax[1], 0, 1])

        pp.xlabel("Time (simulated hours)")
        pp.ylabel("Cumulative Fraction")
        title_key = line_data_list[0][:len(line_data_list[0]) - 2]
        title = "CDF: " + settings_d[title_key]['hosts'] + " hosts, " + fig_title
        pp.title(fig_title)
        pp.legend(loc='lower right')

        if (do_save == True): pp.savefig(fig_title.replace(", ", "_"))
    if do_save == False: pp.show()
    else: print "Done.\nGraphs saved to current directory."

def plot_lines(lines):
    import matplotlib.pyplot as pp
    style_offset = 0
    line_styles = [':', '--', '-']
    to_hours = 1.0 / 60.0 / 60.0
    for key in lines:
        if key not in cdf_d: continue
        #get x values of a run in the dict (in seconds)
        x_sec = sorted(cdf_d[key].values())
        x = []
        y = []
        #insert zeroes so we have an origin
        x.insert(0, x_sec[0] * to_hours)
        y.insert(0, float(0))
        #populate x and y values
        #dont count the first one since we constructed the origin
        for i in xrange(1, len(x_sec)):
            x.append(x_sec[i] * to_hours)
            y.append(y[i - 1] + float(1) / float(len(x_sec)))

        pp.plot(x, y, ls=line_styles[style_offset], lw=2.0, label=key)
        style_offset += 1
        if (style_offset >= len(line_styles)): style_offset = 0

if __name__ == '__main__':
    run()
    pass # useful for breakpoint
