package flow;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.JPanel;

public class FlowPanel extends JPanel{
	Terrain land;
	Water water;
	Simulate sim;

	FlowPanel(Terrain terrain) {
		land = terrain;
		water = new Water(land);
		
		sim = new Simulate(land.dim());

		// Mouse listener adds water where user clicks
		addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent me) { 
				try {
					water.flow(1, me.getX(), me.getY());
					water.color(me.getX(), me.getY());
					repaint();
				}
				catch (ArrayIndexOutOfBoundsException err) {} // do nothing
			} 
		});
	}

	void sim() {
		land.genPermute();
		sim.run();
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

	void clear() {
		water.reset();
		repaint();
	}
	
	class Simulate extends Thread {
		
		int numPts; // number of points to operate on
		
		Simulate(int n) {
			numPts = n;
		}
		
		// must call genpermute first
		@Override
		public void run() {
			
			// TODO: check genpermute has been called
			
			int[] curr = new int[2]; // coords of this pt
			int[] next = new int[2]; // coords of pt water goes to
			
			for(int i=0; i<numPts; i++) {
				land.getPermute(i, curr);
				
				if ( // conditions for points on boundary
						curr[0]==0 ||
						curr[1]==0 ||
						curr[0]==land.getDimX() ||
						curr[1]==land.getDimY()
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