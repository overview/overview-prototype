package snappy.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import snappy.data.SimpleEdge;

public class GlimmerLayout {

	public GraphManager m_gm = null;

	boolean areUsingSubset = false;
	ArrayList<Integer> included_points = null; // use these indices if
												// areUsingSubset == true

	public float[] m_embed = null; // the embedding coordinates
	int g_embedding_dims = 2; // we're always reducing to 2

	public static boolean DRAW_EDGES = false;
	public static boolean DRAW_LABELS = false;
	public static int POWER_FACTOR = 8;
	static int V_SET_SIZE = 14;
	static int S_SET_SIZE = 10; // number of randomly chosen neighbors
	static int MAX_ITERATION = 50000; // maximum number of iterations
	static int COSCLEN = 51; // length of cosc filter
	static float EPS = Float.MIN_VALUE; // termination threshold
	static int MIN_SET_SIZE = 1000; // recursion termination condition
	static int DEC_FACTOR = 8; // decimation factor
	static float cosc[] = { 0.f, -0.00020937301404f, -0.00083238644375f,
			-0.00187445134867f, -0.003352219513758f, -0.005284158713234f,
			-0.007680040381756f, -0.010530536243981f, -0.013798126870435f,
			-0.017410416484704f, -0.021256733995966f, -0.025188599234624f,
			-0.029024272810166f, -0.032557220569071f, -0.035567944643756f,
			-0.037838297355557f, -0.039167132882787f, -0.039385989227318f,
			-0.038373445436298f, -0.036066871845685f, -0.032470479106137f,
			-0.027658859359265f, -0.02177557557417f, -0.015026761314847f,
			-0.007670107630023f, 0.f, 0.007670107630023f, 0.015026761314847f,
			0.02177557557417f, 0.027658859359265f, 0.032470479106137f,
			0.036066871845685f, 0.038373445436298f, 0.039385989227318f,
			0.039167132882787f, 0.037838297355557f, 0.035567944643756f,
			0.032557220569071f, 0.029024272810166f, 0.025188599234624f,
			0.021256733995966f, 0.017410416484704f, 0.013798126870435f,
			0.010530536243981f, 0.007680040381756f, 0.005284158713234f,
			0.003352219513758f, 0.00187445134867f, 0.00083238644375f,
			0.00020937301404f, 0.f };

	public class IndexType {

		boolean isDuplicate = false;
		SimpleEdge se;		// simple edge we're grabing
		public float lowd; // low dimensional distance
	}

	public boolean isDone() {
		
		return g_done;
	}
	
	boolean g_init = false;
	
	public void updateLayout() {

		if( g_init ) {			// initialize
						
			init_embedding();
			g_init = false;
		}
		
		if (!g_done) {

			// move the points
			force_directed();

			// check the termination condition
			if (terminate()) {

				g_stop_iteration = g_cur_iteration;

				if (g_interpolating) {

					g_interpolating = false;
				} else {

					g_current_level--; // move to the next level down
					g_interpolating = true;

					// check if the algorithm is complete (no more levels)
					if (g_current_level < 0) {

						g_done = true;
					}
				}
			}

			g_cur_iteration++; // increment the current iteration count
		}
	}

	public boolean terminate() {

		float numer = 0.f; // sq diff of dists
		float denom = 0.f; // sq dists
		float temp = 0.f;

		if (g_cur_iteration > MAX_ITERATION) {

			return true;
		}

		int size = g_heir[g_current_level];

		// compute sparse stress
		for (int i = 0; i < size; i++) {

			for (int j = 0; j < (V_SET_SIZE + S_SET_SIZE); j++) {

				temp = (g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].isDuplicate) ? 0.f
						: (g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se.w - g_idx[i
								* (V_SET_SIZE + S_SET_SIZE) + j].lowd);
				// System.out.print(","+temp);
				numer += temp * temp;
				denom += (g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].isDuplicate) ? 0.f
						: (g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se.w * g_idx[i
								* (V_SET_SIZE + S_SET_SIZE) + j].lowd);
			}
		}
		sstress[g_cur_iteration] = numer / denom;

		// convolve the signal
		float signal = 0.f;
		if (g_cur_iteration - g_stop_iteration > COSCLEN) {

			for (int i = 0; i < COSCLEN; i++) {

				signal += sstress[(g_cur_iteration - COSCLEN) + i] * cosc[i];
			}

			if (Math.abs(signal) < EPS) {

				// stop_iteration = iteration;
				return true;
			}
		}

		return false;
	}
	public class IdxComp implements Comparator<IndexType> {

		@Override
		public int compare(IndexType da, IndexType db) {
			return (int)(da.se.dst - db.se.dst);
		}
		
	}
	public class DistComp implements Comparator<IndexType> {

		@Override
		public int compare(IndexType da, IndexType db) {
			if(da.se.w == db.se.w)
				return 0;
			return (da.se.w - db.se.w)<0.f?-1:1;
		}
		
	}
	
	

	/**
	 * Compute Chalmers' an iteration of force directed simulation on subset of
	 * size 'ssize' holding fixedsize fixed
	 * 
	 * @param ssize
	 *            - the size of the moving point set
	 * @param fixedsize
	 *            - the size of the fixed point set
	 * @param iteration
	 *            - the current iteration
	 * @param stop_iteration
	 *            - total number of iterations since changing levels
	 */
	public void force_directed() {

		int ssize = g_heir[g_current_level];
		int fixedsize = 0;
		if (g_interpolating)
			fixedsize = g_heir[g_current_level + 1];

		// initialize index sets
		if (g_cur_iteration == g_stop_iteration) {

			for (int i = 0; i < ssize; i++) {

				for (int j = 0; j < V_SET_SIZE; j++) {

					SimpleEdge se = null;
					if( j > m_gm.nodeEdgeLookup.get(i).size() - 2 ) {
						se = m_gm.nodeEdgeLookup.get(i).get(1);
					}
					else {
						se = m_gm.nodeEdgeLookup.get(i).get(j+1);
					}
					g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se = se; 
				}
			}
		}

		// perform the force simulation iteration
		float[] dir_vec = new float[g_embedding_dims];
		float[] relvel_vec = new float[g_embedding_dims];
		float diff = 0.f;
		float norm = 0.f;
		float lo = 0.f;
		float hi = 0.f;

		// compute new forces for each point
		for (int i = fixedsize; i < ssize; i++) {

			for (int j = 0; j < V_SET_SIZE + S_SET_SIZE; j++) {

				// update the S set with random entries
				if (j >= V_SET_SIZE) {
					
					SimpleEdge se = null;
					if( V_SET_SIZE > m_gm.nodeEdgeLookup.get(i).size() - 2 ) {
						se = m_gm.nodeEdgeLookup.get(i).get(1);
					}
					else {
						se = m_gm.nodeEdgeLookup.get(i).get(V_SET_SIZE + myRandom.nextInt(m_gm.nodeEdgeLookup.get(i).size()-V_SET_SIZE));
					} 
					g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se = se;
					// g_idx[i*(V_SET_SIZE+S_SET_SIZE)+j].index =
					// myRandom.nextInt(g_interpolating?fixedsize:ssize);
				}
			}

			// sort index set by index
			Arrays.sort(g_idx, i * (V_SET_SIZE + S_SET_SIZE), (i + 1)
					* (V_SET_SIZE + S_SET_SIZE), new IdxComp());

			// mark duplicates (with 1000)
			for (int j = 0; j < V_SET_SIZE + S_SET_SIZE; j++) {

				if( j > 0) {
					if (g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se.dst == g_idx[i
							* (V_SET_SIZE + S_SET_SIZE) + j - 1].se.dst)
						g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].isDuplicate = true;
					else {
						g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].isDuplicate = false;
					}
				}
				
				if( g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se.src == g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se.dst ) {
					
					g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].isDuplicate = true;
				}
			}

			// sort index set by distance
			Arrays.sort(g_idx, i * (V_SET_SIZE + S_SET_SIZE), (i + 1)
					* (V_SET_SIZE + S_SET_SIZE), new DistComp());

			// move the point
			for (int j = 0; j < (V_SET_SIZE + S_SET_SIZE); j++) {

				// get a reference to the other point in the index set
				int idx = g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se.dst;
				norm = 0.f;
				for (int k = 0; k < g_embedding_dims; k++) {

					// calculate the direction vector
					dir_vec[k] = m_embed[idx * g_embedding_dims + k]
							- m_embed[i * g_embedding_dims + k];
					norm += dir_vec[k] * dir_vec[k];
				}
				norm = (float) Math.sqrt(norm);
				g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].lowd = norm;
				if (norm > 1.e-6
						&& !g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].isDuplicate ) {
					
					// normalize direction vector
					for (int k = 0; k < g_embedding_dims; k++) {

						dir_vec[k] /= norm;
					}

					// calculate relative velocity
					for (int k = 0; k < g_embedding_dims; k++) {
						relvel_vec[k] = g_vel[idx *g_embedding_dims + k]
								- g_vel[i * g_embedding_dims + k];
					}

					// calculate difference between lo and hi distances
					lo = g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].lowd;
					hi = (float) Math.pow(g_idx[i * (V_SET_SIZE + S_SET_SIZE) + j].se.w,POWER_FACTOR);
					diff = (lo - hi) * SPRINGFORCE;
					// compute damping value
					norm = 0.f;
					for (int k = 0; k < g_embedding_dims; k++) {

						norm += dir_vec[k] * relvel_vec[k];
					}
					diff += norm * DAMPING;

					// accumulate the force
					for (int k = 0; k < g_embedding_dims; k++) {

						g_force[i * g_embedding_dims + k] += dir_vec[k] * diff;
					}
				}
			}

			// scale the force by the size factor
			for (int k = 0; k < g_embedding_dims; k++) {

				g_force[i * g_embedding_dims + k] *= SIZE_FACTOR;
			}
		}

		// compute new velocities for each point with Euler integration
		for (int i = fixedsize; i < ssize; i++) {

			for (int k = 0; k < g_embedding_dims; k++) {

				float foo = g_vel[i * g_embedding_dims + k];
				float bar = foo + g_force[i * g_embedding_dims + k] * DELTATIME;
				float baz = bar * FREENESS;
				g_vel[i * g_embedding_dims + k] = (float) Math.max(
						Math.min(baz, 2.0), -2.0);
			}
		}

		// compute new positions for each point with Euler integration
		for (int i = fixedsize; i < ssize; i++) {
			for (int k = 0; k < g_embedding_dims; k++) {

				m_embed[i * g_embedding_dims + k] += g_vel[i * g_embedding_dims
						+ k]
						* DELTATIME;
			}
		}
	}

	/*
	 * init embedding to a uniform random initialization in (-1,1) x (-1,1)
	 */
	void init_embedding() {

		g_heir = new int[50]; // handles up to 8^50 points
		g_levels = 0; // stores the point-counts at the associated levels

		// reset the embedding

		int N = m_gm.getNodeCount();
		if (areUsingSubset) {
			N = included_points.size();
		}
		
		m_embed = new float[N*g_embedding_dims];

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < g_embedding_dims; j++) {
				m_embed[i * (g_embedding_dims) + j] = ((float) (myRandom
						.nextInt(10000)) / 10000.f) - 0.5f;
			}
		}

		// calculate the heirarchy
		g_levels = fill_level_count(N, g_heir, 0);
		g_levels = 1;
		
		// generate vector data

		g_current_level = g_levels - 1;
		g_vel = new float[g_embedding_dims * N];
		g_force = new float[g_embedding_dims * N];

		// compute the index sets

		g_idx = new IndexType[N * (V_SET_SIZE + S_SET_SIZE)];
		for (int i = 0; i < g_idx.length; i++) {

			g_idx[i] = new IndexType();
		}

		g_done = false; // controls the movement of points
		g_interpolating = false; // specifies if we are interpolating yet

		g_cur_iteration = 0; // total number of iterations
		g_stop_iteration = 0; // total number of iterations since changing
								// levels
	}

	/*
	 * computes the input level hierarchy and size
	 */
	int fill_level_count(int input, int[] h, int levels) {

		h[levels] = input;
		if (input <= MIN_SET_SIZE)
			return levels + 1;
		return fill_level_count(input / DEC_FACTOR, h, levels + 1);
	}

	/*
	 * FORCE CONSTANTS
	 */
	static float SIZE_FACTOR = (1.f / ((float) (V_SET_SIZE + S_SET_SIZE)));
	static float DAMPING = (0.3f);
	static float SPRINGFORCE = (0.7f);
	static float DELTATIME = (0.3f);
	static float FREENESS = (0.85f);

	Random myRandom = null;
	int[] idx_map = null;
	int outDims = -1;
	boolean isShuffled = true;
	int stress_runs = 1;
	int g_levels = 0;
	int[] g_heir = null;
	float[] g_force = null; // pointer to embedding coords' force vectors
	float[] g_vel = null; // pointer to embedding coords' velocity vectors
	int g_current_level = 0; // current level being processed

	IndexType[] g_idx = null; // pointer to INDEXTYPE coords
	boolean g_done = false; // controls the movement of points
	boolean g_interpolating = false; // specifies if we are interpolating yet
	float[] sstress = new float[MAX_ITERATION];

	int g_cur_iteration = 0; // total number of iterations
	int g_stop_iteration = 0; // total number of iterations since changing
								// levels


	public GlimmerLayout(GraphManager gm) {

		m_gm = gm;
		this.myRandom = new Random();

		// initialize data
		init_embedding();

	}
}
