/* 
 * Copyright 2008 TKK/ComNet, 2009 Rob Jansen
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import input.EventQueue;
import input.EventQueueHandler;

import java.io.*;
import java.util.*;

import routing.MessageRouter;

import movement.*;
import movement.map.*;

/**
 * A simulation scenario used for getting and storing the settings of a
 * simulation run.
 */
public class SimScenario implements Serializable {
	/** namespace of scenario settings ({@value} ) */
	public static final String SCENARIO_NS = "Scenario";
	/** number of host groups -setting id ({@value} ) */
	public static final String NROF_GROUPS_S = "nrofHostGroups";
	/** scenario name -setting id ({@value} ) */
	public static final String NAME_S = "name";
	/** end time -setting id ({@value} ) */
	public static final String END_TIME_S = "endTime";
	/** update interval -setting id ({@value} ) */
	public static final String UP_INT_S = "updateInterval";
	/** simulate connections -setting id ({@value} ) */
	public static final String SIM_CON_S = "simulateConnections";
	
	/** namespace for host group settings ({@value} ) */
	public static final String ANONYMITY_NS = "Anonymity";
	/** is anonymity on? */
	public static final String ANONYMITY_ON = "anonymityOn";
	/** number of groups in the anonymity set */
	public static final String ANONYMITY_GROUPS = "numberOfAnonymityGroups";
	/** threshold for tps */
	public static final String ANONYMITY_THRESHOLD = "anonymityThreshold";
	/** version of anon system to run - can be RandomPivot or ThresholdPivot */
	public static final String ANONYMITY_SYSTEM = "anonymitySystem";
	public static final String ANONYMITY_SYSTEM_RANDOMPIVOT = "RandomPivot";
	public static final String ANONYMITY_SYSTEM_THRESHOLDPIVOT = "ThresholdPivot";

	/** namespace for host group settings ({@value} ) */
	public static final String GROUP_NS = "Group";
	/** group id -setting id ({@value} ) */
	public static final String GROUP_ID_S = "groupID";
	/** number of hosts in the group -setting id ({@value} ) */
	public static final String NROF_HOSTS_S = "nrofHosts";
	/** transmit range -setting id ({@value} ) */
	public static final String TRANSMIT_RANGE_S = "transmitRange";
	/** transmit speed -setting id ({@value} ) */
	public static final String TRANSMIT_SPEED_S = "transmitSpeed";
	/** scanning interval -setting id ({@value} ) */
	public static final String SCAN_INTERVAL_S = "scanInterval";
	/** movement model class -setting id ({@value} ) */
	public static final String MOVEMENT_MODEL_S = "movementModel";
	/** router class -setting id ({@value} ) */
	public static final String ROUTER_S = "router";

	/** package where to look for movement models */
	private static final String MM_PACKAGE = "movement.";
	/** package where to look for router classes */
	private static final String ROUTING_PACKAGE = "routing.";

	/** List of hosts in this simulation */
	private List<DTNHost> hosts;
	/** Name of the simulation */
	private String name;
	/** number of host groups */
	int nrofGroups;
	public static boolean isAnonOn;
	/** number of anonymity groups */
	public static int numAnonGroups;
	public static int anonThreshold;
	public static String anonymitySystem;
	/** Width of the world */
	private int worldSizeX;
	/** Height of the world */
	private int worldSizeY;
	/** Largest host's radio range */
	private double maxHostRange;
	/** Simulation end time */
	private double endTime;
	/** Update interval of sim time */
	private double updateInterval;
	/** External events queue */
	private EventQueueHandler eqHandler;
	/** Should connections between hosts be simulated */
	private boolean simulateConnections;
	/** Map used for host movement (if any) */
	private SimMap simMap;

	/** Global connection event listeners */
	private List<ConnectionListener> connectionListeners;
	/** Global message event listeners */
	private List<MessageListener> messageListeners;
	/** Global movement event listeners */
	private List<MovementListener> movementListeners;
	/** Global update event listeners */
	private List<UpdateListener> updateListeners;

	/**
	 * Creates a scenario based on Settings object.
	 */
	public SimScenario() {
		Settings anonSettings = new Settings(ANONYMITY_NS);
		isAnonOn = anonSettings.getBoolean(ANONYMITY_ON);
		numAnonGroups = anonSettings.getInt(ANONYMITY_GROUPS);
		anonThreshold = anonSettings.getInt(ANONYMITY_THRESHOLD);
		ensurePositiveValue(anonThreshold, ANONYMITY_THRESHOLD);
		ensurePositiveValue(numAnonGroups, ANONYMITY_GROUPS);
		anonymitySystem = anonSettings.getSetting(ANONYMITY_SYSTEM);

		Settings s = new Settings(SCENARIO_NS);
		nrofGroups = s.getInt(NROF_GROUPS_S);

		this.name = s.valueFillString(s.getSetting(NAME_S));
		this.endTime = s.getDouble(END_TIME_S);
		this.updateInterval = s.getDouble(UP_INT_S);
		this.simulateConnections = s.getBoolean(SIM_CON_S);

		ensurePositiveValue(nrofGroups, NROF_GROUPS_S);
		ensurePositiveValue(endTime, END_TIME_S);
		ensurePositiveValue(updateInterval, UP_INT_S);

		this.simMap = null;
		this.maxHostRange = 1;

		this.connectionListeners = new ArrayList<ConnectionListener>();
		this.messageListeners = new ArrayList<MessageListener>();
		this.movementListeners = new ArrayList<MovementListener>();
		this.updateListeners = new ArrayList<UpdateListener>();
		this.eqHandler = new EventQueueHandler();
	}

	/**
	 * Makes sure that a value is positive
	 * 
	 * @param value
	 *            Value to check
	 * @param settingName
	 *            Name of the setting (for error's message)
	 * @throws SettingsError
	 *             if the value was not positive
	 */
	private void ensurePositiveValue(double value, String settingName) {
		if (value < 0) {
			throw new SettingsError("Negative value (" + value
					+ ") not accepted for setting " + settingName);
		}
	}

	/**
	 * Returns the name of the simulation run
	 * 
	 * @return the name of the simulation run
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns true if connections should be simulated
	 * 
	 * @return true if connections should be simulated (false if not)
	 */
	public boolean simulateConnections() {
		return this.simulateConnections;
	}

	/**
	 * Returns the width of the world
	 * 
	 * @return the width of the world
	 */
	public int getWorldSizeX() {
		return this.worldSizeX;
	}

	/**
	 * Returns the height of the world
	 * 
	 * @return the height of the world
	 */
	public int getWorldSizeY() {
		return worldSizeY;
	}

	/**
	 * Returns simulation's end time
	 * 
	 * @return simulation's end time
	 */
	public double getEndTime() {
		return endTime;
	}

	/**
	 * Returns update interval (simulated seconds) of the simulation
	 * 
	 * @return update interval (simulated seconds) of the simulation
	 */
	public double getUpdateInterval() {
		return updateInterval;
	}

	/**
	 * Returns how long range the hosts' radios have
	 * 
	 * @return Range in meters
	 */
	public double getMaxHostRange() {
		return maxHostRange;
	}

	/**
	 * Returns the (external) event queue(s) of this scenario or null if there
	 * aren't any
	 * 
	 * @return External event queues in a list or null
	 */
	public List<EventQueue> getExternalEvents() {
		return this.eqHandler.getEventQueues();
	}

	/**
	 * Returns the SimMap this scenario uses, or null if scenario doesn't use
	 * any map
	 * 
	 * @return SimMap or null if no map is used
	 */
	public SimMap getMap() {
		return this.simMap;
	}

	/**
	 * Adds a new connection listener for all nodes
	 * 
	 * @param cl
	 *            The listener
	 */
	public void addConnectionListener(ConnectionListener cl) {
		this.connectionListeners.add(cl);
	}

	/**
	 * Adds a new message listener for all nodes
	 * 
	 * @param ml
	 *            The listener
	 */
	public void addMessageListener(MessageListener ml) {
		this.messageListeners.add(ml);
	}

	/**
	 * Adds a new movement listener for all nodes
	 * 
	 * @param ml
	 *            The listener
	 */
	public void addMovementListener(MovementListener ml) {
		this.movementListeners.add(ml);
	}

	/**
	 * Adds a new update listener for the world
	 * 
	 * @param ul
	 *            The listener
	 */
	public void addUpdateListener(UpdateListener ul) {
		this.updateListeners.add(ul);
	}

	/**
	 * Returns the list of registered update listeners
	 * 
	 * @return the list of registered update listeners
	 */
	public List<UpdateListener> getUpdateListeners() {
		return this.updateListeners;
	}

	/**
	 * Creates hosts for the scenario
	 */
	public void createHosts() {
		this.hosts = new ArrayList<DTNHost>();

		for (int i = 1; i <= nrofGroups; i++) {
			Settings s = new Settings(GROUP_NS + i);
			s.setSecondaryNamespace(GROUP_NS);
			double scanInterval = 0;
			String gid = s.getSetting(GROUP_ID_S);
			int nrofHosts = s.getInt(NROF_HOSTS_S);
			double transmitRange = s.getDouble(TRANSMIT_RANGE_S);
			int transmitSpeed = s.getInt(TRANSMIT_SPEED_S);

			// creates prototypes of MessageRouter and MovementModel
			MovementModel mmProto = (MovementModel) s
					.createIntializedObject(MM_PACKAGE
							+ s.getSetting(MOVEMENT_MODEL_S));
			MessageRouter mRouterProto = (MessageRouter) s
					.createIntializedObject(ROUTING_PACKAGE
							+ s.getSetting(ROUTER_S));

			if (s.contains(SCAN_INTERVAL_S)) {
				scanInterval = s.getDouble(SCAN_INTERVAL_S);
			}

			// checks that these values are positive (throws Error if not)
			ensurePositiveValue(nrofHosts, NROF_HOSTS_S);
			ensurePositiveValue(transmitRange, TRANSMIT_RANGE_S);
			ensurePositiveValue(transmitSpeed, TRANSMIT_SPEED_S);

			// update max values of transmit range and width & height of world
			if (mmProto.getMaxX() > this.worldSizeX) {
				this.worldSizeX = mmProto.getMaxX();
			}
			if (mmProto.getMaxY() > this.worldSizeY) {
				this.worldSizeY = mmProto.getMaxY();
			}
			if (transmitRange > this.maxHostRange) {
				this.maxHostRange = transmitRange;
			}

			if (mmProto instanceof MapBasedMovement) {
				this.simMap = ((MapBasedMovement) mmProto).getMap();
			}
			
			// creates hosts of ith group
			for (int j = 0; j < nrofHosts; j++) {
				ModuleCommunicationBus comBus = new ModuleCommunicationBus();
				comBus.addProperty(NetworkLayer.SCAN_INTERVAL_ID, scanInterval);
				comBus.addProperty(NetworkLayer.RANGE_ID, transmitRange);
				comBus.addProperty(NetworkLayer.SPEED_ID, transmitSpeed);

				// prototypes are given to new DTNHost which replicates
				// new instances of movement model and message router
				DTNHost host = new DTNHost(this.connectionListeners,
						this.messageListeners, this.movementListeners, gid,
						comBus, mmProto, mRouterProto);
				hosts.add(host);
			}
		}
	}

	/**
	 * Returns the list of nodes for this scenario.
	 * 
	 * @return the list of nodes for this scenario.
	 */
	public List<DTNHost> getHosts() {
		return this.hosts;
	}

}