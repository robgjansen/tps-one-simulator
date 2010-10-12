'''
Copyright 2009 Rob Jansen
Released under GPLv3. See LICENSE.txt for details. 
'''

import sys, tarfile, matplotlib.pyplot as pp
from numpy import array, std, mean, sqrt, pi, arctan, linspace

### Configuration
common_path = "../data/sim_v4/"
filenames = [#"Epidemic/MapBased/ep_mb_sims.tgz", 
             "Epidemic/RandomWaypoint/ep_rw_sims.tgz",
             #"FirstContact/MapBased/fc_mb_sims.tgz",
             "FirstContact/RandomWaypoint/fc_rw_sims.tgz"]
save_figures = False

### Internal data structures and indices
'''
    data[router][movement][groups][nodes][seed][msgs || calc]
    where msgs[id][action] = time
        where action =
            [created,
            decrypted,
            delivered,
            dist_decrypted]
    where calc =
        [num_created
        num_decrypted
        num_delivered
        mean_dist_decrypted]
'''
data_d = {}
# indexes into data_d
msgs = "msgs"
### WARNING - dont change these - they are dependent on parsed data
created_i = "created"
decrypted_i = "decrypted"
delivered_i = "delivered"
dist_decrypted_i = "euclidean"
### end above WARNING
num_created_i = "num_created"
num_decrypted_i = "num_decrypted"
num_delivered_i = "num_delivered"
mean_dist_decrypted_i = "mean_dist_decrypted"

'''
    results[movement][graphtype][groups][coord] = (list of line plot points)
    where graphtype =
        mean_ratio_delivered
        mean_ratio_decrypted
        mean_overhead
        mean_distance
    where coord = 
        x_points
        y_points
        y_error_delta
        label
        marker
        linestyle
'''
results_d = {}
# indexes into results_d, also represents graphs y axis
mean_ratio_delivered_i = "Message Delivery Ratio"
mean_ratio_decrypted_i = "Message Decryption Ratio"
mean_overhead_i = "Delivery Time Ratio"
mean_distance_i = "Mean Distance from Source (m)"
x_points_i = "x_points"
y_points_i = "y_points"
y_error_delta_i = "y_error_delta"
label_i = "label"
marker_i = "marker"
linestyle_i = "linestyle"
color_i = "color"

'''
represent the current keys for data storage at any point during parsing
'''
router, movement, groups, nodes, seed = "", "", "", "", ""

def run():
    import_data()
    setup_results()
    print_mean_delivery_time()
    compute_mean_results()
    sort_mean_results()
    plot_mean_results()
    # cdf using X node sims
    #plot_cdf_distance(25.0)
    #plot_cdf_distance(50.0)
    #plot_cdf_distance(100.0)
    #plot_cdf_distance(150.0)
    #plot_cdf_distance(200.0)
    #plot_cdf_distance(250.0)
    pp.show()
    
def import_data():
    for filename in filenames:
        tar = tarfile.open(common_path + filename, 'r:gz')
        members = tar.getmembers()
        for member in members:
            if member.isfile() and member.name.find(".log") > -1:
                parse_file(tar.extractfile(member.name))

def parse_file(file):
    global data_d, router, movement, groups, nodes, seed
    if file is None: return
    # we are starting a new file, make sure old keys are not confused
    router, movement, groups, nodes, seed = "", "", "", "", ""
    for line in file:
        if(line[0:1] is "#"): print "Not consumed:", line,
        elif(line.find("Running simulation '") > -1):
            parse_sim_header(line)
        elif(line.find("MSGINFO:") > -1):
            parse_msg_line(line)
        else: print "Not consumed:", line,
    file.close()

'''
    Example line:
    Running simulation 'epidemic_scenario: seed=452823721; anongroups=1; movement=MapBasedMovement; hosts=100; AnonymitySystem=None;'
'''
def parse_sim_header(line):
    global data_d, router, movement, groups, nodes, seed
    router = line[line.find("'")+1:line.find("_")]
    seed = float(line[line.find("=") + 1:line.find(";")])
    line = line[line.find(";") + 2:]
    groups = float(line[line.find("=") + 1:line.find(";")])
    line = line[line.find(";") + 2:]
    movement = line[line.find("=") + 1:line.find(";")]
    line = line[line.find(";") + 2:]
    nodes = float(line[line.find("=") + 1:line.find(";")])
    
    # need correction for random pivots - we want them graphed
    # together, not on separate lines (even though they have
    # different group values)
    if groups == nodes:
        groups = 100.0
        
    # correction for epidemic
    if router == "epidemic":
        groups = 0.0
    
    # set up potential new keys
    if router not in data_d:
        data_d[router] = {}
    if movement not in data_d[router]:
        data_d[router][movement] = {}
    if groups not in data_d[router][movement]:
        data_d[router][movement][groups] = {}
    if nodes not in data_d[router][movement][groups]:
        data_d[router][movement][groups][nodes] = {}
    if seed not in data_d[router][movement][groups][nodes]:
        data_d[router][movement][groups][nodes][seed] = {}
    local_d = data_d[router][movement][groups][nodes][seed]
    if msgs not in local_d:
        local_d[msgs] = {}
    if num_created_i not in local_d[msgs]:
        local_d[num_created_i] = 0
    if num_decrypted_i not in local_d[msgs]:
        local_d[num_decrypted_i] = 0
    if num_delivered_i not in local_d[msgs]:
        local_d[num_delivered_i] = 0
    if mean_dist_decrypted_i not in local_d[msgs]:
        local_d[mean_dist_decrypted_i] = 0.0

'''
parse MSGINFO line
    MSGINFO: Mid, created=val
    MSGINFO: Mid, decrypted=val
    MSGINFO: Mid, delivered=val
    MSGINFO: Mid, euclidean=val
'''
def parse_msg_line(line):
    global data_d
    mid = line[line.find(":") + 2: line.find(",")]
    line = line[line.find(",") + 2:]
    action_key = line[:line.find("=")]
    action_val = float(line[line.find("=") + 1:])

    local_d = data_d[router][movement][groups][nodes][seed]

    # store the action
    if mid not in local_d[msgs]:
        local_d[msgs][mid] = {}
        local_d[msgs][mid][created_i] = 0.0
        local_d[msgs][mid][decrypted_i] = 0.0
        local_d[msgs][mid][delivered_i] = 0.0
        local_d[msgs][mid][dist_decrypted_i] = 0.0
    local_d[msgs][mid][action_key] = action_val

    # update aggregate data
    if action_key == created_i:
        local_d[num_created_i] += 1.0
    elif action_key == decrypted_i:
        local_d[num_decrypted_i] += 1.0
    elif action_key == delivered_i:
        local_d[num_delivered_i] += 1.0
    elif action_key == dist_decrypted_i:
        # euclidean measurement is output before decrypted count is updated
        # this keeps a running total of the mean
        local_d[mean_dist_decrypted_i] = ((local_d[mean_dist_decrypted_i] * local_d[num_decrypted_i]) + action_val) / (local_d[num_decrypted_i] + 1.0)
    else: raise Error()

def setup_results():
    '''
    Should be called before any results computations
    '''
    global results_d
    for router in data_d:
        for movement in data_d[router]:
            # do default setups and make sure keys exist for results_d
            setup_configurations(data_d[router][movement], router, movement)

def compute_mean_results():
    global results_d
    for router in data_d:
        for movement in data_d[router]:
            for group in data_d[router][movement]:
                for node in data_d[router][movement][group]:
                    # compute values from all seeds for all graph types
                    r = results_d[movement]
                    d = data_d[router][movement][group][node]    
                    compute_mean_delivery(d, r[mean_ratio_delivered_i][group], node)
                    compute_mean_decrypt(d, r[mean_ratio_decrypted_i][group], node)
                    if 1.0 in data_d[router][movement]:
                        # epidemic, for example, has no overhead
                        normal_msg_data = data_d[router][movement][1.0][node]
                        compute_mean_overhead(d, r[mean_overhead_i][group], node, normal_msg_data, group)
                    compute_mean_distance(d, r[mean_distance_i][group], node)

def compute_CI_delta(data):
    # assumes sample size of n = 10 for 95% confidence interval
    t = 2.262
    return t * (array(data).std() / sqrt(len(data)))

def compute_mean_delivery(d, r, node):
    temp = []
    for seed in d:
        #show even if none have been delivered
        temp.append(float(d[seed][num_delivered_i] / d[seed][num_created_i]))
    # compute mean and CI error bars from seed data
    if len(temp) > 0:
        r[x_points_i].append(node)
        r[y_points_i].append(array(temp).mean())
        r[y_error_delta_i].append(compute_CI_delta(temp))

def compute_mean_decrypt(d, r, node):
    temp = []
    for seed in d:
        if d[seed][num_decrypted_i] > 0:
            temp.append(d[seed][num_decrypted_i] / d[seed][num_created_i])
    # compute mean and CI error bars from seed data
    if len(temp) > 0:
        r[x_points_i].append(node)
        r[y_points_i].append(array(temp).mean())
        r[y_error_delta_i].append(compute_CI_delta(temp))
        
def compute_mean_overhead(d, r, node, normal_msg_data, group):
    temp = []
    #hack to throw out biased point for 14,14 threshol
    if node == 25 and group == 14: return
    for seed in d:
        if d[seed][num_decrypted_i] > 0:
            total_overhead_this_node = 0.0
            overhead_count = 0.0
            total_deliv = 0.0
            total_anon = 0.0
            for id in d[seed][msgs]:
                if id in normal_msg_data[seed][msgs]:
                    # msg in both normal and anon data
                    m = d[seed][msgs][id]
                    n = normal_msg_data[seed][msgs][id]
                    if m[delivered_i] > 0 and n[delivered_i] > 0:
                        # overhead is anon msg (deliv - create) - normal msg (deliv - create)
                        # (rejecting alternative def of overhead = anon msg (decrypt - create)
                        # this gives you the +- delay time caused by the onion
                        total_overhead_this_node += ((m[delivered_i] - m[created_i]) -(n[delivered_i] - n[created_i]))
                        total_deliv += (n[delivered_i] - n[created_i])
                        total_anon += (m[delivered_i] - m[created_i])
                        overhead_count += 1.0
            if overhead_count > 0:
#                temp.append((total_overhead_this_node / overhead_count) / 3600.0)
                temp.append(float(total_anon)/float(total_deliv))
    if len(temp) > 0:
        r[x_points_i].append(node)
        r[y_points_i].append(array(temp).mean())
        r[y_error_delta_i].append(compute_CI_delta(temp))
        
def compute_mean_distance(d, r, node):
    temp = []
    for seed in d:
        if d[seed][mean_dist_decrypted_i] > 0:
            temp.append(d[seed][mean_dist_decrypted_i])
    # compute mean and CI error bars from seed data
    if len(temp) > 0:
        r[x_points_i].append(node)
        r[y_points_i].append(array(temp).mean())
        r[y_error_delta_i].append(compute_CI_delta(temp))

def sort_mean_results():
    global results_d
    # convert list of plot data to tuples, sort by x axis, convert back to lists
    for movement in results_d:
        for graphtype in results_d[movement]:
            for coord in results_d[movement][graphtype]:
                out = results_d[movement][graphtype][coord]
                if len(out[x_points_i]) != len(out[y_points_i]) or len(out[x_points_i]) != len(out[y_error_delta_i]):
                    raise Error()
                    exit()
                temp = []
                for i in xrange(len(out[x_points_i])):
                    temp.append((out[x_points_i][i], out[y_points_i][i], out[y_error_delta_i][i]))
                # regular sort
                temp = sorted(temp, key=lambda x:(x[0], x[1], x[2]))
                out[x_points_i], out[y_points_i], out[y_error_delta_i] = [], [], []
                for i in xrange(len(temp)):
                    out[x_points_i].append(temp[i][0])
                    out[y_points_i].append(temp[i][1])
                    out[y_error_delta_i].append(temp[i][2])

def setup_configurations(d, router, movement):
    global results_d
    if movement not in results_d:
        results_d[movement] = {}
    for group in d:
        for node in d[group]:
            graphtypes = [mean_ratio_delivered_i, mean_ratio_decrypted_i, mean_overhead_i, mean_distance_i]
            # make sure results_d is keyed properly for each graph type
            for type in graphtypes:
                if type not in results_d[movement]:
                    results_d[movement][type] = {}
                if group not in results_d[movement][type]:
                    results_d[movement][type][group] = {}
                r = results_d[movement][type][group]
                if x_points_i not in r:
                    r[x_points_i] = []
                if y_points_i not in r:
                    r[y_points_i] = []
                if y_error_delta_i not in r:
                    r[y_error_delta_i] = []
                
                # set up some graph display properties
                mss = {2.0 : '^', 6.0 : 's', 10.0 : 'o', 14.0 : 'd'}
                lss = {2.0 : '--', 6.0 : ':', 10.0 : '--', 14.0 : ':'}
                cls = {2.0 : '0.7', 6.0 : '0.3', 10.0 : '0.3', 14.0 : '0.7'}
                if label_i not in r:
                    if router == "epidemic": # group == 0
                        r[label_i] = router
                        r[marker_i] = 'd'
                        r[linestyle_i] = ':'
                        r[color_i] = '0.5'
                    elif group == 100.0: # randompivot correction
                        r[label_i] = "ran. pivot"
                        r[marker_i] = 'x'
                        r[linestyle_i] = '-'
                        r[color_i] = '0.5'
                    elif group == 1.0:
                        r[label_i] = "first contact"
                        r[marker_i] = '*'
                        r[linestyle_i] = '-'
                        r[color_i] = 'k'
                    else:
                        r[label_i] = ' '.join([str(int(group)), "of", str(int(group))]) # "threshold"
                        r[marker_i] = mss[group]
                        r[linestyle_i] = lss[group]
                        r[color_i] = cls[group]
                    
def plot_mean_results():
    for movement in results_d:
        for graphtype in results_d[movement]:
            pp.figure(figsize=(4.5, 3))
            # correct line draw order
            draw_order = []
            for coord in results_d[movement][graphtype]:
                draw_order.append(coord)
            draw_order.sort()
            for line in draw_order:
                out = results_d[movement][graphtype][line]
                if(len(out[x_points_i]) > 0):
                    # one or the other
                    #pp.plot(out[x_points_i], out[y_points_i], lw=2.0, color=out[color_i], marker=out[marker_i], ls=out[linestyle_i], label=out[label_i])
                    lab = out[label_i]
                    #if lab != 'normal' and lab != 'epidemic': lab=''
                    ew=1
                    if out[marker_i] == 'x' or out[marker_i] == '+': ew=1.5
                    pp.errorbar(out[x_points_i], out[y_points_i], out[y_error_delta_i], lw=2.0, color=out[color_i], marker=out[marker_i], ls=out[linestyle_i], label=lab, mew=ew)
            #axis() returns [xmin, xmax, ymin, ymax]
            ax = pp.axis()
            if graphtype.find("Ratio") > -1:
                pp.axis([0, 300, 0, 1])
            else:
                pp.axis([0, 300, ax[2], ax[3]])
            pp.xlabel("Number of Nodes")
            pp.ylabel(graphtype)
            #pp.ylim(0.95,1.45)
            #pp.xlim(15,260)
            #pp.title("Mean Results, 10 Simulations, 95% CIs, " + movement)
            do_legend2()
            if save_figures == True: 
                if graphtype.find("(") > 0:
                    graphtype = graphtype[:graphtype.find("(")-1]
                pp.savefig('_'.join([movement, graphtype.replace(' ', '')]))

def plot_cdf_distance(num):
    pp.figure(figsize=(4.5, 3))
    # first draw ideal
    # worldsize = 4500x3400 -- close to square of 3950 edges
    x, y = get_unit_square_coords(3950)
    pp.plot(x, y, label="sq. line", lw=2.0, ls='-', color='k')
    # correct line draw order for the rest
    draw_order = []
    for group in data_d["fcontact"]["RandomWaypoint"]:
        draw_order.append(group)
    draw_order.sort()
    
    for group in draw_order:
        d = data_d["fcontact"]["RandomWaypoint"][group][num]
        x = []
        y = []
        for seed in d:
            for id in d[seed][msgs]:
                euclid = d[seed][msgs][id][dist_decrypted_i]
                if euclid > 0:
                    x.append(euclid)
        if len(x) == 0: continue
        x.sort()
        y.insert(0, 0)
        count = 0.0
        for item in x:
            count += 1.0 / len(x)
            y.append(count)
        x.insert(0, 0)
        out = results_d["RandomWaypoint"][mean_distance_i][group]
        pp.plot(x, y, lw=2.0, color=out[color_i], ls=out[linestyle_i], label=out[label_i])
    
    ax = pp.axis()
    pp.axis([0, 5000, 0, 1])
    pp.xlabel("Euclidean Distance from Source (m)")
    pp.ylabel("Cumulative Fraction")
    #pp.title("CDF, 10 Simulations, " + str(int(num)) + "nodes, RandomWaypoint")
    do_legend2()
    if save_figures == True: 
        pp.savefig('_'.join([movement, "EuclideanDistanceCDF", str(int(num)) + "nodes"]))
    
def do_legend():
    from matplotlib.pylab import legend
    # see http://matplotlib.sourceforge.net/users/legend_guide.html
    leg = legend(bbox_to_anchor=(0., 1.02, 1., .102), loc=3, ncol=7, mode="expand", borderaxespad=0., numpoints=1, handletextpad=0.2)
    if leg is not None:
        for t in leg.get_texts():
            t.set_fontsize('small')
    #do_grid()
            
def do_legend2():
    from matplotlib.pylab import legend
    # see http://matplotlib.sourceforge.net/users/legend_guide.html
    leg = legend(loc='upper right', ncol=1, borderaxespad=0.5, numpoints=1, handletextpad=0.2)
    if leg is not None:
        for t in leg.get_texts():
            t.set_fontsize('small')
    #do_grid()
    
def do_grid():
    from matplotlib.pylab import gca
    gca().yaxis.grid(True, c='0.5')
    gca().xaxis.grid(True, c='0.5')

def get_unit_square_coords(scale):
    x = linspace(0, sqrt(2)) # includes 0.0
    y = []
    for i in xrange(len(x)):
        y.append(unit_square_D(x[i]))
        x[i] = x[i] * scale
    #pp.figure()
    #pp.plot(x, y)
    #pp.show()
    return x, y
    
def unit_square_D(l):
    # from http://mathworld.wolfram.com/SquareLinePicking.html
    if l >= 0 and l <= 1:
        return (l**4 / 2.0) - (8.0 * l**3 / 3.0) + (pi * l**2)
    elif l >= 1 and l <= sqrt(2.0):
        return ((4.0 / 3.0) * (2 * l**2 + 1) * sqrt(l**2 - 1)) + (l**2 * (pi - 2)) + (1.0 / 3.0) - ((1.0 / 2.0) * l**4) - (4 * l**2 * arctan(sqrt(l**2 - 1)))
    else:
        print "unit square l value out of range"
        exit()

def print_mean_delivery_time():
    grand_total = 0.0
    grand_count = 0.0
    for router in data_d:
        # don't count epidemic low delivery times
        if router != "fcontact": continue
        for movement in data_d[router]:
            for group in data_d[router][movement]:
                if group != 1.0: continue
                for node in data_d[router][movement][group]:
                    d = data_d[router][movement][group][node] 
                    for seed in d:
                        for id in d[seed][msgs]:
                            if d[seed][msgs][id][delivered_i] > 0:
                                grand_total += d[seed][msgs][id][delivered_i] - d[seed][msgs][id][created_i]
                                grand_count += 1.0
    print "grand total mean fcontact message delivery time without anonymity =", (grand_total / grand_count) / 3600

if __name__ == '__main__':
    run()
    print "Done! Goodbye..."
    pass # useful for breakpoint
