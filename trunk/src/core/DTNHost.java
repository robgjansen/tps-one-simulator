/* 
 * Copyright 2008 TKK/ComNet, 2009 Rob Jansen
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import core.ConnectivityGrid.GridCell;
import movement.*;
import java.util.*;

import routing.*;

/**
 * A DTN capable host.
 */
public class DTNHost implements Comparable<DTNHost> {
	private Coord location; // where is the host
	private Coord destination; // where is it going
	private GridCell curCell; // current cell for cell-based optimization

	private NetworkLayer net;
	private MessageRouter router;
	private MovementModel movement;
	private Path path;
	private double speed;
	private double nextTimeToMove;
	private String name;
	private List<MessageListener> msgListeners;
	private List<MovementListener> movListeners;
	private ModuleCommunicationBus comBus;
	private AnonymousGroupManager gm;

	/**
	 * Creates a new DTNHost.
	 * 
	 * @param conLs
	 *            Connection listeners
	 * @param msgLs
	 *            Message listeners
	 * @param groupId
	 *            GroupID of this host
	 * @param comBus
	 *            Module communication bus object
	 * @param mmProto
	 *            Prototype of the movement model of this host
	 * @param mRouterProto
	 *            Prototype of the message router of this host
	 */
	public DTNHost(List<ConnectionListener> conLs, List<MessageListener> msgLs,
			List<MovementListener> movLs, String groupId,
			ModuleCommunicationBus comBus, MovementModel mmProto,
			MessageRouter mRouterProto) {
		this.comBus = comBus;
		this.net = new NetworkLayer(this, conLs, comBus);
		this.name = groupId + net.getAddress();
		this.msgListeners = msgLs;
		this.movListeners = movLs;

		// create instances by replicating the prototype
		this.movement = mmProto.replicate();
		setRouter(mRouterProto.replicate());

		this.location = movement.getInitialLocation();

		this.nextTimeToMove = movement.nextPathAvailable();
		this.path = null;

		if (movLs != null) { // inform movement listeners about the location
			for (MovementListener l : movLs) {
				l.initialLocation(this, this.location);
			}
		}

		// initialize the group manager
		gm = new AnonymousGroupManager(this);
	}

	public AnonymousGroupManager getGroupManager() {
		return gm;
	}

	/**
	 * Returns true if this node is active (false if not)
	 * 
	 * @return true if this node is active (false if not)
	 */
	public boolean isActive() {
		return this.movement.isActive();
	}

	/**
	 * Set a router for this host
	 * 
	 * @param router
	 *            The router to set
	 */
	private void setRouter(MessageRouter router) {
		router.init(this, msgListeners);
		this.router = router;
	}

	/**
	 * Returns the router of this host
	 * 
	 * @return the router of this host
	 */
	public MessageRouter getRouter() {
		return this.router;
	}

	/**
	 * Returns the network-layer address of this host.
	 */
	public int getAddress() {
		return this.net.getAddress();
	}

	/**
	 * Returns the transmit range of this host's radio
	 * 
	 * @see NetworkLayer#getTransmitRange()
	 */
	public double getTransmitRange() {
		return this.net.getTransmitRange();
	}

	/**
	 * Returns the transmit speed of this host's radio
	 * 
	 * @see NetworkLayer#getTransmitSpeed()
	 */
	public int getTransmitSpeed() {
		return this.net.getTransmitSpeed();
	}

	/**
	 * Returns this hosts's ModuleCommunicationBus
	 * 
	 * @return this hosts's ModuleCommunicationBus
	 */
	public ModuleCommunicationBus getComBus() {
		return this.comBus;
	}

	/**
	 * Informs the router of this host about state change in a connection
	 * object.
	 * 
	 * @param con
	 *            The connection object whose state changed
	 */
	public void changedConnection(Connection con) {
		this.router.changedConnection(con);
	}

	/**
	 * Returns a list of connections this host has with other hosts
	 * 
	 * @return a list of connections this host has with other hosts
	 */
	public List<Connection> getConnections() {
		return this.net.getConnections();
	}

	/**
	 * Returns the current location of this host.
	 * 
	 * @return The location
	 */
	public Coord getLocation() {
		return this.location;
	}

	/**
	 * Returns the Path this node is currently traveling or null if no path is
	 * in use at the moment.
	 * 
	 * @return The path this node is traveling
	 */
	public Path getPath() {
		return this.path;
	}

	/**
	 * Return the "current cell" set by the setCurCell method (for cell based
	 * connection checking).
	 * 
	 * @return The current cell
	 */
	public GridCell getCurCell() {
		return this.curCell;
	}

	/**
	 * Sets the "current cell" (for cell based connection checking).
	 * 
	 * @param c
	 *            The cell to set
	 * @see #getCurCell()
	 */
	public void setCurCell(GridCell c) {
		this.curCell = c;
	}

	/**
	 * Sets the Node's location overriding any location set by movement model
	 * 
	 * @param location
	 *            The location to set
	 */
	public void setLocation(Coord location) {
		this.location = location.clone();
	}

	/**
	 * Sets the Node's name overriding the default name (groupId + netAddress)
	 * 
	 * @param name
	 *            The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the messages in a collection.
	 * 
	 * @return Messages in a collection
	 */
	public Collection<Message> getMessageCollection() {
		return this.router.getMessageCollection();
	}

	/**
	 * Returns the number of messages this node is carrying.
	 * 
	 * @return How many messages the node is carrying currently.
	 */
	public int getNrofMessages() {
		return this.router.getNrofMessages();
	}

	/**
	 * Returns the buffer occupancy percentage. Occupancy is 0 for empty buffer
	 * but can be over 100 if a created message is bigger than buffer space that
	 * could be freed.
	 * 
	 * @return Buffer occupancy percentage
	 */
	public double getBufferOccupancy() {
		double bSize = router.getBufferSize();
		double freeBuffer = router.getFreeBufferSize();
		return 100 * ((bSize - freeBuffer) / bSize);
	}

	/**
	 * Returns routing info of this host's router.
	 * 
	 * @return The routing info.
	 */
	public RoutingInfo getRoutingInfo() {
		return this.router.getRoutingInfo();
	}

	/**
	 * Updates node's network layer and router.
	 */
	public void update() {
		if (!isActive()) {
			return;
		}

		this.net.update();
		this.router.update();
	}

	/**
	 * Moves the node towards the next waypoint or waits if it is not time to
	 * move yet
	 * 
	 * @param timeIncrement
	 *            How long time the node moves
	 */
	public void move(double timeIncrement) {
		double possibleMovement;
		double distance;
		double dx, dy;

		if (!isActive() || SimClock.getTime() < this.nextTimeToMove) {
			return;
		}
		if (this.destination == null) {
			if (!setNextWaypoint()) {
				return;
			}
		}

		possibleMovement = timeIncrement * speed;
		distance = this.location.distance(this.destination);

		while (possibleMovement >= distance) {
			// node can move past its next destination
			this.location.setLocation(this.destination); // snap to destination
			possibleMovement -= distance;
			if (!setNextWaypoint()) { // get a new waypoint
				return; // no more waypoints left
			}
			distance = this.location.distance(this.destination);
		}

		// move towards the point for possibleMovement amount
		dx = (possibleMovement / distance)
				* (this.destination.getX() - this.location.getX());
		dy = (possibleMovement / distance)
				* (this.destination.getY() - this.location.getY());
		this.location.translate(dx, dy);
	}

	/**
	 * Sets the next destination and speed to correspond the next waypoint on
	 * the path.
	 * 
	 * @return True if there was a next waypoint to set, false if node still
	 *         should wait
	 */
	private boolean setNextWaypoint() {
		if (path == null) {
			path = movement.getPath();
		}

		if (path == null || !path.hasNext()) {
			this.nextTimeToMove = movement.nextPathAvailable();
			this.path = null;
			return false;
		}

		this.destination = path.getNextWaypoint();
		this.speed = path.getSpeed();

		if (this.movListeners != null) {
			for (MovementListener l : this.movListeners) {
				l.newDestination(this, this.destination, this.speed);
			}
		}

		return true;
	}

	/**
	 * Creates a connection to another host (if it is within range)
	 * 
	 * @param anotherHost
	 */
	public void connect(DTNHost anotherHost) {
		if (!isActive() || this == anotherHost) {
			return; // cannot connect self
		} else {
			net.connect(anotherHost);
		}
	}

	/**
	 * Forces this host to connect/disconnect to/from another host. If the
	 * connection to set up already exists, or the connection to destroy doesn't
	 * exist, nothing happens.
	 * 
	 * @param anotherHost
	 *            The host to connect to or disconnect from
	 * @param up
	 *            If true, the a new connection is created, if false, existing
	 *            connection is destroyed.
	 */
	public void forceConnection(DTNHost anotherHost, boolean up) {
		if (up) {
			this.net.createConnection(anotherHost);
		} else {
			this.net.destroyConnection(anotherHost);
		}
	}

	/**
	 * Sends a message from this host to another host
	 * 
	 * @param id
	 *            Identifier of the message
	 * @param to
	 *            Host the message should be sent to
	 */
	public void sendMessage(String id, DTNHost to) {
		this.router.sendMessage(id, to);
	}

	/**
	 * Start receiving a message from another host
	 * 
	 * @param m
	 *            The message
	 * @param from
	 *            Who the message is from
	 * @return The value returned by
	 *         {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int receiveMessage(Message m, DTNHost from) {
		int retVal = this.router.receiveMessage(m, from);

		if (retVal == MessageRouter.RCV_OK) {
			m.addNodeOnPath(this); // add this node on the messages path
		}

		return retVal;
	}

	/**
	 * Requests for deliverable message from this host to be sent trough a
	 * connection.
	 * 
	 * @param con
	 *            The connection to send the messages trough
	 * @return True if this host started a transfer, false if not
	 */
	public boolean requestDeliverableMessages(Connection con) {
		return this.router.requestDeliverableMessages(con);
	}

	/**
	 * Informs the host that a message was successfully transferred.
	 * 
	 * @param id
	 *            Identifier of the message
	 * @param from
	 *            From who the message was from
	 */
	public void messageTransferred(String id, DTNHost from) {
		this.router.messageTransferred(id, from);
	}

	/**
	 * Informs the host that a message transfer was aborted.
	 * 
	 * @param id
	 *            Identifier of the message
	 * @param from
	 *            From who the message was from
	 * @param bytesRemaining
	 *            Nrof bytes that were left before the transfer would have been
	 *            ready; or -1 if the number of bytes is not known
	 */
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		this.router.messageAborted(id, from, bytesRemaining);
	}

	/**
	 * Creates a new message to this host's router
	 * 
	 * @param m
	 *            The message to create
	 */
	public void createNewMessage(Message m) {
		this.router.createNewMessage(m);
	}

	/**
	 * Deletes a message from this host
	 * 
	 * @param id
	 *            Identifier of the message
	 * @param drop
	 *            True if the message is deleted because of "dropping" (e.g.
	 *            buffer is full) or false if it was deleted for some other
	 *            reason (e.g. the message got delivered to final destination).
	 *            This effects the way the removing is reported to the message
	 *            listeners.
	 */
	public void deleteMessage(String id, boolean drop) {
		this.router.deleteMessage(id, drop);
	}

	/**
	 * Returns a string presentation of the host.
	 * 
	 * @return Host's name
	 */
	public String toString() {
		return name;
	}

	/**
	 * Checks if a host is the same as this host by comparing the object
	 * reference
	 * 
	 * @param otherHost
	 *            The other host
	 * @return True if the hosts objects are the same object
	 */
	public boolean equals(DTNHost otherHost) {
		return this == otherHost;
	}

	/**
	 * Compares two DTNHosts by their addresses.
	 * 
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(DTNHost h) {
		return this.getAddress() - h.getAddress();
	}
}
