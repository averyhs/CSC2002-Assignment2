package flow;

import java.awt.Graphics;
import javax.swing.JPanel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowPanel extends JPanel{

	Grid grid;
	Terrain terrain;
	Water water;
	
	CyclicBarrier barrier;

	final static int NUM_THREADS = 1;
	final static int DROP_DEPTH = 1;
	final static int DROP_SIZE = 3;
	
	AtomicInteger count;

	volatile boolean paused;
	volatile boolean ended;

	FlowPanel (Terrain t) {
		terrain = t;
		water = new Water (terrain);
		grid = new Grid (NUM_THREADS, terrain.dimx, terrain.dimy);

		ended = false;
		paused = true;
		
		count = new AtomicInteger(0);

		barrier = new CyclicBarrier(NUM_THREADS, () -> count.getAndIncrement());
		
		// create and start threads
		for (int s=0; s<NUM_THREADS; s++) {
			Thread temp =  new Thread(new Simulate(s));
			temp.start();
		}

		// Mouse listener adds water where user clicks
		addMouseListener (new MouseAdapter() { 
			public void mouseClicked (MouseEvent me) { 
				try {
					water.add (me.getX(), me.getY(), DROP_DEPTH, DROP_SIZE);
					repaint();
				}
				catch (ArrayIndexOutOfBoundsException err) {} // Off map, do nothing
			} 
		});
	}

	// responsible for painting the terrain and water
	// as images
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// draw the landscape in greyscale as an image
		if (terrain.getImage() != null){
			g.drawImage(terrain.getImage(), 0, 0, null);
		}

		// draw water
		if (water.getImage() != null){
			g.drawImage(water.getImage(), 0, 0, null);
		}
	}

	// controls
	void play() {
		paused = false;
	}
	
	void pause() {
		paused = true;
	}
	
	void reset() {
		water.reset();
		paused = true;
		count.set(0);
		repaint();
	}
	
	void end() {
		ended = true;
	}
	
	int count() {
		return count.get();
	}

	class Simulate implements Runnable {

		int tNum; // thread number
		int lo, hi;
		int[] curr, next; // coords of current pt and pt water goes to

		Simulate (int t) {
			tNum = t;

			lo = (int)(t*grid.dim()/NUM_THREADS);
			hi = (int)((t+1)*grid.dim()/NUM_THREADS);

			curr = new int[2];
			next = new int[2];
		}

		@Override
		public void run() {

			while (!ended) {

				if (paused) {
					continue;
				}
				
				System.out.println(count.get());
				
				for(int i=lo; i<hi; i++) {					
					grid.getPermute(tNum, i, curr);

					if (onMapBoundary()) {
						updateEdge();
						repaint();
					}

//					else if (onThreadBoundary()) {
//						updateSync();
//						repaint();
//					}

					else {
						update();
						repaint();
					}
				}
				
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException err) {
					err.printStackTrace();
				}
			}
		}

		void updateEdge() {
			water.flow(0, curr[0], curr[1]);
			water.color(curr[0], curr[1]);
		}

		void update() {
			if (water.depth[curr[0]][curr[1]] != 0) {
				findLowest(curr[0], curr[1], next);

				if (next[0]<0) return; // no water flow
				water.flow(-1, curr[0], curr[1]); // water out
				water.flow(1, next[0], next[1]); // water in

				// update color
				water.color(curr[0], curr[1]);
				water.color(next[0], next[1]);
			}
		}

		void updateSync() {
			// same as update() but with safety measures
		}

		boolean onMapBoundary() {
			return curr[0]==0 || curr[1]==0 ||
					curr[0]==grid.dimx()-1 || curr[1]==grid.dimy()-1;
		}

		boolean onThreadBoundary() {
			int yBound = hi%grid.dimy();
			return curr[1]==yBound || curr[1]==yBound-1 || curr[1]==yBound+1;
		}

		// find lowest neighboring point
		// sets param c to coords of lowest pt
		// negative values indicate that none are lower
		private void findLowest(int x, int y, int[] c) {

			// set initial min to surface of current point
			float min = terrain.height[x][y] + 0.01f*water.depth[x][y];

			// surrounding surface values
			float[] s = {
					terrain.height[x-1][y-1] + 0.01f*water.depth[x-1][y-1],
					terrain.height[x-1][y] + 0.01f*water.depth[x-1][y],
					terrain.height[x-1][y+1] + 0.01f*water.depth[x-1][y+1],
					terrain.height[x][y-1] + 0.01f*water.depth[x][y-1],
					terrain.height[x][y+1] + 0.01f*water.depth[x][y+1],
					terrain.height[x+1][y-1] + 0.01f*water.depth[x+1][y-1],
					terrain.height[x+1][y] + 0.01f*water.depth[x+1][y],
					terrain.height[x+1][y+1] + 0.01f*water.depth[x+1][y+1]
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
	} // End of Simulate class
}