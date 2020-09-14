package flow;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.JPanel;

public class FlowPanel extends JPanel implements Runnable {
	Terrain land;
	Water water;

	FlowPanel(Terrain terrain) {
		land = terrain;
		water = new Water(land);

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
	
	public void run() {
		// display loop here
		// to do: this should be controlled by the GUI
		// to allow stopping and starting
		repaint();
	}
}