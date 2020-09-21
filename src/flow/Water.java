package flow;

import java.awt.image.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Water {
	
	BufferedImage img;
	int[][] depth; // synchronized arraylist?
	Terrain terrain;
	
	AtomicInteger waterAdded;
	AtomicInteger waterRemoved;
	AtomicInteger waterCount;
	
	Water (Terrain t) {
		terrain = t;
		
		waterAdded = new AtomicInteger(0);
		waterRemoved = new AtomicInteger(0);
		waterCount = new AtomicInteger(0);
		
		// filled with zeros by default
		depth = new int[terrain.dimx()][terrain.dimy()];
		
		// transparent image
		img = new BufferedImage(terrain.dimx(), terrain.dimy(), BufferedImage.TYPE_INT_ARGB);
	}
	
	BufferedImage getImage() {
		return img;
	}
	
	int waterAdded() { return waterAdded.get(); }
	int waterRemoved() { return waterRemoved.get(); }
	int waterCount() {
		synchronized (depth) {
			waterCount.set(0);
			for (int i=0; i<terrain.dimx(); i++) {
				for(int j=0; j<terrain.dimy(); j++) {
					if (depth[i][j] > 0) {
						for (int d=0; d<depth[i][j]; d++) {
							waterCount.getAndIncrement();
						}
					}
				}
			}
			return waterCount.get();
		}
	}
	
	// in my code this is not concurrent, and it works fine, and
	// if i made it concurrent, there wouldn't need to be any shared
	// resources, so this is fine
	void reset() {
		// zero depth everywhere
		// TODO: candidate for parallelization
		for (int i=0; i<terrain.dimx(); i++) {
			for(int j=0; j<terrain.dimy(); j++) {
				if (depth[i][j] > 0) {
					for (int d=0; d<depth[i][j]; d++) {
						waterRemoved.getAndIncrement();
					}
				}
				
				depth[i][j] = 0;
			}
		}
		
		// set to transparent
		img = new BufferedImage(terrain.dimx(), terrain.dimy(), BufferedImage.TYPE_INT_ARGB);
	}
	
	// change depth at a point
	// change param can be +ve or -ve, will be added to current depth
	// if change is 0, depth will be set to zero.
	//
	// check-act and read-modify-write here. need protection.
	// but only for regions at the meeting of thread zones because
	// thats the only place with concurrent access
	//
	// this one is for independent, unprotected access
	void flow (int change, int x, int y) {
		if (change==0) {
			depth[x][y] = 0;
		}
		else {
			depth[x][y] += change;
		}
	}
	
	// this one is for synchronized access
	void flowS (int change, int x, int y) {
		synchronized (depth) {
			if (change==0) {
				depth[x][y] = 0;
			}
			else {
				depth[x][y] += change;
			}
		}
	}
	
	// i didn't code this in a thread, but could be concurrent
	// access behind the scenes?
	void add(int x, int y, int d, int s) {
		for (int i=-s; i<=s; i++) {
			for (int j=-s; j<=s; j++) {
				depth[x+i][y+j] = d;
				color(x+i,y+j);
				
				for (int a=0; a<d; a++) {
					waterAdded.getAndIncrement();
				}
			}
		}
	}
	
	// check-act pattern with read from water. needs protection
	//
	// for independent, unprotected access
	void color (int x, int y) {
		// https://dyclassroom.com/image-processing-project/how-to-get-and-set-pixel-value-in-java
		if (depth[x][y]==0) {
			// Empty: A=0 R=0 G=0 B=0
			img.setRGB(x, y, 0);
		}
		else {
			// Blue: A=255 R=0 G=0 B=255 
			int p = (255<<24) | 255;
			img.setRGB(x, y, p);
		}
	}
	
	// for synchronized access
	void colorS (int x, int y) {
		synchronized (depth) {
			if (depth[x][y]==0) {
				// Empty: A=0 R=0 G=0 B=0
				img.setRGB(x, y, 0);
			}
			else {
				// Blue: A=255 R=0 G=0 B=255 
				int p = (255<<24) | 255;
				img.setRGB(x, y, p);
			}
		}
	}
	
	void updateEdge(int x, int y) {
		if (depth[x][y] > 0) {
			for (int i=0; i<depth[x][y]; i++) {
				waterRemoved.getAndIncrement();
			}
		}
		
		flow(0, x, y);
		color(x, y);
	}
	
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
	
	void updateS(int x, int y) {
		int[] nextPt = new int[2];
		
		synchronized (depth) {
			if (depth[x][y] != 0) {
				findLowestS(x, y, nextPt);

				if (nextPt[0]<0) { return; } // no water flow
				flowS(-1, x, y); // water out
				flowS(1, nextPt[0], nextPt[1]); // water in

				// update color
				colorS(x, y);
				colorS(nextPt[0], nextPt[1]);
			}
		}
	}
	
	// find lowest neighboring point
	// sets param c to coords of lowest pt
	// negative values indicate that none are lower
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
	
	// find lowest neighboring point
	// sets param c to coords of lowest pt
	// negative values indicate that none are lower
	private void findLowestS(int x, int y, int[] c) {
		synchronized (depth) {
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
	}
}