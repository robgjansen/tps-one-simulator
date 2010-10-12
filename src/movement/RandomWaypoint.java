/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.*;

/**
 * Random waypoint movement model. Creates zig-zag paths within the simulation
 * area.
 */
public class RandomWaypoint extends MovementModel {
	/** how many waypoints should there be per path */
	private static final int PATH_LENGTH = 1;
	private Coord lastWaypoint;

	public RandomWaypoint(Settings settings) {
		super(settings);
	}

	private RandomWaypoint(RandomWaypoint rwp) {
		super(rwp);
	}

	/**
	 * Returns a possible (random) placement for a host
	 * 
	 * @return Random position on the map
	 */
	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		double x = rng.nextDouble() * getMaxX();
		double y = rng.nextDouble() * getMaxY();
		Coord c = new Coord(x, y);

		this.lastWaypoint = c;
		return c;
	}

	@Override
	public Path getPath() {
		Path p;
		p = new Path(generateSpeed());
		p.addWaypoint(lastWaypoint.clone());
		Coord c = lastWaypoint;

		for (int i = 0; i < PATH_LENGTH; i++) {
			c = new Coord(rng.nextDouble() * getMaxX(), rng.nextDouble()
					* getMaxY());
			p.addWaypoint(c);
		}

		this.lastWaypoint = c;
		return p;
	}

	@Override
	public RandomWaypoint replicate() {
		return new RandomWaypoint(this);
	}
}
