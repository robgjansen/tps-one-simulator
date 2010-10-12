/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package input;

import core.DTNHost;
import core.World;

/**
 * External event for deleting a message.
 */

public class MessageDeleteEvent extends MessageEvent {
	/** is the delete caused by a drop (not "normal" removing) */
	private boolean drop;

	/**
	 * Creates a message delete event
	 * 
	 * @param host
	 *            Where to delete the message
	 * @param id
	 *            ID of the message
	 * @param time
	 *            Time when the message is deleted
	 */
	public MessageDeleteEvent(int host, String id, double time, boolean drop) {
		super(host, host, id, time);
		this.drop = drop;
	}

	/**
	 * Deletes the message
	 */
	public void processEvent(World world) {
		DTNHost host = world.getNodeByAddress(this.fromAddr);
		host.deleteMessage(id, drop);
	}

	public String toString() {
		return super.toString() + " [" + fromAddr + "] DELETE";
	}

}
