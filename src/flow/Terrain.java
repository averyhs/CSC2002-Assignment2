package flow;

import java.io.File;
import java.awt.image.*;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Terrain {

	float [][] height; // regular grid of height values
	int dimx, dimy, dim; // data dimensions
	BufferedImage img; // greyscale image for displaying the terrain top-down

	// Immutable
	private int n; // number of threads
	private List<Integer>[] permute;
	
	Terrain(String filepath, int numThreads) {
		readData(filepath);
		dim = dimx*dimy;
		n = numThreads;
		
		/* I use this in a private method that only adds the correct
		 * type, so i'll just suppress this warning 
		 */
		permute = new List[n];
		genPermute();
	}
	
	// overall number of elements in the height grid
	int dim(){
		return dimx*dimy;
	}

	// get x-dimensions (number of columns)
	int dimx(){
		return dimx;
	}

	// get y-dimensions (number of rows)
	int dimy(){
		return dimy;
	}

	// get greyscale image
	public BufferedImage getImage() {
		return img;
	}
	
	// convert linear position into 2D location in grid
		// don't need protection here because nothing is changed concurrently
		void locate (int pos, int [] ind)
		{
			ind[0] = (int)(pos/dimy); // x
			ind[1] = pos%dimy; // y	
		}

		// generate permuted lists of linear index positions to allow a random
		// traversal over the terrain
		// slower setup, faster play
		void genPermute() {
			for (int i=0; i<n; i++) {
				permute[i] = new ArrayList<Integer>();
				for (int idx=(int)(i*dim/n); idx<(int)((i+1)*dim/n); idx++) {
					permute[i].add (idx);
				}
				java.util.Collections.shuffle (permute[i]);
			}
		}

		// find permuted 2D location from a linear index in the
		// range [0, dimx*dimy)
		void getPermute (int pIdx, int i, int [] loc) {
			locate (permute[pIdx].get(i), loc);
		}
		
		int subLen() {
			return permute[0].size();
		}

	// convert height values to greyscale colour and populate an image
	void deriveImage()
	{
		img = new BufferedImage(dimx, dimy, BufferedImage.TYPE_INT_ARGB);
		float maxh = -10000.0f, minh = 10000.0f;

		// determine range of heights
		for(int x=0; x < dimx; x++)
			for(int y=0; y < dimy; y++) {
				float h = height[x][y];
				if(h > maxh)
					maxh = h;
				if(h < minh)
					minh = h;
			}

		for(int x=0; x < dimx; x++)
			for(int y=0; y < dimy; y++) {
				// find normalized height value in range
				float val = (height[x][y] - minh) / (maxh - minh);
				Color col = new Color(val, val, val, 1.0f);
				img.setRGB(x, y, col.getRGB());
			}
	}

	// read in terrain from file
	void readData(String fileName){ 
		try{ 
			Scanner sc = new Scanner(new File(fileName));

			// read grid dimensions
			// x and y correpond to columns and rows, respectively.
			// Using image coordinate system where top left is (0, 0).
			dimy = sc.nextInt(); 
			dimx = sc.nextInt();

			// populate height grid
			height = new float[dimx][dimy];

			for(int y = 0; y < dimy; y++){
				for(int x = 0; x < dimx; x++)	
					height[x][y] = sc.nextFloat();
			}

			sc.close(); 

			// generate greyscale heightfield image
			deriveImage();
		} 
		catch (IOException e){ 
			System.out.println("Unable to open input file "+fileName);
			e.printStackTrace();
		}
		catch (java.util.InputMismatchException e){ 
			System.out.println("Malformed input file "+fileName);
			e.printStackTrace();
		}
	}
}