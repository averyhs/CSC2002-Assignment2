package flow;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.color.*;

public class FlowPanel extends JPanel{

	Grid grid;
	Terrain terrain;
	Water water;
	
	CyclicBarrier barrier;

	final static int NUM_THREADS = 4;
	final static int DROP_DEPTH = 1;
	final static int DROP_SIZE = 3;
	
	AtomicInteger count;
	JLabel countL;

	volatile boolean paused;
	volatile boolean ended;

	FlowPanel (Terrain t) {
		this.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		terrain = t;
		water = new Water (terrain);
		grid = new Grid (NUM_THREADS, terrain.dimx, terrain.dimy);

		ended = false;
		paused = true;
		
		count = new AtomicInteger(0);
		
		countL = new JLabel("0",10);
		countL.setOpaque(true);
		countL.setBackground(Color.lightGray);
		countL.setBorder(BorderFactory.createEmptyBorder(1,2,1,2));
		this.add(countL);

		barrier = new CyclicBarrier(NUM_THREADS, () -> {
			count.getAndIncrement();
			countL.setText(String.valueOf(count.get()));
			});
		
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
	
	@Override
	protected void paintChildren(Graphics g) {
		super.paintChildren(g);
	}

	// controls
	void play() {
		paused = false;
	}
	
	void pause() {
		paused = true;
	}
	
	void reset() {
		paused = true;
		water.reset();
		repaint();
		
		// Wait so threads don't rewrite countL
		try { Thread.sleep(100); }
		catch (InterruptedException err) { err.printStackTrace(); }
		
		count.set(0);
		countL.setText("0");
		repaint();
	}
	
	void end() {
		ended = true;
	}

	class Simulate implements Runnable {

		int tNum; // thread number
		int lo, hi;
		int[] curr, next; // coords of current pt and pt water goes to

		Simulate (int t) {
			tNum = t;

			//lo = (int)(t*grid.dim()/NUM_THREADS);
			//hi = (int)((t+1)*grid.dim()/NUM_THREADS);

			curr = new int[2];
			//next = new int[2];
		}

		@Override
		public void run() {

			while (!ended) {

				if (paused) {
					continue;
				}
				
				for(int i=0; i<grid.subLen(); i++) {					
					grid.getPermute(tNum, i, curr);

					if (onMapBoundary()) {
						water.updateEdge(curr[0], curr[1]);
						repaint();
					}

					else if (onThreadBoundary()) {
						water.updateS(curr[0], curr[1]);
						repaint();
					}

					else {
						water.update(curr[0], curr[1]);
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

		boolean onMapBoundary() {
			return curr[0]==0 || curr[1]==0 ||
					curr[0]==grid.dimx()-1 || curr[1]==grid.dimy()-1;
		}

		boolean onThreadBoundary() {
			int yBound = hi%grid.dimy();
			return curr[1]==yBound || curr[1]==yBound-1 || curr[1]==yBound+1;
		}
	} // End of Simulate class
}