/* 
 * Copyright 2009 Rob Jansen
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.List;


import core.AnonymousGroupManager;
import core.DTNHost;

public class AnonymousGroupReport extends Report {
	private List<DTNHost> hosts = null;

	@Override
	public void done() {
		AnonymousGroupManager.printGroupsSizes();
		// notify group managers that sim is done so they can print
		write(AnonymousGroupManager.GM_STRING_FORMAT);
		if (this.hosts != null) {
			for (DTNHost host : this.hosts) {
				write(host.getGroupManager().toString());
			}
		}
		super.done();
	}

	public void setHosts(List<DTNHost> hosts) {
		this.hosts = hosts;
	}

	@Override
	protected void write(String txt) {
		System.out.println(txt);
	}
}
