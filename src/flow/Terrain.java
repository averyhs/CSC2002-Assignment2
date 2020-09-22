package flow;

import java.io.File;
import java.awt.image.*;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Terrain {

	// No fields are changed externally

	float [][] height; // Regular grid of height values
	int dimx, dimy, dim; // Data dimensions
	BufferedImage img; // Grayscale image for displaying the terrain top-down

	/**
	 * <p>Permuted lists of indexes corresponding to points on the grid.</p> 
	 */
	private List<Integer>[] permute;
	private int n; // Number of threads

	/**
	 * <p>Reads terrain data from file, generates permuted index lists.</p>
	 * 
	 * @param filepath Path of file containing terrain data.
	 * @param numThreads Number of threads used to traverse the grid
	 */
	@SuppressWarnings("unchecked")
	Terrain(String filepath, int numThreads) {
		readData(filepath);
		dim = dimx*dimy;
		n = numThreads;

		/* I use this in a private method that only adds the correct
		 * type, so i just suppress this warning 
		 */
		permute = new List[n];
		genPermute(); // Generate permuted lists
	}

	/**
	 * <p>Accessor for grid dimension.</p>
	 * @return Total number of elements in the height grid
	 */
	int dim(){
		return dimx*dimy;
	}

	/**
	 * <p>Accessor for x-dimension of grid.</p>
	 * @return x-dimension of grid
	 */
	int dimx(){
		return dimx;
	}

	/**
	 * <p>Accessor for y-dimension of grid.</p>
	 * @return y-dimension of grid
	 */
	int dimy(){
		return dimy;
	}

	/**
	 * <p>Accessor for terrain image.</p>
	 * @return terrain image
	 */
	public BufferedImage getImage() {
		return img;
	}

	/**
	 * <p>Converts linear position into 2D coords.</p>
	 * <p>Used to find point on the grid for an index from a 
	 * {@link permute} list.</p>
	 * 
	 * @param pos linear position
	 * @param ind grid coordinates corresponding to linear position (pass in empty array)
	 */
	void locate (int pos, int [] ind)
	{
		ind[0] = (int)(pos/dimy); // x
		ind[1] = pos%dimy; // y	
	}

	/**
	 * <p>Generates permuted lists ({@link permute}) of linear index positions</p>
	 * <p>Permuted lists allow the grid to be traversed randomly, which helps
	 * the water flow simulation to be smoother.</p>
	 */
	void genPermute() {
		for(int i=0; i<n; i++) {
			permute[i] = new ArrayList<Integer>();
			for(int idx=(int)(i*dim/n); idx<(int)((i+1)*dim/n); idx++) {
				permute[i].add(idx);
			}
			java.util.Collections.shuffle (permute[i]);
		}
	}

	/**
	 * <p>Finds the location on grid for a given linear index.<br>
	 * (Depends on which thread wants to know)</p>
	 * 
	 * @param pIdx thread number (indicating which {@link permute} list to use)
	 * @param i list index
	 * @param loc location on grid (pass in empty array)
	 */
	void getPermute(int pIdx, int i, int [] loc) {
		locate(permute[pIdx].get(i), loc);
	}

	/**
	 * <p>Accessor for length of each {@link permute} list.</p>
	 * <p>{@link getPermute} takes an index, which is the index of one 
	 * list, not the whole grid. So a calling function needs to know the
	 * length of the lists. (They are all equal length).</p>
	 * 
	 * @return length of each {@link permute} list
	 */
	int subLen() {
		return permute[0].size();
	}

	/**
	 * <p>Converts height values into grayscale color and populates image.</p>
	 */
	void deriveImage()
	{
		img = new BufferedImage(dimx, dimy, BufferedImage.TYPE_INT_ARGB);
		float maxh = -10000.0f, minh = 10000.0f;

		// Determine range of heights
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
				// Find normalized height value in range
				float val = (height[x][y] - minh) / (maxh - minh);
				Color col = new Color(val, val, val, 1.0f);
				img.setRGB(x, y, col.getRGB());
			}
	}

	/**
	 * <p>Reads in terrain from file.</p>
	 * @param fileName Path of file
	 */
	void readData(String fileName){ 
		try{ 
			Scanner sc = new Scanner(new File(fileName));

			/* Read grid dimensions
			 * x and y correpond to columns and rows, respectively.
			 * Using image coordinate system where top left is (0,0).
			 */
			dimy = sc.nextInt(); 
			dimx = sc.nextInt();

			// Populate height grid
			height = new float[dimx][dimy];

			for(int y = 0; y < dimy; y++){
				for(int x = 0; x < dimx; x++)	
					height[x][y] = sc.nextFloat();
			}

			sc.close(); 

			// Generate grayscale heightfield image
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