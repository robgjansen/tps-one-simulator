/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.*;

/**
 * Network layer of a DTNHost. Takes care of connectivity among hosts.
 */
public class NetworkLayer implements ModuleCommunicationListener {
	/**
	 * {@link ModuleCommunicationBus} identifier for the "scanning interval"
	 * variable.
	 */
	public static final String SCAN_INTERVAL_ID = "Network.scanInterval";
	/**
	 * {@link ModuleCommunicationBus} identifier for the "radio range" variable.
	 * Value type: double
	 */
	public static final String RANGE_ID = "Network.radioRange";
	/**
	 * {@link ModuleCommunicationBus} identifier for the "transmission speed"
	 * variable. Value type: integer
	 */
	public static final String SPEED_ID = "Network.speed";

	private static final int CON_UP = 1;
	private static final int CON_DOWN = 2;
	private static int nextAddress;
	private static Random rng;

	private DTNHost host;
	private List<Connection> connections; // connected hosts
	private List<ConnectionListener> cListeners; // list of listeners
	private double transmitRange; // transmission coverage
	private int transmitSpeed; // bandwidth of the transmission (Bps)
	/** scanning interval, or 0.0 if n/a */
	private double scanInterval;
	private double lastScanTime;
	private int address; // network layer address

	static {
		DTNSim.registerForReset("core.NetworkLayer");
		reset();
	}

	/**
	 * Constructor.
	 * 
	 * @param host
	 *            The host where this network layer is
	 * @param cListeners
	 *            ConnectionListeners
	 * @param comBus
	 *            Communication bus
	 */
	public NetworkLayer(DTNHost host, List<ConnectionListener> cListeners,
			ModuleCommunicationBus comBus) {
		this.connections = new ArrayList<Connection>();
		this.host = host;
		this.cListeners = cListeners;
		this.address = getNextNetAddress();
		this.transmitRange = (Double) comBus.getProperty(RANGE_ID);
		this.transmitSpeed = (Integer) comBus.getProperty(SPEED_ID);

		/* sets the scan interval and to given values or 0.0 if not defined */
		this.scanInterval = comBus.getDouble(SCAN_INTERVAL_ID, 0.0);

		comBus.subscribe(SCAN_INTERVAL_ID, this);
		comBus.subscribe(RANGE_ID, this);
		comBus.subscribe(SPEED_ID, this);

		/* draw lastScanTime of [0 -- scanInterval] */
		this.lastScanTime = rng.nextDouble() * scanInterval;
	}

	/**
	 * Returns a new network layer address and increments the address for
	 * subsequent calls.
	 * 
	 * @return The next address.
	 */
	private static int getNextNetAddress() {
		return nextAddress++;
	}

	/**
	 * Returns the network layer address.
	 * 
	 * @return The address (integer)
	 */
	public int getAddress() {
		return this.address;
	}

	/**
	 * Returns the transmit range of this network layer
	 * 
	 * @return the transmit range
	 */
	public double getTransmitRange() {
		return this.transmitRange;
	}

	/**
	 * Returns the transmit speed of this network layer
	 * 
	 * @return the transmit speed
	 */
	public int getTransmitSpeed() {
		return this.transmitSpeed;
	}

	/**
	 * Returns a list of currently connected connections
	 * 
	 * @return a list of currently connected connections
	 */
	public List<Connection> getConnections() {
		return this.connections;
	}

	public boolean isScanning() {
		double simTime = SimClock.getTime();

		if (scanInterval > 0.0) {
			if (simTime < lastScanTime) {
				return false; /* not time for the first scan */
			} else if (simTime > lastScanTime + scanInterval) {
				lastScanTime = simTime; /* time to start the next scan round */
				return true;
			} else if (simTime != lastScanTime) {
				return false; /* not in the scan round */
			}
		}
		/* interval == 0 or still in the last scan round */
		return true;
	}

	/**
	 * Tries to connect this host to another host. This host must be currently
	 * in the "scanning" mode, the other host must be active and within range of
	 * this host for the connection to succeed.
	 * 
	 * @param anotherHost
	 *            The host to try to connect to
	 */
	public void connect(DTNHost anotherHost) {
		if (!isScanning()) {
			return;
		}

		if (anotherHost.isActive() && isWithinRange(anotherHost)) {
			createConnection(anotherHost);
		}
	}

	/**
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active (cf.
	 * {@link #connect(DTNHost)}).
	 * 
	 * @param anotherHost
	 *            The host to create the connection to
	 */
	public void createConnection(DTNHost anotherHost) {
		int conSpeed;

		if (isConnected(anotherHost)) {
			return; // already connected
		}

		// connection speed is the lower one of the two speeds
		conSpeed = anotherHost.getTransmitSpeed();
		if (conSpeed > this.transmitSpeed) {
			conSpeed = this.transmitSpeed;
		}

		Connection con = new Connection(this.host, anotherHost, conSpeed);
		this.connections.add(con);
		notifyConnectionListeners(CON_UP, anotherHost);

		// set up bidirectional connection
		anotherHost.getConnections().add(con);

		// inform routers about the connection
		this.host.changedConnection(con);
		anotherHost.changedConnection(con);
	}

	/**
	 * Disconnect a connection between this and another host.
	 * 
	 * @param anotherHost
	 *            The host to disconnect from this host
	 */
	public void destroyConnection(DTNHost anotherHost) {
		for (int i = 0; i < this.connections.size(); i++) {
			if (this.connections.get(i).getOtherNode(this.host) == anotherHost) {
				removeConnectionByIndex(i);
			}
		}
		// the connection didn't exist, do nothing
	}

	/**
	 * Removes a connection by its position (index) in the connections array
	 * 
	 * @param index
	 *            The array index of the connection to be removed
	 */
	private void removeConnectionByIndex(int index) {
		Connection con = this.connections.get(index);
		DTNHost anotherNode = con.getOtherNode(this.host);
		con.setUpState(false);
		notifyConnectionListeners(CON_DOWN, anotherNode);

		// tear down bidirectional connection
		if (!anotherNode.getConnections().remove(con)) {
			throw new SimError("No connection " + con + " found in "
					+ anotherNode);
		}

		this.host.changedConnection(con);
		anotherNode.changedConnection(con);

		connections.remove(index);
	}

	/**
	 * Updates the state of current connections (i.e. tears down connections
	 * that are out of range).
	 */
	public void update() {
		for (int i = 0; i < this.connections.size();) {
			Connection con = this.connections.get(i);
			DTNHost anotherNode = con.getOtherNode(this.host);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherNode)) {
				removeConnectionByIndex(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * Returns true if another node is within radio range of this node and this
	 * node is also within radio range of the another node.
	 * 
	 * @param anotherHost
	 *            The another host
	 * @return True if the node is within range, false if not
	 */
	private boolean isWithinRange(DTNHost anotherHost) {
		double smallerRange = anotherHost.getTransmitRange();
		if (this.transmitRange < smallerRange) {
			smallerRange = this.transmitRange;
		}

		return this.host.getLocation().distance(anotherHost.getLocation()) <= smallerRange;
	}

	/**
	 * Returns true if the given DTNHost is connected to this host.
	 * 
	 * @param node
	 *            The other DTNHost to check
	 * @return True if the two hosts are connected
	 */
	private boolean isConnected(DTNHost node) {
		for (int i = 0; i < this.connections.size(); i++) {
			if (this.connections.get(i).getOtherNode(this.host) == node) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Notifies all the connection listeners about a change in connections.
	 * 
	 * @param type
	 *            Type of the change (e.g. {@link #CON_DOWN} )
	 * @param otherHost
	 *            The other host on the other end of the connection.
	 */
	private void notifyConnectionListeners(int type, DTNHost otherHost) {
		if (this.cListeners == null) {
			return;
		}
		for (ConnectionListener cl : this.cListeners) {
			switch (type) {
			case CON_UP:
				cl.hostsConnected(this.host, otherHost);
				break;
			case CON_DOWN:
				cl.hostsDisconnected(this.host, otherHost);
				break;
			default:
				assert false : type; // invalid type code
			}
		}
	}

	/**
	 * This method is called by the {@link ModuleCommunicationBus} when/if
	 * someone changes the scanning interval, transmit speed, or range
	 * 
	 * @param key
	 *            Identifier of the changed value
	 * @param newValue
	 *            New value for the variable
	 */
	public void moduleValueChanged(String key, Object newValue) {
		if (key.equals(SCAN_INTERVAL_ID)) {
			this.scanInterval = (Double) newValue;
		} else if (key.equals(SPEED_ID)) {
			this.transmitSpeed = (Integer) newValue;
		} else if (key.equals(RANGE_ID)) {
			this.transmitRange = (Double) newValue;
		} else {
			throw new SimError("Unexpected combus ID " + key);
		}
	}

	/**
	 * Returns a string representation of the object.
	 * 
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "net layer " + " of " + this.host + ". Connections: "
				+ this.connections;
	}

	/**
	 * Resets the static fields of the class
	 */
	public static void reset() {
		nextAddress = 0;
		rng = new Random(0);
	}

}
