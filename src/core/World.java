/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * World contains all the nodes and is responsible for updating their location
 * and connections.
 */
public class World {
	/** namespace of optimization settings ({@value} ) */
	public static final String SETTINGS_NS = "Optimization";
	/**
	 * Connection algorithm selection -setting id ({@value} ). <BR>
	 * Valid values are
	 * <UL>
	 * <LI>1 (standard and slow but saves memory)
	 * <LI>2 (cell optimized for mediocre amount of nodes) see
	 * {@link ConnectivityGrid#connectToNearNodes(DTNHost)}
	 * <LI>3 (cell optimized for large amount of nodes) see
	 * {@link ConnectivityGrid#connectAllNearNodes()}
	 * </UL>
	 * Default value is {@link #DEF_CON_ALG}. Selection of the algorithm should
	 * not affect the amount of connections but it may affect the ordering of
	 * them (i.e. the order in which connections during the same update interval
	 * happen may change and also the "initiator" of the connection may change
	 * if the algorithm is changed).
	 */
	public static final String CON_ALG_S = "connectionAlg";
	/**
	 * Cell based optimization cell size multiplier -setting id ({@value} ).
	 * Single ConnectivityCell's size is the biggest radio range times this.
	 * Larger values save memory and decrease startup time but may result in
	 * slower simulation especially with algorithm 2. This has no effect for
	 * algorithm 1. Default value is {@link #DEF_CON_CELL_SIZE_MULT}. Smallest
	 * accepted value is 2.
	 * 
	 * @see ConnectivityGrid
	 */
	public static final String CELL_SIZE_MULT_S = "cellSizeMult";
	/**
	 * Should the order of node updates be different (random) within every
	 * update step -setting id ({@value} ). Boolean (true/false) variable.
	 * Default is @link {@link #DEF_RANDOMIZE_UPDATES}.
	 */
	public static final String RANDOMIZE_UPDATES_S = "randomizeUpdateOrder";

	/** default value for connection checking algorithm ({@value} ) */
	public static final int DEF_CON_ALG = 2;
	/** default value for cell size multiplier ({@value} ) */
	public static final int DEF_CON_CELL_SIZE_MULT = 5;
	/**
	 * should the update order of nodes be randomized -setting's default value
	 * ({@value} )
	 */
	public static final boolean DEF_RANDOMIZE_UPDATES = true;

	private int sizeX;
	private int sizeY;
	private List<EventQueue> eventQueues;
	private ConnectivityGrid conGrid;
	private double updateInterval;
	private SimClock simClock;
	private double nextQueueEventTime;
	private EventQueue nextEventQueue;
	/** list of nodes; nodes are indexed by their network address */
	private List<DTNHost> hosts;
	private boolean simulateConnections;
	/**
	 * nodes in the order they should be updated (if the order should be
	 * randomized; null value means that the order should not be randomized)
	 */
	private ArrayList<DTNHost> updateOrder;
	/** is cancellation of simulation requested from UI */
	private boolean isCancelled;
	private List<UpdateListener> updateListeners;
	/** Queue of scheduled update requests */
	private static ScheduledUpdatesQueue scheduledUpdates;

	/** used connection algorithm */
	private int conAlgorithm;
	/** single ConnectivityCell's size is biggest radio range times this */
	private int conCellSizeMult;

	/**
	 * Constructor.
	 * 
	 * @param scen
	 *            The Scenario to base this world on.
	 */
	public World(SimScenario scen) {
		this.sizeX = scen.getWorldSizeX();
		this.sizeY = scen.getWorldSizeY();
		this.simClock = SimClock.getInstance();
		this.updateInterval = scen.getUpdateInterval();
		this.updateListeners = scen.getUpdateListeners();

		this.hosts = scen.getHosts();
		this.simulateConnections = scen.simulateConnections();
		this.eventQueues = scen.getExternalEvents();
		scheduledUpdates = new ScheduledUpdatesQueue();
		setNextEventQueue();

		this.isCancelled = false;
		initSettings();

		if (this.simulateConnections && conAlgorithm != 1) {
			/* use cell based optimization */
			this.conGrid = new ConnectivityGrid(sizeX, sizeY, (int) (scen
					.getMaxHostRange() * conCellSizeMult));
			this.conGrid.addNodes(this.hosts);
		}
	}

	/**
	 * Initializes settings fields that can be configured using Settings class
	 */
	private void initSettings() {
		Settings s = new Settings(SETTINGS_NS);

		boolean randomizeUpdates = DEF_RANDOMIZE_UPDATES;
		if (s.contains(RANDOMIZE_UPDATES_S)) {
			randomizeUpdates = s.getBoolean(RANDOMIZE_UPDATES_S);
		}

		if (randomizeUpdates) {
			// creates the update order array that can be shuffled
			this.updateOrder = new ArrayList<DTNHost>(this.hosts);
		} else { // null pointer means "don't randomize"
			this.updateOrder = null;
		}

		if (s.contains(CON_ALG_S)) {
			conAlgorithm = s.getInt(CON_ALG_S);
		} else {
			conAlgorithm = DEF_CON_ALG;
		}
		if (s.contains(CELL_SIZE_MULT_S)) {
			conCellSizeMult = s.getInt(CELL_SIZE_MULT_S);
		} else {
			conCellSizeMult = DEF_CON_CELL_SIZE_MULT;
		}

		// check that values are within limits
		if (conCellSizeMult < 2) {
			throw new SettingsError("Too small value (" + conCellSizeMult
					+ ") for " + SETTINGS_NS + "." + CELL_SIZE_MULT_S);
		}
		if (conAlgorithm > 3 || conAlgorithm < 1) {
			throw new SettingsError("Invalid value (" + conAlgorithm + ") for "
					+ SETTINGS_NS + "." + CON_ALG_S);
		}
	}

	/**
	 * Moves hosts in the world for the time given time initialize host
	 * positions properly. SimClock must be set to <CODE>-time</CODE> before
	 * calling this method.
	 * 
	 * @param time
	 *            The total time (seconds) to move
	 */
	public void warmupMovementModel(double time) {
		if (time <= 0) {
			return;
		}

		while (SimClock.getTime() < -updateInterval) {
			moveHosts(updateInterval);
			simClock.advance(updateInterval);
		}

		double finalStep = -SimClock.getTime();

		moveHosts(finalStep);
		simClock.setTime(0);
	}

	/**
	 * Goes through all event Queues and sets the event queue that has the next
	 * event.
	 */
	public void setNextEventQueue() {
		EventQueue nextQueue = scheduledUpdates;
		double earliest = nextQueue.nextEventsTime();

		/* find the queue that has the next event */
		for (EventQueue eq : eventQueues) {
			if (eq.nextEventsTime() < earliest) {
				nextQueue = eq;
				earliest = eq.nextEventsTime();
			}
		}

		this.nextEventQueue = nextQueue;
		this.nextQueueEventTime = earliest;
	}

	/**
	 * Update (move, connect, disconnect etc.) all hosts in the world. Runs all
	 * external events that are due between the time when this method is called
	 * and after one update interval.
	 */
	public void update() {
		double runUntil = SimClock.getTime() + this.updateInterval;

		setNextEventQueue();

		/* process all events that are due until next interval update */
		while (this.nextQueueEventTime <= runUntil) {
			simClock.setTime(this.nextQueueEventTime);
			ExternalEvent ee = this.nextEventQueue.nextEvent();
			ee.processEvent(this);
			updateHosts(); // update all hosts after every event
			setNextEventQueue();
		}

		moveHosts(this.updateInterval);
		simClock.setTime(runUntil);

		if (simulateConnections) {
			connectHosts(); // make connections
		}

		updateHosts();

		/* inform all update listeners */
		for (UpdateListener ul : this.updateListeners) {
			ul.updated(this.hosts);
		}
	}

	/**
	 * Updates all hosts (calls update for every one of them). If update order
	 * randomizing is on (updateOrder array is defined), the calls are made in
	 * random order.
	 */
	private void updateHosts() {
		if (this.updateOrder == null) { // randomizing is off
			for (int i = 0, n = hosts.size(); i < n; i++) {
				hosts.get(i).update();
			}
		} else { // update order randomizing is on
			assert this.updateOrder.size() == this.hosts.size() : "Nrof hosts has changed unexpectedly";
			Random rng = new Random(SimClock.getIntTime());
			Collections.shuffle(this.updateOrder, rng);
			for (int i = 0, n = hosts.size(); i < n; i++) {
				this.updateOrder.get(i).update();
			}
		}
	}

	/**
	 * Moves all hosts in the world for a given amount of time
	 * 
	 * @param timeIncrement
	 *            The time how long all nodes should move
	 */
	private void moveHosts(double timeIncrement) {
		for (int i = 0, n = hosts.size(); i < n; i++) {
			DTNHost host = hosts.get(i);
			host.move(timeIncrement);
			if (conGrid != null) {
				conGrid.updateLocation(host);
			}
		}
	}

	/**
	 * Try to connect all hosts within range
	 */
	private void connectHosts() {
		if (this.conGrid != null) { // cell-optimized way
			switch (conAlgorithm) {
			case 2: // algorithm number 2
				for (int i = 0, n = hosts.size(); i < n; i++) {
					DTNHost node = hosts.get(i);
					conGrid.connectToNearNodes(node);
					if (isCancelled) {
						// stop connecting if user wants to shut down the sim
						return;
					}
				}
				break;
			case 3: // algorithm number 3
				conGrid.connectAllNearNodes();
				break;
			default:
				assert false : "Invalid algorithm (" + conAlgorithm + ")";
			}
		} else { // the old way to do it (aka Algorithm no 1)
			// try to connect every single node to all other nodes
			for (int i = 0, n = hosts.size(); i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (isCancelled) {
						return;
					}
					hosts.get(i).connect(hosts.get(j));
				}
			}
		}
	}

	/**
	 * Asynchronously cancels the currently running simulation
	 */
	public void cancelSim() {
		this.isCancelled = true;
	}

	/**
	 * Returns the hosts in a list
	 * 
	 * @return the hosts in a list
	 */
	public List<DTNHost> getHosts() {
		return this.hosts;
	}

	/**
	 * Returns the x-size (width) of the world
	 * 
	 * @return the x-size (width) of the world
	 */
	public int getSizeX() {
		return this.sizeX;
	}

	/**
	 * Returns the y-size (height) of the world
	 * 
	 * @return the y-size (height) of the world
	 */
	public int getSizeY() {
		return this.sizeY;
	}

	/**
	 * Returns a node from the world by its address
	 * 
	 * @param address
	 *            The address of the node
	 * @return The requested node or null if it wasn't found
	 */
	public DTNHost getNodeByAddress(int address) {
		if (address < 0 || address >= hosts.size()) {
			throw new SimError("No host for address " + address + ". Address "
					+ "range of 0-" + (hosts.size() - 1) + " is valid");
		}

		DTNHost node = this.hosts.get(address);
		assert node.getAddress() == address : "Node indexing failed. "
				+ "Node " + node + " in index " + address;

		return node;
	}

	/**
	 * Schedules an update request to all nodes to happen at the specified
	 * simulation time.
	 * 
	 * @param simTime
	 *            The time of the update
	 */
	public static void scheduleUpdate(double simTime) {
		scheduledUpdates.addUpdate(simTime);
	}
}
