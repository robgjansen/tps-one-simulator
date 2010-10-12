/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import routing.MessageRouter;

/**
 * A connection between two DTN nodes.
 */
public class Connection {
	private DTNHost toNode;
	private DTNHost fromNode;
	private DTNHost msgFromNode;
	private boolean isUp;
	private int speed;
	private Message msgOnFly;
	private double transferDoneTime;
	/** how many bytes this connection has transferred */
	private int bytesTransferred;

	/**
	 * Creates a new connection between nodes and sets the connection state to
	 * "up".
	 * 
	 * @param fromNode
	 *            The node that initiated the connection
	 * @param toNode
	 *            The node in the other side of the connection
	 * @param connectionSpeed
	 *            Transfer speed of the connection (Bps)
	 */
	public Connection(DTNHost fromNode, DTNHost toNode, int connectionSpeed) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.speed = connectionSpeed;
		this.isUp = true;
		this.transferDoneTime = 0;
		this.bytesTransferred = 0;
	}

	/**
	 * Returns true if the connection is up
	 * 
	 * @return state of the connection
	 */
	public boolean isUp() {
		return this.isUp;
	}

	/**
	 * Returns true if the given node is the initiator of the connection, false
	 * otherwise
	 * 
	 * @param node
	 *            The node to check
	 * @return true if the given node is the initiator of the connection
	 */
	public boolean isInitiator(DTNHost node) {
		return node == this.fromNode;
	}

	/**
	 * Sets the state of the connection.
	 * 
	 * @param state
	 *            True if the connection is up, false if not
	 */
	public void setUpState(boolean state) {
		this.isUp = state;
	}

	/**
	 * Sets a message that this connection is currently transferring. If message
	 * passing is controlled by external events, this method is not needed (but
	 * then e.g. {@link #finalizeTransfer()} and {@link #isMessageTransferred()}
	 * will not work either). Only a one message at a time can be transferred
	 * using one connection.
	 * 
	 * @param m
	 *            The message
	 * @return The value returned by
	 *         {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int startTransfer(DTNHost from, Message m) {
		assert this.msgOnFly == null : "Already transferring " + this.msgOnFly
				+ " from " + this.msgFromNode + " to "
				+ this.getOtherNode(this.msgFromNode) + ". Can't "
				+ "start transfer of " + m + " from " + from;

		this.msgFromNode = from;
		Message newMessage = m.replicate();
		int retVal = getOtherNode(from).receiveMessage(newMessage, from);

		if (retVal == MessageRouter.RCV_OK) {
			this.msgOnFly = newMessage;
			this.transferDoneTime = SimClock.getTime() + (1.0 * m.getSize())
					/ this.speed;
		}

		return retVal;
	}

	/**
	 * Aborts the transfer of the currently transferred message.
	 */
	public void abortTransfer() {
		assert msgOnFly != null : "No message to abort at " + msgFromNode;
		int bytesRemaining = getRemainingByteCount();

		this.bytesTransferred += msgOnFly.getSize() - bytesRemaining;

		getOtherNode(msgFromNode).messageAborted(this.msgOnFly.getId(),
				msgFromNode, bytesRemaining);
		clearMsgOnFly();

		this.transferDoneTime = 0; // transfer is "ready"
	}

	/**
	 * Returns the time when the current transfer is done (if the connection
	 * doesn't break and the transfer is not aborted).
	 * 
	 * @return the time when the current transfer is done
	 */
	public double getTransferDoneTime() {
		return this.transferDoneTime;
	}

	/**
	 * Returns the amount of bytes to be transferred before ongoing transfer is
	 * ready or 0 if there's no ongoing transfer or it has finished already
	 * 
	 * @return the amount of bytes to be transferred
	 */
	public int getRemainingByteCount() {
		int remaining;

		if (msgOnFly == null) {
			return 0;
		}

		remaining = (int) ((this.transferDoneTime - SimClock.getTime()) * this.speed);

		return (remaining > 0 ? remaining : 0);
	}

	/**
	 * Clears the message that is currently being transferred. Calls to
	 * {@link #getMessage()} will return null after this.
	 */
	private void clearMsgOnFly() {
		this.msgOnFly = null;
		this.msgFromNode = null;
	}

	/**
	 * Finalizes the transfer of the currently transferred message. The message
	 * that was being transferred can <STRONG>not</STRONG> be retrieved from
	 * this connections after calling this method (using {@link #getMessage()}).
	 */
	public void finalizeTransfer() {
		assert this.msgOnFly != null : "Nothing to finalize in " + this;
		this.bytesTransferred += msgOnFly.getSize();

		getOtherNode(msgFromNode).messageTransferred(this.msgOnFly.getId(),
				msgFromNode);
		clearMsgOnFly();
	}

	/**
	 * Returns true if the current message transfer is done
	 * 
	 * @return True if the transfer is done, false if not
	 */
	public boolean isMessageTransferred() {
		return getRemainingByteCount() == 0;
	}

	/**
	 * Returns true if the connection is ready to transfer a message (connection
	 * is up and there is no message being transferred).
	 * 
	 * @return true if the connection is ready to transfer a message
	 */
	public boolean isReadyForTransfer() {
		return this.isUp && this.msgOnFly == null;
	}

	/**
	 * Gets the message that this connection is currently transferring.
	 * 
	 * @return The message or null if no message is being transferred
	 */
	public Message getMessage() {
		return this.msgOnFly;
	}

	/**
	 * Returns the total amount of bytes this connection has transferred so far
	 * (including all transfers).
	 */
	public int getTotalBytesTransferred() {
		if (this.msgOnFly == null) {
			return this.bytesTransferred;
		} else {
			if (isMessageTransferred()) {
				return this.bytesTransferred + this.msgOnFly.getSize();
			} else {
				return this.bytesTransferred
						+ (msgOnFly.getSize() - getRemainingByteCount());
			}
		}
	}

	/**
	 * Returns the node in the other end of the connection
	 * 
	 * @param node
	 *            The node in this end of the connection
	 * @return The requested node
	 */
	public DTNHost getOtherNode(DTNHost node) {
		if (node == this.fromNode) {
			return this.toNode;
		} else {
			return this.fromNode;
		}
	}

	/**
	 * Returns a String presentation of the connection.
	 */
	public String toString() {
		return fromNode
				+ "<->"
				+ toNode
				+ " ("
				+ speed
				+ "Bps) is "
				+ (isUp() ? "up" : "down")
				+ (this.msgOnFly != null ? " transferring " + this.msgOnFly
						+ " from " + this.msgFromNode + " until "
						+ this.transferDoneTime : "");
	}

}
