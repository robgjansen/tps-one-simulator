/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <P>
 * Overlay grid of the world where each node is put on a cell depending of its
 * location. This is used in cell-based optimization of connecting the nodes.
 * </P>
 * 
 * <P>
 * The idea in short:<BR>
 * Instead of checking for every node if some of the other nodes are close
 * enough (this approach obviously doesn't scale) we check only nodes that are
 * "close enough" to be possibly connected. Being close enough is determined by
 * keeping track of the approximate location of the nodes by storing them in
 * overlay grid's cells and updating the cell information every time the nodes
 * move. If two nodes are in the same cell or in neighboring cells, they have a
 * chance of being close enough for connection. Then only that subset of nodes
 * is checked for possible connectivity.
 * </P>
 * <P>
 * <strong>Note:</strong> this class does NOT support negative coordinates.
 * Also, it makes sense to normalize the coordinates to start from zero to
 * conserve memory.
 */
public class ConnectivityGrid {
	private GridCell[][] cells;
	private int cellSize;
	private int rows;
	private int cols;

	/**
	 * Creates a new overlay connectivity grid
	 * 
	 * @param worldSizeX
	 *            Width of the world (biggest possible x coordinate)
	 * @param worldSizeY
	 *            Height of the world (biggest possible y coordinate)
	 * @param cellSize
	 *            Cell's edge's length (must be larger than the largest radio
	 *            coverage's diameter)
	 */
	public ConnectivityGrid(int worldSizeX, int worldSizeY, int cellSize) {
		this.rows = worldSizeY / cellSize + 1;
		this.cols = worldSizeX / cellSize + 1;
		// leave empty cells on both sides to make neighbor search easier
		this.cells = new GridCell[rows + 2][cols + 2];
		this.cellSize = cellSize;

		for (int i = 0; i < rows + 2; i++) {
			for (int j = 0; j < cols + 2; j++) {
				this.cells[i][j] = new GridCell();
			}
		}
	}

	/**
	 * Adds nodes to overlay grid
	 * 
	 * @param nodes
	 *            Collection of nodes to add
	 */
	public void addNodes(Collection<DTNHost> nodes) {
		for (DTNHost n : nodes) {
			GridCell c = cellFromCoord(n.getLocation());
			c.addNode(n);
			n.setCurCell(c);
		}
	}

	/**
	 * Checks and updates (if necessary) node's position in the grid
	 * 
	 * @param node
	 *            The node to update
	 */
	public void updateLocation(DTNHost node) {
		GridCell oldCell = node.getCurCell();
		GridCell newCell = cellFromCoord(node.getLocation());

		if (newCell != oldCell) {
			oldCell.moveNode(node, newCell);
			node.setCurCell(newCell);
		}
	}

	/**
	 * Connects a node to all nodes that are in the same cell or in adjacent
	 * cells. Good algorithm for small (less than 2k) amounts of nodes or highly
	 * clustered nodes. For larger amounts of nodes connectAllNearNodes() could
	 * be a better choice.
	 * 
	 * @param node
	 *            Node to connect
	 * @see #connectAllNearNodes()
	 */
	public void connectToNearNodes(DTNHost node) {
		GridCell[] neighbors = getNeighborCellsByCoord(node.getLocation());
		for (int i = 0; i < neighbors.length; i++) {
			List<DTNHost> l = neighbors[i].getNodes();
			for (int j = 0; j < l.size(); j++) {
				node.connect(l.get(j));
			}
		}
	}

	/**
	 * Connects all nodes, that are in adjacent cells, to each other. Good
	 * algorithm for large amount of nodes (more than 2k) that are relatively
	 * evenly distributed, lousy for small amounts or highly clustered nodes.
	 * For smaller amounts, use connectToNearNodes()
	 * 
	 * @see #connectToNearNodes(DTNHost)
	 */
	public void connectAllNearNodes() {
		for (int i = 1; i <= this.rows; i++) {
			for (int j = 1; j <= this.cols; j++) {
				List<DTNHost> nodes = this.cells[i][j].getNodes();
				if (nodes.size() == 0) {
					continue; // skip empty cells
				}
				GridCell[] neighbors = getNeighborCells(i, j);
				for (int k = 0, n = nodes.size(); k < n; k++) {
					connectNodesInCells(nodes.get(k), neighbors);
				}
			}
		}
	}

	/**
	 * Connects a node to all other nodes in a cell array
	 * 
	 * @param node
	 *            Node to connect
	 * @param cells
	 *            Cell array where the other nodes are looked from
	 */
	private void connectNodesInCells(DTNHost node, GridCell[] cells) {
		for (int i = 0; i < cells.length; i++) {
			List<DTNHost> list = cells[i].getNodes();
			for (int j = 0, n = list.size(); j < n; j++) {
				node.connect(list.get(j));
			}
		}
	}

	private GridCell[] getNeighborCellsByCoord(Coord c) {
		// +1 due empty cells on both sides of the matrix
		int row = (int) (c.getY() / cellSize) + 1;
		int col = (int) (c.getX() / cellSize) + 1;
		return getNeighborCells(row, col);
	}

	/**
	 * Returns an array of Cells that contains the neighbors of a certain cell
	 * and the cell itself.
	 * 
	 * @param row
	 *            Row index of the cell
	 * @param col
	 *            Column index of the cell
	 * @return Array of neighboring Cells
	 */
	private GridCell[] getNeighborCells(int row, int col) {
		return new GridCell[] { cells[row - 1][col - 1],
				cells[row - 1][col],
				cells[row - 1][col + 1],// 1st row
				cells[row][col - 1], cells[row][col],
				cells[row][col + 1],// 2nd row
				cells[row + 1][col - 1], cells[row + 1][col],
				cells[row + 1][col + 1] // 3rd row
		};
	}

	private GridCell cellFromCoord(Coord c) {
		// +1 due empty cells on both sides of the matrix
		int row = (int) (c.getY() / cellSize) + 1;
		int col = (int) (c.getX() / cellSize) + 1;

		assert row > 0 && row <= rows && col > 0 && col <= cols : "Location "
				+ c + " is out of world's bounds";

		return this.cells[row][col];
	}

	/**
	 * Returns a string representation of the ConnectivityCells object
	 * 
	 * @return a string representation of the ConnectivityCells object
	 */
	public String toString() {
		return getClass().getSimpleName() + " of size " + this.cols + "x"
				+ this.rows + ", cell size=" + this.cellSize;
	}

	/**
	 * A single cell in the cell grid. Contains the nodes that are currently in
	 * that part of the grid.
	 */
	public class GridCell {
		// how large array is initially chosen
		private static final int EXPECTED_NODE_COUNT = 5;
		private ArrayList<DTNHost> nodes;

		private GridCell() {
			this.nodes = new ArrayList<DTNHost>(EXPECTED_NODE_COUNT);
		}

		/**
		 * Returns a list of of nodes in this cell
		 * 
		 * @return a list of of nodes in this cell
		 */
		public List<DTNHost> getNodes() {
			return this.nodes;
		}

		/**
		 * Adds a node to this cell
		 * 
		 * @param node
		 *            The node to add
		 */
		public void addNode(DTNHost node) {
			this.nodes.add(node);
		}

		/**
		 * Moves a node in a Cell to another Cell
		 * 
		 * @param node
		 *            The node to move
		 * @param to
		 *            The cell where the node should be moved to
		 */
		public void moveNode(DTNHost node, GridCell to) {
			to.addNode(node);
			boolean removeOk = this.nodes.remove(node);
			assert removeOk : "Node " + node + " not found from cell with "
					+ nodes.toString();
		}

		/**
		 * Returns a string representation of the cell
		 * 
		 * @return a string representation of the cell
		 */
		public String toString() {
			return getClass().getSimpleName() + " with " + this.nodes.size()
					+ " nodes :" + this.nodes;
		}
	}
}
