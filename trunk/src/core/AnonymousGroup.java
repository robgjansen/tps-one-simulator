/* 
 * Copyright 2009 Rob Jansen
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.TreeSet;


public class AnonymousGroup implements Comparable<AnonymousGroup> {

	private TreeSet<DTNHost> members;
	private String name;

	public AnonymousGroup(String name) {
		this.name = name;
		this.members = new TreeSet<DTNHost>();
	}

	public void join(DTNHost host) {
		this.members.add(host);
	}

	public void leave(DTNHost host) {
		this.members.remove(host);
	}

	public boolean isMember(DTNHost host) {
		return members.contains(host);
	}

	public TreeSet<DTNHost> getMemberSet() {
		return members;
	}

	public String getName() {
		return name;
	}

	@Override
	public int compareTo(AnonymousGroup o) {
		return name.compareTo(o.getName());
	}

	@Override
	public String toString() {
		return name;
	}
}
