/* 
 * Copyright 2009 Rob Jansen
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.Hashtable;
import java.util.Random;
import java.util.TreeSet;

public class AnonymousGroupManager {
	public static final String GM_STRING_FORMAT = "# node_id, [group_names], num_groups_found, time_found_all, [groups_found]";
	public static final String GROUP_BASENAME = "g";
	public static Hashtable<String, AnonymousGroup> definedGroups = new Hashtable<String, AnonymousGroup>();
	private static Random rng = new Random();
	private static boolean defined = false;
	private static int definedIndex = 0;

	// the host this manager is managing for
	private DTNHost host;
	// the groups the host is a part of
	private TreeSet<AnonymousGroup> groups;
	// other groups this manager has connected with
	private TreeSet<AnonymousGroup> groupsMet;
	private boolean metAllGroups;
	private Double timeMetAllGroups;

	public AnonymousGroupManager(DTNHost host) {
		super();
		this.host = host;
		this.groups = new TreeSet<AnonymousGroup>();
		this.groupsMet = new TreeSet<AnonymousGroup>();
		this.metAllGroups = false;
		this.timeMetAllGroups = Double.NaN;
		AnonymousGroup g = null;
		if (SimScenario.anonymitySystem.equals(SimScenario.ANONYMITY_SYSTEM_RANDOMPIVOT)) {
			// join a group uniformly, for one node per group situations
			g = joinNextGroup();
		} else if (SimScenario.anonymitySystem.equals(SimScenario.ANONYMITY_SYSTEM_THRESHOLDPIVOT)) {
			// join a random group, they should have been defined before hosts created
			g = joinRandomGroup();
		}
		if (g != null) {
			updateHistory(g);
		}
	}

	/**
	 * Updates this group manager's knowledge of all groups the given group
	 * manager is a part of.
	 * 
	 * @param otherGm
	 *            the other host's groupManager
	 */
	public void notifyConnection(AnonymousGroupManager otherGm) {
		for (AnonymousGroup otherGroup : otherGm.groups) {
			updateHistory(otherGroup);
		}
	}

	/**
	 * Adds the given group to the history list of groups we have previously
	 * connected to. Also adds the given group to the set of groups this manager
	 * knows of if this manager does not already know all groups.
	 */
	private void updateHistory(AnonymousGroup otherGroup) {
		// keep track of groups met of all defined
		if (!this.metAllGroups && !groupsMet.contains(otherGroup)) {
			groupsMet.add(otherGroup);
			if (groupsMet.size() >= definedGroups.size()) {
				metAllGroups = true;
				timeMetAllGroups = SimClock.getTime();
			}
		}
	}

	/**
	 * This group manager joins its host to a random defined group
	 * 
	 * @return g the group joined, or null if there are no groups to join
	 */
	public AnonymousGroup joinRandomGroup() {
		AnonymousGroup g = getRandomGroup();
		g.join(host);
		this.groups.add(g);
		return g;
	}
	
	public static AnonymousGroup getRandomGroup() {
		int n = definedGroups.size();
		if (n < 1) {
			// no groups defined yet!
			return null;
		}
		int index = AnonymousGroupManager.rng.nextInt(n);
		AnonymousGroup g = (AnonymousGroup) definedGroups.values().toArray()[index];
		return g;
	}
	
	public AnonymousGroup joinNextGroup(){
		int n = definedGroups.size();
		if (n < 1) {
			// no groups defined yet!
			return null;
		}
		int index = definedIndex;
		AnonymousGroup g = (AnonymousGroup) definedGroups.values().toArray()[index];
		g.join(host);
		this.groups.add(g);
		definedIndex++;
		if(definedIndex >= definedGroups.size()){
			definedIndex = 0;
		}
		return g;
	}

	/**
	 * This group manager joins its host to the group with the given name
	 * 
	 * @return true if join was successful, false if group does not exist
	 */
	public boolean joinGroup(String groupName) {
		if (definedGroups.containsKey(groupName)) {
			AnonymousGroup g = definedGroups.get(groupName);
			g.join(host);
			this.groups.add(g);
			return true;
		} else {
			return false;
		}
	}

	@Override
	// !change the static format string if this method changes!
	public String toString() {
		String msg = this.host.toString() + ", ";
		msg += this.groups.toString() + ", ";
		msg += this.groupsMet.size() + ", ";
		msg += this.timeMetAllGroups.toString() + ", ";
		msg += this.groupsMet.toString();
		return msg;
	}

	/**
	 * Defines the global groups that are available to join.
	 * 
	 * @param numberOfGroups
	 *            the number of groups to define
	 * @return true if this call defined groups, false if this call did not
	 *         define any new groups since they have already been defined
	 */
	public static boolean defineGroups() {
		int n = SimScenario.numAnonGroups;
		if (!defined && n > 0) {
			for (int i = 0; i < n; i++) {
				String name = GROUP_BASENAME + i;
				AnonymousGroup g = new AnonymousGroup(name);
				definedGroups.put(name, g);
			}
			defined = true;
			return true;
		}
		return false;
	}

	/**
	 * Clears the hashtable and resets the defined flag so groups can be
	 * re-defined for the next run of the sim.
	 */
	public static void resetGroups() {
		definedGroups.clear();
		defined = false;
	}

	/**
	 * prints the sizes of each group
	 */
	public static void printGroupsSizes() {
		String msg = "# group sizes:";
		for (AnonymousGroup g : definedGroups.values()) {
			msg += " " + g.getName() + "=" + g.getMemberSet().size();
		}
		System.out.println(msg);
	}

	/**
	 * Gets the Anonymous groups this manager belongs to.
	 * @return groups as a TreeSet
	 */
	public TreeSet<AnonymousGroup> getGroups() {
		return this.groups;
	}
}
