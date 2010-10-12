/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report information about all delivered messages. Messages created during the
 * warm up period are ignored. For output syntax, see {@link #HEADER}.
 */
public class DeliveredMessagesReport extends Report implements MessageListener {
	public static String HEADER = "# time  ID  size  hopcount  deliveryTime  remainingTtl  isResponse";

	/**
	 * Constructor.
	 */
	public DeliveredMessagesReport() {
		init();
	}

	@Override
	public void init() {
		super.init();
		write(HEADER);
	}

	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
		if (!isWarmupID(m.getId()) && firstDelivery) {
			int ttl = m.getTtl();
			write(format(getSimTime()) + " " + m.getId() + " " + m.getSize()
					+ " " + m.getHopCount() + " "
					+ format(getSimTime() - m.getCreationTime())
					+ (ttl != Integer.MAX_VALUE ? " " + ttl : " n/a")
					+ (m.isResponse() ? " Y" : " N"));
		}
	}

	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
		}
	}

	// nothing to implement for the rest
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
	}

	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
	}

	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
	}

	@Override
	public void done() {
		super.done();
	}
}
