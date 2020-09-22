package flow;

import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Panel that manages the simulation.</p>
 * 
 * @author avk
 */
public class FlowPanel extends JPanel{

	/* XXX: Not having this can result in unexpected InvalidClassExceptions 
	 * due to sensitivity to different compilers according to documentation 
	 * (see link), so i let Eclipse add a generated serialVersionUID
	 * stackoverflow:
	 * https://stackoverflow.com/questions/285793/what-is-a-serialversionuid-and-why-should-i-use-it
	 */
	private static final long serialVersionUID = 2900279500141540118L;

	// ========
	//  Fields
	// ========

	// Data
	Terrain terrain;
	Water water;

	// Constants
	final static int NUM_THREADS = 4;
	final static int DROP_DEPTH = 3;
	final static int DROP_SIZE = 3;

	// Count
	AtomicInteger count;
	JLabel countL;

	// Threading
	CyclicBarrier barrier;
	volatile boolean paused;
	volatile boolean ended;

	// =============
	//  Constructor
	// =============

	/**
	 * <p><code>FlowPanel</code> constructor.</p>
	 * <p>Initializes data, counter, threads, and mouse listener.</p>s
	 * 
	 * @param t Terrain object for simulation
	 */
	FlowPanel (String dataFile) {

		// =========
		//  Counter
		// =========
		// Determines where count label will be
		this.setLayout(new FlowLayout(FlowLayout.RIGHT));
		// Label and properties
		countL = new JLabel("0",10);
		countL.setOpaque(true);
		countL.setBackground(Color.lightGray);
		countL.setBorder(BorderFactory.createEmptyBorder(1,2,1,2));
		this.add(countL);
		// Initialize counter
		count = new AtomicInteger(0);

		// ===================
		//  Terrain and Water
		// ===================
		terrain = new Terrain(dataFile, NUM_THREADS);
		water = new Water(terrain);

		// =========
		//  Threads
		// =========

		// Set initial ended and paused states
		ended = false;
		paused = true;

		// Set up a cyclic barrier
		/* This defines the number of threads required to trip barrier
		 * and what actions should be taken by the last thread to trip
		 * the barrier (lambda expression is for Runnable interface)
		 */
		barrier = new CyclicBarrier(NUM_THREADS, () -> {
			count.getAndIncrement();
			countL.setText(String.valueOf(count.get()));
		});

		// Create and start threads
		for (int s=0; s<NUM_THREADS; s++) {
			Thread temp =  new Thread(new Simulate(s));
			temp.start();
		}

		// ================
		//  Mouse listener
		// ================
		addMouseListener(new MouseAdapter() { 
			public void mouseClicked(MouseEvent me) { 
				try {
					// Add water where user clicks
					water.add(me.getX(), me.getY(), DROP_DEPTH, DROP_SIZE);
					repaint();
				}
				catch (ArrayIndexOutOfBoundsException err) {} // Off map, do nothing
			}
		});
	}

	// ============
	//  Frame dims
	// ============

	/**
	 * <p>Accessor for grid x-dimension.</p>
	 * <p>Used to get frame dimensions for {@link Flow}.</p>
	 * 
	 * @return grid x-dimension
	 */
	int dimx() {
		return terrain.dimx();
	}

	/**
	 * <p>Accessor for grid y-dimension.</p>
	 * <p>Used to get frame dimensions for {@link Flow}.</p>
	 * 
	 * @return grid y-dimension
	 */
	int dimy() {
		return terrain.dimy();
	}

	// ==========
	//  Painters
	// ==========

	/**
	 * <p>Paints terrain and water.</p>
	 * 
	 * @param g Panel's graphics object
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// draw landscape (grayscale)
		if (terrain.getImage() != null){
			g.drawImage(terrain.getImage(), 0, 0, null);
		}

		// draw water
		if (water.getImage() != null){
			g.drawImage(water.getImage(), 0, 0, null);
		}
	}

	/**
	 * <p>Paints count label.</p>
	 * 
	 * @param g Panel's graphics object
	 */
	@Override
	protected void paintChildren(Graphics g) {
		super.paintChildren(g);
	}

	// ==========
	//  Controls
	// ==========

	/**
	 * <p>Resumes simulation.</p>
	 * <p>Sets <code>paused</code> to false.</p>
	 */
	void play() {
		paused = false;
	}

	/**
	 * <p>Pause simulation.</p>
	 * <p>Sets <code>paused</code> to true.</p>
	 */
	void pause() {
		paused = true;

		// Water conservation testing    |
		// Uncomment                     v
		//System.out.println("+------------------------------------------------+");
		//System.out.println(" water added: "+water.waterAdded());
		//System.out.println(" water removed: "+water.waterRemoved());
		//System.out.println(" (water added) - (water removed): " + (water.waterAdded()-water.waterRemoved()));
		//System.out.println(" water count: "+water.waterCount());
		//System.out.println("+------------------------------------------------+");
	}

	/**
	 * <p>Resets simulation.</p>
	 * <p>Sets <code>paused</code> to true, resets water to zero,
	 * resets counter to zero.</p>
	 */
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

	/**
	 * <p>Ends simulation.</p>
	 * <p>Sets <code>ended</code> to true.</p>
	 */
	void end() {
		ended = true;
	}

	/**
	 * <p>Simulation engine.</p>
	 * <p>Runnable class that is instantiated by {@link FlowPanel} 
	 * as Threads to traverse the grid and simulate the flow of the 
	 * water over the terrain. When a thread completes one iteration
	 * (one traversal of it's section of the grid) it trips the 
	 * cyclic barrier, so threads are synchronized at each 
	 * traversal.</p>
	 * <p>This is a Controller class (of the MVC design pattern).
	 * It handles the data and makes decisions on what to do and
	 * when to do it.</p>
	 * 
	 * @author avk
	 */
	class Simulate implements Runnable {

		int tNum; // Thread number [0, NUM_THREADS)
		int[] coords; // Coords of current point

		/**
		 * <p><code>Simulate</code> constructor.</p>
		 * @param t Thread number (0,1,...)
		 */
		Simulate (int t) {
			tNum = t;
			coords = new int[2];
		}

		/**
		 * <p>Traverses grid updating water depths.</p>
		 */
		@Override
		public void run() {

			while (!ended) { // Loop until simulation is ended

				if (paused) { // Just spin if paused
					continue;
				}

				// Loop over a quarter of the grid (assuming 4 threads)
				for(int i=0; i<terrain.subLen(); i++) {
					// Get coords of point to consider
					terrain.getPermute(tNum, i, coords);

					if (onMapBoundary()) {
						// Run off edge
						water.updateEdge(coords[0], coords[1]);
						repaint();
					}

					else if (onThreadBoundary()) {
						// Check & transfer water with mutual exclusion
						water.updateS(coords[0], coords[1]);
						repaint();
					}

					else {
						// Check & transfer water (no mutual exclusion)
						water.update(coords[0], coords[1]);
						repaint();
					}
				}

				try {
					barrier.await(); // Trip barrier
				} catch (InterruptedException | BrokenBarrierException err) {
					System.out.println("Error at cyclic barrier");
					err.printStackTrace();
				}
			}
		}

		/**
		 * <p>Determines if point is on the boundary of the map.</p>
		 * @return true if point is on boundary, false otherwise
		 */
		boolean onMapBoundary() {
			// Min and max values of x and y
			return coords[0]==0 || coords[1]==0 ||
					coords[0]==terrain.dimx()-1 || coords[1]==terrain.dimy()-1;
		}

		/**
		 * <p>Determines if point is in region on boundary of thread zones</p>
		 * <p>I.e. if point is in a region where multiple threads will be accessing it.</p>
		 * @return true if point is in boundary region, false otherwise
		 */
		boolean onThreadBoundary() {
			/* bound is the last row of the grid that the thread is responsible for.
			 * This and the rows above and below it are accessed by multiple threads, 
			 * so there needs to be mutual exclusion on that data.
			 * 
			 * Calculation of bound:
			 * grid.dim()/NUM_THREADS divides grid into NUM_THREADS zones. Multiplying
			 * by (tNum+1) selects the end of this thread's zone. %grid.dimy() gets the 
			 * row number.
			 */
			int bound = ((tNum+1)*terrain.dim()/NUM_THREADS)%terrain.dimy();
			return coords[1]==bound || coords[1]==bound-1 || coords[1]==bound+1;
		}
	} // End of Simulate class
}