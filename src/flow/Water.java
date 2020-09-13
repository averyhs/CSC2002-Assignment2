package flow;

import java.awt.image.*;
import java.awt.Color;

public class Water {
	
	BufferedImage img;
	int dimx, dimy;
	
	Water(Terrain land) {
		dimx = land.getDimX();
		dimy = land.getDimY();
		
		// transparent image, size of terrain
		img = new BufferedImage(dimx, dimy, BufferedImage.TYPE_INT_ARGB);
		
	}
	
	BufferedImage getImage() {
		return img;
	}
	
	void add(int x, int y) {
		// https://dyclassroom.com/image-processing-project/how-to-get-and-set-pixel-value-in-java
		// solid blue: A=255 R=0 G=0 B=255 
		int p = (255<<24) | 255;
		img.setRGB(x, y, p);
	}
}