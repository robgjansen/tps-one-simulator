'''
Copyright 2009 Rob Jansen
Released under GPLv3. See LICENSE.txt for details. 

    # format of data is assumed:
    # list = [tuple1, tuple2,...]
    # where each tuple is [[x_1,x_2,...], [y_1,y_2,...]]
    
    Currently only can create a single graph from each import file.
'''

def get_options():
    global options, args
    from optparse import OptionParser, OptionGroup
    
    usage = "usage: %prog [options] -i arg1 | arg1 ..."
    description = "This is a log parser. An input source must specified with either the -i flag or a list of args to parse. The default mode of operation is to parse a logfile and produce graphs. The parsed data can be exported and later imported to avoid reparsing files when re-graphing data. Saving and viewing options are available for execution on remote machines."
    
    # setup options
    parser = OptionParser(usage=usage, description=description)
    parser.add_option("-p", "--parallel", metavar="N", type="int", action="store", dest="num_workers", default=1,
                      help="concurrently parse several files using N worker processes (set N<1 for N=cpu_count) [default=1]")
    parser.add_option("-e", "--export", metavar="FILENAME", type="string", action="store", dest="csv_export_filename",
                      help="export parsed data to a gzipped csv FILENAME that can be imported with the -i flag and graphed with the -g flag")
    parser.add_option("-g", "--graph", action="store_true", dest="graph_figs", default=True,
                      help="graph data either parsed directly or imported with the -i flag")
    parser.add_option("-i", "--import", metavar="FILENAME", type="string", action="store", dest="csv_import_filename",
                      help="import parsed data from a gzipped csv FILENAME exported with the -e flag (this avoids re-parsing original file) [default=None]")
    parser.add_option("-v", "--verbose", action="store_true", dest="verbose", default=False,
                      help="Verbosely print status [default=False]")
    
    group = OptionGroup(parser, "Graphing Options",
                    "These options are only used in graphing mode")
    group.add_option("-s", "--save", action="store_true", dest="save_figs", default=False,
                      help="when graphing, save the generated images as pdfs in the current directory, using default filenames [default=False]")
    group.add_option("-x", "--noshow", action="store_true", dest="noshow_figs", default=False,
                      help="when graphing, do NOT show figures if they are generated (useful for headless servers) [default=False]")
    
    parser.add_option_group(group)
    
    group = OptionGroup(parser, "TPS specific Options",
                    "These options are only used in graphing mode")
    parser.add_option("--normal", metavar="FILENAME", type="string", action="store", dest="normal_filename",
                      help="FILENAME that contains the normal data - used to compute overhead")

    # parse args from command line
    (options, args) = parser.parse_args()
    if len(args) < 1 and options.csv_import_filename is None:
        parser.error("incorrect number of arguments")
    if options.csv_export_filename is not None and options.csv_export_filename.find(".gz") < 0:
        parser.error("please specify a .gz filename for the gzipped export")
    if options.csv_import_filename is not None and options.csv_import_filename.find(".gz") < 0:
        parser.error("please specify a .gz filename for the gzipped import")
        
    # alter options as needed
    if options.num_workers < 1:
        from multiprocessing import cpu_count
        options.num_workers = cpu_count()
    
    print "using options:"
    print "-p", options.num_workers, "-e", options.csv_export_filename, "-g", options.graph_figs, "-i", options.csv_import_filename, "-s", options.save_figs, "-v", options.verbose, "-x", options.noshow_figs
    print "using args:"
    print args

def main():
    get_options()
    
    plotable_xy_tuple_list = []
    
    verbose_print("checking input source")
    if options.csv_import_filename is not None:
        verbose_print("we are importing graph data from an already parsed file")
        plotable_xy_tuple_list = import_dat()
    else:
        verbose_print("we are parsing a file")
        verbose_print("checking concurrency level")
        if options.num_workers > 1:
            verbose_print("using parallel process parse mode with " + str(options.num_workers) + " workers")
            plotable_xy_tuple_list = parallel_parse(args)
        else:
            verbose_print("using single process parse mode")
            for filename in args:
                verbose_print("parsing file " + filename)
                plotable_xy_tuple = parse(filename)
                plotable_xy_tuple_list.append(plotable_xy_tuple)
    
    verbose_print("data imported, checking export options")
    if options.csv_export_filename is not None:
        verbose_print("exporting data to gzipped filename " + options.csv_export_filename)
        export_dat(plotable_xy_tuple_list)
        verbose_print("data exported")
    else: verbose_print("no export option selected")
        
    verbose_print("checking graphing options")        
    if options.graph_figs:
        verbose_print("graphing figures")
        graph(plotable_xy_tuple_list)
    else: verbose_print("no graphing option selected")
        
    verbose_print("parser finished, exiting")
        
def parallel_parse(filenames):
    from multiprocessing import Pool, cpu_count
    pool = Pool(processes=cpu_count())

    results = []
    for filename in filenames:
        results.append(pool.apply_async(parse, [filename]))
    pool.close()
    pool.join()

    # get actual data from multiprocessing result objects
    plotable_xy_tuple_list = []
    for result in results:
        plotable_xy_tuple_list.append(result.get())
    return plotable_xy_tuple_list
    
def parse(filename):
    import gzip
    x, y = [], []
    msg_d = {}
    
    if filename.find('.gz') > -1: file = gzip.open(filename)
    else: file = open(filename)
    
    for line in file:
        if(line.find("MSGINFO:") > -1):
            mid = line[line.find(":") + 2: line.find(",")]
            line = line[line.find(",") + 2:]
            action_key = line[:line.find("=")]
            action_val = float(line[line.find("=") + 1:])
            
            # store the action
            if mid not in msg_d:
                msg_d[mid] = {}
            # possible action keys: created, decrypted, delivered, euclidean
            msg_d[mid][action_key] = action_val
            
            #local_d[mean_dist_decrypted_i] = ((local_d[mean_dist_decrypted_i] * local_d[num_decrypted_i]) + action_val) / (local_d[num_decrypted_i] + 1.0)
        else: verbose_print("Not consumed:" + line)

    for mid in msg_d:
        if 'delivered' in msg_d[mid]:
            x.append((msg_d[mid]['delivered'] - msg_d[mid]['created']) / 3600)
            
    x.sort()
            
    from numpy import arange
    y = arange(len(x))/float(len(x))

    plotable_xy_tuple = []
    plotable_xy_tuple.append(x)
    plotable_xy_tuple.append(y)
    
    return plotable_xy_tuple

def import_dat():
    import gzip, csv
    reader = csv.reader(gzip.open(options.csv_import_filename), delimiter=',')
    plotable_xy_tuple_list = []
    xy_tuple = []
    isy = False
    for row in reader:
        xy_tuple.append(row)
        if isy:
            isy = False
            # reader gives us strings, convert to something plot-able
            for i in xrange(len(xy_tuple)):
                for j in xrange(len(xy_tuple[i])):
                    xy_tuple[i][j] = float(xy_tuple[i][j])
            plotable_xy_tuple_list.append(xy_tuple)
            xy_tuple = []
        else: isy = True

    return plotable_xy_tuple_list
    
def export_dat(plotable_xy_tuple_list):
    import gzip, csv
    writer = csv.writer(gzip.open(options.csv_export_filename, 'w'), delimiter=',')
    for xy_tuple in plotable_xy_tuple_list:
        writer.writerows(xy_tuple)
 
def graph(plotable_xy_tuple_list):
    if options.noshow_figs:
        from matplotlib import use
        use('Agg')
    from matplotlib.pylab import figure, plot, legend, gca, savefig, show, ylabel, xlabel

    figure(figsize=(6, 4))
    for xy_tuple in plotable_xy_tuple_list:
        plot(xy_tuple[0], xy_tuple[1], lw="3.0")
    
    # see http://matplotlib.sourceforge.net/users/legend_guide.html
    leg = legend(loc='lower right', ncol=1, borderaxespad=0.5, numpoints=1, handletextpad=0.2)
    if leg is not None:
        for t in leg.get_texts():
            t.set_fontsize('small')
            
    xlabel("Time (h)")
    ylabel("Cumulative Fraction")
    
    # grid
    gca().yaxis.grid(True, c='0.5')
    gca().xaxis.grid(True, c='0.5')
    
    if options.save_figs:
        # Most backends support png, pdf, ps, eps and svg.
        savefig("_data_graph.pdf", format="pdf")
        
    if not options.noshow_figs:
        show()

def verbose_print(message):
    if options.verbose: print message
        
if __name__ == '__main__':
    main()
