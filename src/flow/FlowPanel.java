package flow;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.JPanel;

public class FlowPanel extends JPanel{
	Terrain land;
	Water water;
	
	Thread[] sim;

	static final int NUM_THREADS = 1;
	static final int DROP_SIZE = 3;
	static final int DROP_DEPTH = 1;

	volatile boolean cancelled;
	boolean threadsWaiting;

	FlowPanel(Terrain terrain) {
		land = terrain; // get terrain
		water = new Water(land); // initialize water
		land.genPermute(); // generate permuted list in land
		
		sim = new Thread[NUM_THREADS];
		
		cancelled = false;
		threadsWaiting = false;

		// Mouse listener adds water where user clicks
		addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent me) { 
				try {
					water.add(me.getX(), me.getY(), DROP_DEPTH, DROP_SIZE);
					//water.flow(1, me.getX(), me.getY());
					//water.color(me.getX(), me.getY());
					repaint();
				}
				catch (ArrayIndexOutOfBoundsException err) {} // do nothing
			} 
		});
	}

	void sim() {
		cancelled = false;
		for (int t=0; t<NUM_THREADS; t++) {
			sim[t] = new Thread(new Simulate(land.dim()));
			sim[t].start();
		}
	}
	
	void clear() {
		cancel();
		water.reset();
		repaint();
	}

	void cancel() {
		cancelled = true;
	}
	
	void pause() {
		try {
			for (int t=0; t<NUM_THREADS; t++) {
				sim[t].wait();
			}
			threadsWaiting = true;
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		catch (NullPointerException e) {
			System.out.println("Null pointer to sim thread");
			e.printStackTrace();
		}
	}
	
	void resume() {
		if (threadsWaiting) {
			for (int t=0; t<NUM_THREADS; t++) {
				sim[t].notify();
			}
			threadsWaiting = false; // all should be notified at once
		}
	}

	// responsible for painting the terrain and water
	// as images
	@Override
	protected void paintComponent(Graphics g) {
		int width = getWidth();
		int height = getHeight();

		super.paintComponent(g);

		// draw the landscape in greyscale as an image
		if (land.getImage() != null){
			g.drawImage(land.getImage(), 0, 0, null);
		}

		// draw water
		if (water.getImage() != null){
			g.drawImage(water.getImage(), 0, 0, null);
		}
	}

	class Simulate implements Runnable {

		int numPts; // number of points to operate on

		Simulate(int n) {
			numPts = n;
		}

		// must call genpermute first
		public void run() {

			// TODO: check genpermute has been called

			int[] curr = new int[2]; // coords of this pt
			int[] next = new int[2]; // coords of pt water goes to

			while(!cancelled) {
				for(int i=0; i<numPts; i++) {
					land.getPermute(i, curr);

					if ( // conditions for points on boundary
							curr[0]==0 ||
							curr[1]==0 ||
							curr[0]==land.getDimX()-1 ||
							curr[1]==land.getDimY()-1
							) {
						water.flow(0, curr[0], curr[1]);
						water.color(curr[0], curr[1]);
						continue;
					}

					if (water.depth[curr[0]][curr[1]] != 0) {
						findLowest(curr[0], curr[1], next);
						if (next[0]<0) continue; // no water flow
						water.flow(-1, curr[0], curr[1]); // water out
						water.flow(1, next[0], next[1]); // water in

						// update color
						water.color(curr[0], curr[1]);
						water.color(next[0], next[1]);
					}
				}
				repaint();
			}
		}

		// find lowest neighboring point
		// set param c to coords of lowest pt
		// negative values indicate that none are lower
		private void findLowest(int x, int y, int[] c) {

			// set initial min to surface of current point
			float min = land.height[x][y] + 0.01f*water.depth[x][y];

			// surrounding surface values
			float[] s = {
					land.height[x-1][y-1] + 0.01f*water.depth[x-1][y-1],
					land.height[x-1][y] + 0.01f*water.depth[x-1][y],
					land.height[x-1][y+1] + 0.01f*water.depth[x-1][y+1],
					land.height[x][y-1] + 0.01f*water.depth[x][y-1],
					land.height[x][y+1] + 0.01f*water.depth[x][y+1],
					land.height[x+1][y-1] + 0.01f*water.depth[x+1][y-1],
					land.height[x+1][y] + 0.01f*water.depth[x+1][y],
					land.height[x+1][y+1] + 0.01f*water.depth[x+1][y+1]
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

	} // end Simulate class

}