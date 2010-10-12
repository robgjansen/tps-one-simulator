/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package gui.playfield;

import core.Connection;
import core.Coord;
import core.DTNHost;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

/**
 * Visualization of a DTN Node
 * 
 */
public class NodeGraphic extends PlayFieldGraphic {
	private static boolean drawCoverage = true;
	private static boolean drawNodeName = true;
	private static boolean drawConnections = true;

	private static Color rangeColor = Color.GREEN;
	private static Color conColor = Color.BLACK;
	private static Color hostColor = Color.BLUE;
	private static Color hostNameColor = Color.BLUE;
	private static Color msgColor1 = Color.BLUE;
	private static Color msgColor2 = Color.GREEN;
	private static Color msgColor3 = Color.RED;

	private DTNHost node;

	public NodeGraphic(DTNHost node) {
		this.node = node;
	}

	@Override
	public void draw(Graphics2D g2) {
		drawHost(g2);
		drawMessages(g2);
	}

	/**
	 * Visualize node's location, radio range and connections
	 * 
	 * @param g2
	 *            The graphic context to draw to
	 */
	private void drawHost(Graphics2D g2) {
		Coord loc = node.getLocation();

		if (drawCoverage && node.isActive()) {
			double range = node.getTransmitRange();
			Ellipse2D.Double coverage;

			coverage = new Ellipse2D.Double(scale(loc.getX() - range),
					scale(loc.getY() - range), scale(range * 2),
					scale(range * 2));

			// draw the "range" circle
			g2.setColor(rangeColor);
			g2.draw(coverage);
		}

		if (drawConnections) {
			g2.setColor(conColor);
			Coord c1 = node.getLocation();
			ArrayList<Connection> conList = new ArrayList<Connection>();
			// create a copy to prevent concurrent modification exceptions
			conList.addAll(node.getConnections());
			for (Connection c : conList) {
				Coord c2 = c.getOtherNode(node).getLocation();

				g2.drawLine(scale(c1.getX()), scale(c1.getY()),
						scale(c2.getX()), scale(c2.getY()));
			}
		}

		g2.setColor(hostColor); // draw rectangle to host's location
		g2.drawRect(scale(loc.getX() - 1), scale(loc.getY() - 1), scale(2),
				scale(2));

		if (drawNodeName) {
			g2.setColor(hostNameColor);
			// Draw node's address next to it
			g2
					.drawString(node.toString(), scale(loc.getX()), scale(loc
							.getY()));
		}
	}

	/**
	 * Sets whether radio coverage of nodes should be drawn
	 * 
	 * @param draw
	 *            If true, radio coverage is drawn
	 */
	public static void setDrawCoverage(boolean draw) {
		drawCoverage = draw;
	}

	/**
	 * Sets whether node's name should be displayed
	 * 
	 * @param draw
	 *            If true, node's name is displayed
	 */
	public static void setDrawNodeName(boolean draw) {
		drawNodeName = draw;
	}

	/**
	 * Sets whether node's connections to other nodes should be drawn
	 * 
	 * @param draw
	 *            If true, node's connections to other nodes is drawn
	 */
	public static void setDrawConnections(boolean draw) {
		drawConnections = draw;
	}

	/**
	 * Visualize the messages this node is carrying
	 * 
	 * @param g2
	 *            The graphic context to draw to
	 */
	private void drawMessages(Graphics2D g2) {
		int nrofMessages = node.getNrofMessages();
		Coord loc = node.getLocation();

		drawBar(g2, loc, nrofMessages % 10, 1);
		drawBar(g2, loc, nrofMessages / 10, 2);
	}

	/**
	 * Draws a bar (stack of squares) next to a location
	 * 
	 * @param g2
	 *            The graphic context to draw to
	 * @param loc
	 *            The location where to draw
	 * @param nrof
	 *            How many squares in the stact
	 * @param col
	 *            Which column
	 */
	private void drawBar(Graphics2D g2, Coord loc, int nrof, int col) {
		final int BAR_HEIGHT = 5;
		final int BAR_WIDTH = 5;
		final int BAR_DISPLACEMENT = 2;

		// draws a stack of squares next loc
		for (int i = 1; i <= nrof; i++) {
			if (i % 2 == 0) { // use different color for every other msg
				g2.setColor(msgColor1);
			} else {
				if (col > 1) {
					g2.setColor(msgColor3);
				} else {
					g2.setColor(msgColor2);
				}
			}

			g2.fillRect(
					scale(loc.getX() - BAR_DISPLACEMENT - (BAR_WIDTH * col)),
					scale(loc.getY() - BAR_DISPLACEMENT - i * BAR_HEIGHT),
					scale(BAR_WIDTH), scale(BAR_HEIGHT));
		}

	}

}