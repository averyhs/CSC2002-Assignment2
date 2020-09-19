package flow;

import java.awt.image.*;
import java.awt.Color;

public class Water {
	
	BufferedImage img;
	int[][] depth; // synchronized arraylist?
	int dimx, dimy;
	
	Water(Terrain land) {
		// same dimensions as terrain
		dimx = land.getDimX();
		dimy = land.getDimY();
		
		// filled with zeros by default
		depth = new int[dimx][dimy];
		
		// transparent image
		img = new BufferedImage(dimx, dimy, BufferedImage.TYPE_INT_ARGB);
	}
	
	BufferedImage getImage() {
		return img;
	}
	
	void reset() {
		// zero depth everywhere
		// TODO: candidate for parallelization
		for(int i=0; i<dimx; i++) {
			for(int j=0; j<dimy; j++) {
				depth[i][j] = 0;
			}
		}
		
		// set to transparent
		img = new BufferedImage(dimx, dimy, BufferedImage.TYPE_INT_ARGB);
	}
	
	// change depth at a point
	// change param can be +ve or -ve, will be added to current depth
	// if change is 0, depth will be set to zero.
	void flow(int change, int x, int y) {
		if (change==0) {
			depth[x][y] = 0;
		}
		else {
			depth[x][y] += change;
		}
	}
	
	void add(int x, int y, int d, int s) {
		for (int i=-s; i<=s; i++) {
			for (int j=-s; j<=s; j++) {
				depth[x+i][y+j] = d;
				color(x+i,y+j);
			}
		}
	}
	
	void color(int x, int y) {
		// https://dyclassroom.com/image-processing-project/how-to-get-and-set-pixel-value-in-java
		if(depth[x][y]==0) {
			// Empty: A=0 R=0 G=0 B=0
			img.setRGB(x, y, 0);
		}
		else {
			// Blue: A=255 R=0 G=0 B=255 
			int p = (255<<24) | 255;
			img.setRGB(x, y, p);
		}
	}
}