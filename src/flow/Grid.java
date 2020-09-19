package flow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class Grid {
	
	// All immutable
	private int n; // number of threads
	private List<Integer>[] permute;
	private int dim, dimx, dimy;
	
	Grid (int n, int dimx, int dimy) {
		this.n = n;
		
		this.dimx = dimx;
		this.dimy = dimy;
		this.dim = dimx*dimy;
		
		/* I use this in a private method that only adds the correct
		 * type, so i'll just suppress this warning 
		 */
		permute = new List[n];
		
		genPermute();
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
	
	int dim() { return dim; }
	
	int dimx() { return dimx; }
	
	int dimy() { return dimy; }	
}