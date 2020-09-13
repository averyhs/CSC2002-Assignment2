package flow;

import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

public class FlowPanel extends JPanel implements Runnable, MouseListener {
	Terrain land;
	Water water;
	
	FlowPanel(Terrain terrain) {
		land = terrain;
		water = new Water(land);
		
		addMouseListener(this);
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

	@Override
	public void mouseClicked(MouseEvent e) {
		System.out.println("Mouse Clicked at "+e.getX()+", "+e.getY());
		water.add(e.getX(), e.getY());
		repaint();
		
		// TODO: catch error for clicking off panel
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {}
	
	@Override
	public void mouseExited(MouseEvent e) {}
	
	@Override
	public void mousePressed(MouseEvent e) {}
	
	@Override
	public void mouseReleased(MouseEvent e) {}
	
	public void run() {
		// display loop here
		// to do: this should be controlled by the GUI
		// to allow stopping and starting
		repaint();
	}
}