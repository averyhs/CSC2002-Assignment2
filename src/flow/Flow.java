package flow;

import javax.swing.*;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;

public class Flow {
	static long startTime = 0;
	static int frameX;
	static int frameY;
	static FlowPanel fp;

	// start timer
	private static void tick(){
		startTime = System.currentTimeMillis();
	}

	// stop timer, return time elapsed in seconds
	private static float tock(){
		return (System.currentTimeMillis() - startTime) / 1000.0f; 
	}

	public static void setupGUI(int frameX,int frameY,Terrain landdata) {

		// ** Window **
		Dimension fsize = new Dimension(800, 800);
		JFrame frame = new JFrame("Waterflow"); 
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());

		// ** Image panel **
		JPanel g = new JPanel();
		g.setLayout(new BoxLayout(g, BoxLayout.PAGE_AXIS)); 

		fp = new FlowPanel(landdata);
		fp.setPreferredSize(new Dimension(frameX,frameY));
		g.add(fp);

		// to do: add a MouseListener, buttons and ActionListeners on those buttons

		// ** Buttons **
		
		// "End" ends program when pressed
		JButton endB = new JButton("End");
		endB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fp.end();
				frame.dispose();
				// There's only one frame, so can just call exit
				//System.exit(0);
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
		
		// ** Button panel **
		JPanel b = new JPanel();
		b.setLayout(new BoxLayout(b, BoxLayout.LINE_AXIS));
		// Add buttons to panel
		b.add(Box.createHorizontalGlue());
		b.add(resetB);
		b.add(Box.createHorizontalGlue());
		b.add(pauseB);
		b.add(Box.createHorizontalGlue());
		b.add(playB);
		b.add(Box.createHorizontalGlue());
		b.add(endB);
		b.add(Box.createHorizontalGlue());
		// Add panel to frame
		g.add(b);

		frame.setSize(frameX, frameY+50);	// a little extra space at the bottom for buttons
		frame.setLocationRelativeTo(null);  // center window on screen
		frame.add(g); //add contents to window
		frame.setContentPane(g);
		frame.setVisible(true);
		//Thread fpt = new Thread(fp);
		//fpt.start();
	}


	public static void main(String[] args) {
		Terrain landdata = new Terrain();

		// check that number of command line arguments is correct
		if(args.length != 1)
		{
			System.out.println("Incorrect number of command line arguments. Should have form: java -jar flow.java intputfilename");
			System.exit(0);
		}

		// landscape information from file supplied as argument
		// 
		landdata.readData(args[0]);

		frameX = landdata.getDimX();
		frameY = landdata.getDimY();
		SwingUtilities.invokeLater(()->setupGUI(frameX, frameY, landdata));

		// to do: initialise and start simulation
		
		//System.out.println("fp: "+fp);
	}
}
