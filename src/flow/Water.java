package flow;

import java.awt.image.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.Color;

/**
 * <p>Water data class.</p>
 * <p>Provides functionality to access and manipulate water 
 * data in the simulation.</p>
 * <p>This is a Model class (of the MVC design pattern).
 * It deals with data only, independent of the UI.</p>
 * 
 * @author avk
 */
public class Water {

	BufferedImage img;
	int[][] depth;
	Terrain terrain;

	// Keep track of water to check concurrency bugs
	AtomicInteger waterAdded;
	AtomicInteger waterRemoved;
	AtomicInteger waterCount;

	// Water color
	final static float MAX_HUE = 234f/360f;
	final static float MIN_HUE = 196f/360f;

	/**
	 * <p>Initializes terrain, depth, image.</p>
	 * 
	 * @param t Terrain object to simulate water on
	 */
	Water (Terrain t) {
		terrain = t;

		/* Water conservation testing    |
		 * Uncomment for debugging       v
		 */
		//waterAdded = new AtomicInteger(0);
		//waterRemoved = new AtomicInteger(0);
		//waterCount = new AtomicInteger(0);

		// filled with zeros by default
		depth = new int[terrain.dimx()][terrain.dimy()];

		// transparent image
		img = new BufferedImage(terrain.dimx(), terrain.dimy(), BufferedImage.TYPE_INT_ARGB);
	}

	/**
	 * <p>Accessor for water image.</p>
	 * @return water image
	 */
	BufferedImage getImage() {
		return img;
	}

	/**
	 * <p>Accessor for water added.</p>
	 * @return Number of water units added over whole simulation
	 */
	int waterAdded() {
		return waterAdded.get();
	}

	/**
	 * <p>Accessor for water removed.</p>
	 * @return Number of water units removed over whole simulation
	 */
	int waterRemoved() {
		return waterRemoved.get();
	}

	/**
	 * <p>Count up water on grid.</p>
	 * @return Number of water units currently on the grid
	 */
	int waterCount() {
		synchronized (depth) {
			waterCount.set(0);
			for(int i=0; i<terrain.dimx(); i++) {
				for(int j=0; j<terrain.dimy(); j++) {
					if(depth[i][j] > 0) {
						for(int d=0; d<depth[i][j]; d++) {
							waterCount.getAndIncrement();
						}
					}
				}
			}
			return waterCount.get();
		}
	}

	/**
	 * <p>Set zero depth everywhere.</p>
	 */
	void reset() {
		for(int i=0; i<terrain.dimx(); i++) {
			for(int j=0; j<terrain.dimy(); j++) {
				/* Water conservation testing    |
				 * Uncomment for debugging       v
				 */
				//if (depth[i][j] > 0) {
				//	for (int d=0; d<depth[i][j]; d++) {
				//		waterRemoved.getAndIncrement();
				//	}
				//}

				depth[i][j] = 0;
			}
		}

		// set to transparent
		img = new BufferedImage(terrain.dimx(), terrain.dimy(), BufferedImage.TYPE_INT_ARGB);
	}

	/**
	 * <p>Adjusts the depth of water by specified amount, at given coords.</p>
	 * <p>{@link flowS} is this with synchronization.</p>
	 * 
	 * @param change Amount of water to add (or remove, if negative), value of 0 sets depth to 0
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 */
	void flow(int change, int x, int y) {
		if (change==0) {
			depth[x][y] = 0;
		}
		else {
			depth[x][y] += change;
		}
	}

	/**
	 * <p>Calls {@link flow}, synchronized on the <code>depth</code> array.</p>
	 * <p>{@link flow} has a check-act pattern and a read-modify-write, 
	 * so for any point that may be accessed concurrently, there needs to 
	 * be synchronization protection.</p>
	 * 
	 * @param change Amount of water to add (or remove, if negative)
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 */
	void flowS(int change, int x, int y) {
		synchronized (depth) {
			flow(change,x,y);
		}
	}

	/**
	 * <p>Adds water with given dimensions at given coords.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 * @param d depth of water drop
	 * @param s spread of water drop (num pixels to spread out from center)
	 */
	void add(int x, int y, int d, int s) {
		for (int i=-s; i<=s; i++) {
			for (int j=-s; j<=s; j++) {
				depth[x+i][y+j] = d;
				color(x+i,y+j);

				/* Water conservation testing    |
				 * Uncomment for debugging       v
				 */
				//for (int a=0; a<d; a++) {
				//	waterAdded.getAndIncrement();
				//}
			}
		}
	}

	/**
	 * <p>Sets color of water at given coords.</p>
	 * <p>Determines the color for the point based on the depth of
	 * water at the point. Scales the depth to a hue range, and
	 * converts HSB or RGB to set for the image.</p>
	 * <p>{@link colorS} is this with synchronization.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 */
	void color(int x, int y) {
		int maxDepth = 6; // Deepest in hue range, shallowest is 1
		// (max depth is kinda randomly chosen atm)
		float h; // hue

		// Calculate Hue value for this point
		if(depth[x][y] <= maxDepth) {
			h = (MAX_HUE - MIN_HUE)*(depth[x][y] - 1)/(maxDepth - 1) + MIN_HUE;
		}
		else {
			h = MAX_HUE;
		}

		// Use s=100, b=75
		int rgb = Color.HSBtoRGB(h, 1f, 0.75f);

		// https://dyclassroom.com/image-processing-project/how-to-get-and-set-pixel-value-in-java
		if (depth[x][y]==0) {
			// Empty: A=0 R=0 G=0 B=0
			img.setRGB(x, y, 0);
		}
		else {
			// Blue: A=255 RGB as calculated
			int p = (255<<24) | rgb;
			img.setRGB(x, y, p);
		}
	}

	/**
	 * <p>Calls {@link color}, synchronized on the <code>depth</code> array.</p>
	 * <p>{@link color} has a check-act pattern, so for any point that may be 
	 * accessed concurrently, there needs to be synchronization protection.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 */
	void colorS(int x, int y) {
		synchronized (depth) {
			color(x,y);
		}
	}

	/**
	 * <p>Clears water from point.</p>
	 * <p>Used to simulate water flowing off the edge of the
	 * grid. Sets depth to zero with a call to {@link flow}, 
	 * and color transparent with call to {@link color}.</p>
	 * <p>{@link updateEdgeS} is this with synchronization.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 */
	void updateEdge(int x, int y) {
		/* Water conservation testing    |
		 * Uncomment for debugging       v
		 */
		//if (depth[x][y] > 0) {
		//	for (int i=0; i<depth[x][y]; i++) {
		//		waterRemoved.getAndIncrement();
		//	}
		//}

		flow(0, x, y);
		color(x, y);
	}

	/**
	 * <p>Calls {@link updateEdge}, synchronized on the <code>depth</code> array.</p>
	 * <p>{@link updateEdge} is changing the value of a <code>depth</code> element, 
	 * so for any point that may be accessed concurrently, there needs to be 
	 * synchronization protection.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 */
	void updateEdgeS(int x, int y) {
		synchronized (depth) {
			updateEdge(x,y);
		}
	}

	/**
	 * <p>Moves water to the lowest neighboring point.</p>
	 * <p>{@link updateS} is this with synchronization.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 */
	void update(int x, int y) {
		int[] nextPt = new int[2];

		if (depth[x][y] != 0) {
			findLowest(x, y, nextPt);

			if (nextPt[0]<0) { return; } // no water flow
			flow(-1, x, y); // water out
			flow(1, nextPt[0], nextPt[1]); // water in

			// update color
			color(x, y);
			color(nextPt[0], nextPt[1]);
		}
	}

	/**
	 * <p>Same function as {@link update}, synchronized on the <code>depth</code> array.</p>
	 * <p>{@link update} has many reads, condition checks, and writes to <code>depth</code> 
	 * array. To prevent concurrency errors especially bad interleavings, if the point being
	 * analyzed, or any of its neighbors, are accessed concurrently, this method needs
	 * to be atomic.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 */
	void updateS(int x, int y) {
		synchronized (depth) {
			int[] nextPt = new int[2];

			if (depth[x][y] != 0) {
				findLowestS(x, y, nextPt);
				// This must be synchronized, so can't just call update()

				if (nextPt[0]<0) { return; } // no water flow
				flow(-1, x, y); // water out
				flow(1, nextPt[0], nextPt[1]); // water in

				// update color
				color(x, y);
				color(nextPt[0], nextPt[1]);
			}
		}
	}

	/**
	 * <p>Finds lowest neighboring point.</p>
	 * <p>{@link findLowestS} is this with synchronization.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 * @param c coords of lowest point (negative value indicates that none are lower)
	 */
	private void findLowest(int x, int y, int[] c) {

		// set initial min to surface of current point
		float min = terrain.height[x][y] + 0.01f*depth[x][y];

		// surrounding surface values
		float[] s = {
				terrain.height[x-1][y-1] + 0.01f*depth[x-1][y-1],
				terrain.height[x-1][y] + 0.01f*depth[x-1][y],
				terrain.height[x-1][y+1] + 0.01f*depth[x-1][y+1],
				terrain.height[x][y-1] + 0.01f*depth[x][y-1],
				terrain.height[x][y+1] + 0.01f*depth[x][y+1],
				terrain.height[x+1][y-1] + 0.01f*depth[x+1][y-1],
				terrain.height[x+1][y] + 0.01f*depth[x+1][y],
				terrain.height[x+1][y+1] + 0.01f*depth[x+1][y+1]
		}; // order: top to bottom, left to right

		int idxMin = -1; // index in s of min value

		// find min
		for (int i=0; i<8; i++) {
			if (s[i] < min) {
				min = s[i];
				idxMin = i;
			}
		}
		/*
		 * note:
		 * idx should never be on terrain boundary,
		 * because boundaries are dealt with separately in run()
		 */

		// set coords corresponding to min surface
		switch (idxMin) {
		case 0:
			c[0] = x-1;
			c[1] = y-1;
			break;
		case 1:
			c[0] = x-1;
			c[1] = y;
			break;
		case 2:
			c[0] = x-1;
			c[1] = y+1;
			break;
		case 3:
			c[0] = x;
			c[1] = y-1;
			break;
		case 4:
			c[0] = x;
			c[1] = y+1;
			break;
		case 5:
			c[0] = x+1;
			c[1] = y-1;
			break;
		case 6:
			c[0] = x+1;
			c[1] = y;
			break;
		case 7:
			c[0] = x+1;
			c[1] = y+1;
			break;
		default:
			c[0] = -1;
			c[1] = -1;
			break;
		}
	}

	/**
	 * <p>Calls {@link findLowest}, synchronized on the <code>depth</code> array.</p>
	 * <p>{@link findLowest} has many reads from <code>depth</code> array, so this 
	 * method needs to be atomic whenever the neighborhood of points may be accessed
	 * concurrently.</p>
	 * 
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point
	 * @param c coords of lowest point (negative value indicates that none are lower)
	 */
	private void findLowestS(int x, int y, int[] c) {
		synchronized (depth) {
			findLowest(x,y,c);
		}
	}
}