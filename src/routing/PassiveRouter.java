/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import core.Connection;
import core.Settings;

/**
 * Passive router that doesn't send anything unless commanded. This is useful
 * for external event -controlled routing or dummy nodes. For implementation
 * specifics, see MessageRouter class.
 */
public class PassiveRouter extends MessageRouter {

	public PassiveRouter(Settings s) {
		super(s);
	}

	/**
	 * Copy-constructor.
	 * 
	 * @param r
	 *            Router to copy the settings from.
	 */
	protected PassiveRouter(PassiveRouter r) {
		super(r);
	}

	@Override
	public void update() {
		// nothing to update..
	}

	@Override
	public void changedConnection(Connection con) {
		// allow group manager to keep track of who it has seen.
		this.notifyConnection(con);
	}

	@Override
	public MessageRouter replicate() {
		return new PassiveRouter(this);
	}
}
