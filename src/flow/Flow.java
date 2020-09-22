package flow;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;

/**
 * <p>GUI handler.</p>
 * <p>Sets up a Frame with two panels. The display panel is 
 * for a {@link FlowPanel} object and the control panel is 
 * for buttons that the user can use to interact with the 
 * simulation.</p>
 * <p>This is a View class (of the MVC design pattern). It is
 * responsible for the visual representation. It takes in input data
 * and has action listeners for the buttons as well, but all processing
 * is done by {@link FlowPanel}</p>
 * 
 * @author hrrhan002
 */
public class Flow {
	// Frame dimensions
	static int frameX;
	static int frameY;

	// FlowPanel -- controller
	static FlowPanel fp;

	/**
	 * <p>Set up the application GUI.</p>
	 * <p>Components of the GUI are a frame, with a panel for {@link FlowPanel}
	 * and a panel for buttons.</p>
	 * 
	 * @param frameX Width of frame
	 * @param frameY Height of frame
	 * @param landdata Terrain data to give to <code>FlowPanel</code>
	 */
	public static void setupGUI(String dataFile) {

		// =============
		//  Frame setup
		// =============
		Dimension fsize = new Dimension(800, 800);
		JFrame frame = new JFrame("Waterflow"); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());

		// ============
		//  Main panel
		// ============
		JPanel g = new JPanel();
		g.setLayout(new BoxLayout(g, BoxLayout.PAGE_AXIS)); 

		fp = new FlowPanel(dataFile);
		frameX = fp.dimx();
		frameY = fp.dimy();
		fp.setPreferredSize(new Dimension(frameX, frameY));
		g.add(fp);

		// =========
		//  Buttons
		// =========

		// "End" ends program
		JButton endB = new JButton("End");
		endB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fp.end();
				frame.dispose();
			}
		});

		// "Reset" resets simulation
		JButton resetB = new JButton("Reset");
		resetB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fp.reset();
			}
		});

		// "Play" starts simulation
		JButton playB = new JButton("Play");
		playB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fp.play();
			}
		});

		// "Pause" pauses simulation
		JButton pauseB = new JButton("Pause");
		pauseB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fp.pause();
			}
		});

		// ==============
		//  Button panel
		// ==============
		JPanel b = new JPanel();
		b.setLayout(new BoxLayout(b, BoxLayout.LINE_AXIS));

		// Add buttons to panel
		b.add(resetB);
		b.add(Box.createRigidArea(new Dimension(10,0)));
		b.add(pauseB);
		b.add(Box.createRigidArea(new Dimension(10,0)));
		b.add(playB);
		b.add(Box.createRigidArea(new Dimension(10,0)));
		b.add(endB);
		// https://docs.oracle.com/javase/tutorial/uiswing/layout/box.html#filler

		g.add(b);

		// =============
		//  Frame setup
		// =============
		frame.setSize(frameX, frameY+50); // Extra space at the bottom for buttons
		frame.setLocationRelativeTo(null);  // Center window on screen
		frame.add(g); // Add components
		frame.setContentPane(g);
		frame.setVisible(true);
	}

	/**
	 * <p>Main method: read input file and invoke GUI.</p>
	 * 
	 * @param args Filepath to terrain data file
	 */
	public static void main(String[] args) {
		// Check that number of command line arguments is correct
		if(args.length != 1) {
			System.out.println("Incorrect number of command line arguments. Should have form: java -jar flow.java intputfilename");
			System.exit(0);
		}

		// Execute in event dispatch thread
		SwingUtilities.invokeLater( () -> setupGUI(args[0]) );
	}

}
