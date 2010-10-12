/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package test;

import routing.PassiveRouter;
import core.*;

/**
 * A test stub of DTNHost for testing. All fields are public so they can be
 * easily read from test cases.
 */
public class TestDTNHost extends DTNHost {
	public double lastUpdate = 0;
	public int nrofConnect = 0;
	public int nrofUpdate = 0;
	public Message recvMessage;
	public DTNHost recvFrom;
	public String abortedId;
	public DTNHost abortedFrom;
	public int abortedBytesRemaining;

	public String transferredId;
	public DTNHost transferredFrom;

	public TestDTNHost(ModuleCommunicationBus comBus) {
		super(null, null, null, "TST", comBus, new StationaryMovement(
				new Coord(0, 0)), new PassiveRouter(new TestSettings()));
	}

	@Override
	public void connect(DTNHost anotherHost) {
		this.nrofConnect++;
	}

	@Override
	public void update() {
		this.nrofUpdate++;
		this.lastUpdate = SimClock.getTime();
	}

	@Override
	public int receiveMessage(Message m, DTNHost from) {
		this.recvMessage = m;
		this.recvFrom = from;
		return routing.MessageRouter.RCV_OK;
	}

	@Override
	public void messageAborted(String id, DTNHost from, int bytesRemaining) {
		this.abortedId = id;
		this.abortedFrom = from;
		this.abortedBytesRemaining = bytesRemaining;
	}

	@Override
	public void messageTransferred(String id, DTNHost from) {
		this.transferredId = id;
		this.transferredFrom = from;
	}
}
