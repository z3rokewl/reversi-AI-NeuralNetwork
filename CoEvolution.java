import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.encog.neural.networks.structure.NetworkCODEC;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;

public class CoEvolution {
	// a bit messy :(
	double matingPer;
	double mutationPer;
	int popSize;
	int boardSize;
	BasicNetwork net;
	// populations that we are breeding
	NeuralPlayer[] hosts;
	NeuralPlayer[] parasites;
	// list of fitnesses against other population
	// ie [[index of player in hosts or parasites, score of player],[...]]
	double[][] pFitnesses; 
	double[][] hFitnesses;
	// list of fitnesses against random player
	// ie [[index of player in hosts or parasites, score of player],[...]]
	double[][] pRandFitnesses;
	double[][] hRandFitnesses;
	// best host player so far against random and its score against random
	double bestHost;
	NeuralPlayer allTimeBestHost;
	// best parasite player so far against random and its score against random
	double bestPara;
	NeuralPlayer allTimeBestPara;
	int generations = 0;
	// thread pool
	ExecutorService executor;
	// size of partitions of the populations that we give to various threads
	int partition;
	int numThreads;
	// multithreading crap
	Collection<Callable<Integer>> tasks;
	Collection<Callable<Integer>> hofTasks;
	// list of players that at some point beat random 100% of the time out 100 games
	// frequently this is just due to luck after a number of generations...
	ArrayList<NeuralPlayer> perfectHosts = new ArrayList<NeuralPlayer>();
	ArrayList<NeuralPlayer> perfectParasites = new ArrayList<NeuralPlayer>();
	// list of average fitness against random
	ArrayList<Double> pHistory = new ArrayList<Double>();
	ArrayList<Double> hHistory = new ArrayList<Double>();
	// list of hall of fame players... the last x # number of champions from previous generatiosn
	// get stored here (champions are the best palyer against the other population)
	NeuralPlayer[] hostHOF;
	NeuralPlayer[] parHOF;
	
	public CoEvolution(BasicNetwork net, int popSize, double mutationPer, double matingPer, int boardSize)
	{
		this.popSize = popSize;
		this.boardSize = boardSize;
		this.matingPer = matingPer;
		this.mutationPer = mutationPer;
		this.net = (BasicNetwork)net.clone();
		hosts = generatePopulation(popSize, net, 1);
		parasites = generatePopulation(popSize, net, -1);
		pFitnesses = new double[popSize][2];
		hFitnesses = new double[popSize][2];
		pRandFitnesses = new double[popSize][2];
		hRandFitnesses = new double[popSize][2];
		bestHost = 0;
		allTimeBestHost = hosts[0];
		bestPara = 0;
		allTimeBestPara = parasites[0];
		numThreads = Math.min(Runtime.getRuntime().availableProcessors(),100);
	    executor = Executors.newFixedThreadPool(numThreads);
	    
	    if (numThreads > 1) // leave one processor free
	    	numThreads--;
	    partition = popSize/numThreads; // not necessarily a true statement
	    System.out.println("Using "+numThreads+" processors");
	    
        tasks = new ArrayList<Callable<Integer>>(numThreads);
        hofTasks = new ArrayList<Callable<Integer>>(numThreads);
        // multithreading stuff... we can parallelize the fitness calculations 
        // which basically just have players play a bunch of games
		for (int i = 0 ; i < hosts.length; i+=partition)
		{
			Callable<Integer> worker = new fitnessCalculator(i,Math.min(i+partition,hosts.length),false);
			tasks.add(worker);
			
			Callable<Integer> worker2 = new HOFCalculator(i,Math.min(i+partition,hosts.length));
			hofTasks.add(worker2);
		}
		
		hostHOF = generatePopulation((int)(.1*popSize), net, 1);
		parHOF = generatePopulation((int)(.1*popSize), net, -1);
	}
	// allows you to more easily play around with seeding the neural nets
	// with different ranges of numbers... surprisingly this has a huge effect
	// (i.e. values between -1 and 1 do not seem to work very well)
	public static double randomNum(Random g)
	{
		return -5+g.nextDouble()*10;
	}
	
	public static NeuralPlayer[] generatePopulation(int size, BasicNetwork netType, int color)
	{
		NeuralPlayer[] newOrgs = new NeuralPlayer[size];
		Random g = new Random();
		for (int i = 0; i<size; i++)
		{
			BasicNetwork newNet = (BasicNetwork)netType.clone();
			newNet.reset();
			double[] genome1 = NetworkCODEC.networkToArray(newNet);
			for (int j = 0; j<genome1.length; j++)
				genome1[j] = randomNum(g);
			
			NetworkCODEC.arrayToNetwork(genome1, newNet);	
			newOrgs[i] = new NeuralPlayer(color, Integer.toString(i), newNet);
		}
		return newOrgs;
	}
	// prints out fitnesses against random and the other population
	// the stats against random will not be updated every generation unless
	// updates are set to 1 which slows stuff down
	public void printStats(boolean recalculate)
	{
		if (recalculate)
			bestIndividual(true);
		System.out.println("\nHosts");
		System.out.println("Best individual won "+hFitnesses[0][1]);
		System.out.println("Worst individual won "+hFitnesses[hFitnesses.length-1][1]);
		
		double avg = 0;
		for (int i =0; i< hFitnesses.length; i++)
			avg=avg+hFitnesses[i][1];
		avg=avg/hFitnesses.length;
		
		System.out.println("average wins is "+avg);
		avg = 0;
		for (int i =0; i< hRandFitnesses.length; i++)
			avg=avg+hRandFitnesses[i][1];
		avg=avg/hRandFitnesses.length;
		if (recalculate)
			hHistory.add(avg);
		System.out.println("**********************************************");
		System.out.println("average wins against random is "+avg);
		System.out.println("Best individual won "+hRandFitnesses[0][1]+" against Random");
		System.out.println("Worst individual won "+hRandFitnesses[hFitnesses.length-1][1]);
		System.out.println("**********************************************");
		
		System.out.println("\nParasites");
		System.out.println("Best individual won "+pFitnesses[0][1]);
		System.out.println("Worst individual won "+pFitnesses[pFitnesses.length-1][1]);
		
		avg = 0;
		for (int i =0; i< pFitnesses.length; i++)
			avg=avg+pFitnesses[i][1];
		avg=avg/pFitnesses.length;
		
		System.out.println("average wins is "+avg);
		avg = 0;
		for (int i =0; i< pRandFitnesses.length; i++)
			avg=avg+pRandFitnesses[i][1];
		avg=avg/pRandFitnesses.length;
		if (recalculate)
			pHistory.add(avg);
		System.out.println("**********************************************");
		System.out.println("average wins against random is "+avg);
		System.out.println("Best individual won "+pRandFitnesses[0][1]+" against Random");
		System.out.println("Worst individual won "+pRandFitnesses[hFitnesses.length-1][1]);
		System.out.println("**********************************************");
		
		if (perfectParasites.size() > 0)
			System.out.println("There are "+perfectParasites.size()+" perfect scoring parasites");
		
		if (perfectHosts.size() > 0)
			System.out.println("There are "+perfectHosts.size()+" perfect scoring hosts");
			
	}
	public void printHistory()
	{
		System.out.println("\nHost History");
		for (Double h : hHistory)
			System.out.print(h+" ");
		
		System.out.println("\nParasite History");
		for (Double p : pHistory)
			System.out.print(p+" ");
	}
	// returns best parasite and host players as measured against the random player
	// could be improved a lot
	public NeuralPlayer[] bestIndividual(boolean recalculate)
	{
		if (recalculate)
		{
			Collection<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>(numThreads);
			
			for (int i = 0 ; i < hosts.length; i+=partition)
			{
				Callable<Integer> worker = new fitnessCalculator(i,Math.min(i+partition,hosts.length),true);
				tasks.add(worker);
			}
			try
			{
				executor.invokeAll(tasks);
			}
			catch (Exception e)
			{
				System.out.println("Something bad happened...");
				System.exit(-1);
			}
			Arrays.sort(hRandFitnesses,new ArrayComparator(1,false));
			Arrays.sort(pRandFitnesses,new ArrayComparator(1,false));
			double bestScore = hRandFitnesses[0][1];
			double bestIndex = hRandFitnesses[0][0];
			
			if (bestScore >= bestHost)
			{
				if (bestScore == 100)
				{
					System.out.println("WE HAVE A PERFECT HOST");
					System.out.println("WE HAVE A PERFECT HOST");
					ReversiBoard b = new ReversiBoard(boardSize);
					RandomPlayer r = new RandomPlayer(-1, "Mr Random");
					Game gg = new Game(hosts[(int)bestIndex],r,b);
					double score1 = gg.playGames(1000,false)[0];
					
					gg = new Game(allTimeBestHost,r,b);
					double score2 = gg.playGames(1000,false)[0];
					
					if (score1 >= score2)
					{
						bestHost = bestScore;
						allTimeBestHost = new NeuralPlayer(1,"Da best H",(BasicNetwork)hosts[(int)bestIndex].net.clone());
					}
					perfectHosts.add(new NeuralPlayer(1,"A good H",(BasicNetwork)hosts[(int)bestIndex].net.clone()));
					
				}
				else
				{
					bestHost = bestScore;
					allTimeBestHost = new NeuralPlayer(1,"Da best H",(BasicNetwork)hosts[(int)bestIndex].net.clone());
				}
			}
			
			bestScore = pRandFitnesses[0][1];
			bestIndex = pRandFitnesses[0][0];
			
			if (bestScore >= bestPara)
			{
				if (bestScore == 100)
				{
					System.out.println("WE HAVE A PERFECT PARASITE");
					System.out.println("WE HAVE A PERFECT PARASITE");
					ReversiBoard b = new ReversiBoard(boardSize);
					RandomPlayer r = new RandomPlayer(1, "Mr Random");
					Game gg = new Game(r,parasites[(int)bestIndex],b);
					double score1 = gg.playGames(1000,false)[1];
					
					gg = new Game(allTimeBestPara,r,b);
					double score2 = gg.playGames(1000,false)[1];
					
					if (score1 >= score2)
					{
						bestPara = bestScore;
						allTimeBestPara = new NeuralPlayer(-1,"Da best P",(BasicNetwork)parasites[(int)bestIndex].net.clone());
					}
					perfectParasites.add(new NeuralPlayer(-1,"A good P",(BasicNetwork)parasites[(int)bestIndex].net.clone()));
				}
				else
				{
					bestPara = bestScore;
					allTimeBestPara = new NeuralPlayer(-1,"Da best P",(BasicNetwork)parasites[(int)bestIndex].net.clone());
				}
			}
		}
		NeuralPlayer[] result = {allTimeBestHost,allTimeBestPara};
		return result;
	}
	// take 2 organisms, cross over their weights, and dump those weights into a
	// new neural player
	private void mate(NeuralPlayer org1, NeuralPlayer org2, NeuralPlayer offspring)
	{
		BasicNetwork newGuy = (BasicNetwork)org1.net.clone();
		Random g = new Random();
		double[] genome1 = NetworkCODEC.networkToArray(org1.net);
		double[] genome2 = NetworkCODEC.networkToArray(org2.net);
		
		int numCrosses = 1;//g.nextInt(20)+2;//g.nextInt(genome1.length/4)+1;
		for (int j = 0; j< numCrosses; j++)
		{
			int crossIndex = g.nextInt(genome1.length);
			//int crossLength = g.nextInt(genome1.length)+1;//g.nextInt(50);
			//int stop = Math.min(crossIndex+crossLength,genome1.length);
			for (int i = crossIndex; i<genome1.length; i++)
				genome2[i] = genome1[i]; // 1 point crossover
		}
		NetworkCODEC.arrayToNetwork(genome2, newGuy);
		offspring.net = newGuy;
	}
	// possibly not the best way to mutate things (flipping bits may be better)
	// but basically has a chance of randomly changing each weight in a player 
	// with muationPer chance
	public void mutate(NeuralPlayer p)
	{
		double[] genome = NetworkCODEC.networkToArray(p.net);
		Random g = new Random();
		for (int i = 0; i< genome.length; i++)
		{
			if (g.nextDouble() <= mutationPer)
				genome[i] = randomNum(g) + genome[i];
		}
		NetworkCODEC.arrayToNetwork(genome, p.net);
	}
	// multithreading is tedious in java!
	public class fitnessCalculator implements Callable<Integer>
	{   
		int start;
		int end;
		boolean random;
		
		public fitnessCalculator(int start, int end, boolean random)
		{
			this.start = start;
			this.end = end;
			this.random = random;
		}
		@Override
		public Integer call()
		{
			if (random)
				calculateRandomFitness();
			else
				calculateFitness();
			return 1;
		}
		
		public void calculateFitness()
		{
			ReversiBoard b = new ReversiBoard(boardSize);
			Random rand = new Random();
			for (int h = start; h < end; h++)
			{
				int score = 0;
				for (int p = 0; p < popSize; p++)
				{
					Game g = new Game(hosts[h],parasites[p],b);
					int result = g.playGames(1,false)[0];
					score+=result;
					synchronized(CoEvolution.class)
					{
						pFitnesses[p][0] = p;
						pFitnesses[p][1]+=(1-result);
					}
				}
				hFitnesses[h][0] = h;
				hFitnesses[h][1] = score;
			}
				
		}
		
		public void calculateRandomFitness()
		{
			ReversiBoard b = new ReversiBoard(boardSize);
			RandomPlayer r = new RandomPlayer(-1, "Mr Random");
			RandomPlayer r2 = new RandomPlayer(1, "Mr Random");
			for (int h = start; h < end; h++)
			{
				
				Game gg = new Game(hosts[h],r,b);
				double score = gg.playGames(100,false)[0];
				hRandFitnesses[h][0] = h;
				hRandFitnesses[h][1] = score;
				
				gg = new Game(r2,parasites[h],b);
				score = gg.playGames(100,false)[1];
				pRandFitnesses[h][0] = h;
				pRandFitnesses[h][1] = score;
			}
		}
	}
	// more multithreading...
	public class HOFCalculator implements Callable<Integer>
	{   
		int start;
		int end;
		
		public HOFCalculator(int start, int end)
		{
			this.start = start;
			this.end = end;
		}
		@Override
		public Integer call()
		{
			calculate();
			return 1;
		}
		
		public void calculate()
		{
			ReversiBoard b = new ReversiBoard(boardSize);
			int l = parHOF.length;
			for (int p = start; p < end; p++)
			{
				for (int i = 0 ; i < l; i++)
				{
					Game g = new Game(hostHOF[i],parasites[p],b);
					int result = g.playGames(1, false)[1];
					pFitnesses[p][1] += result*popSize/l;
					
					g = new Game(hosts[p],parHOF[i],b);
					result = g.playGames(1, false)[0];
					hFitnesses[p][1] += result*popSize/l;
				}
			}
		}
	}
	// evolve the populations one generation
	public void iterate()
	{
		for (int p = 0; p < parasites.length; p++)
			pFitnesses[p][1] = 0;
		
		try
		{	// calculate fitnesses against other population
			executor.invokeAll(tasks);
			// calculate fitness against hall of fame individuals
			executor.invokeAll(hofTasks);
		}
		catch (Exception e)
		{
			System.out.println("Something bad happened...");
			System.exit(-1);
		}
	
		Arrays.sort(hFitnesses,new ArrayComparator(1,false));
		Arrays.sort(pFitnesses,new ArrayComparator(1,false));
		
		int bestIndex = (int)pFitnesses[0][0];
		int size = parHOF.length;
		
		parHOF[generations%size]=new NeuralPlayer(-1,"HOF P",(BasicNetwork)parasites[(int)bestIndex].net.clone());
		bestIndex = (int)hFitnesses[0][0];
		hostHOF[generations%size]=new NeuralPlayer(1,"HOF H",(BasicNetwork)hosts[(int)bestIndex].net.clone());
		
		int matePopIndex = (int)(matingPer*popSize)+1;
		Random indexG = new Random();
		// allow top percentage to breed and replace bottom 2*percentage
		// leave everyone else alone
		for (int i = 0; i < popSize*2*matingPer; i++)
		{
			int pInd1 = indexG.nextInt(matePopIndex);
			int pInd2 = indexG.nextInt(matePopIndex);
			NeuralPlayer p1 = hosts[(int)hFitnesses[pInd1][0]];
			NeuralPlayer p2 = hosts[(int)hFitnesses[pInd2][0]];
			NeuralPlayer child = hosts[(int)hFitnesses[popSize - 1 - i][0]];
			mate(p1, p2,child);
		}
		
		for (int i = 0; i < popSize*2*matingPer; i++)
		{
			int pInd1 = indexG.nextInt(matePopIndex);
			int pInd2 = indexG.nextInt(matePopIndex);
			NeuralPlayer p1 = parasites[(int)pFitnesses[pInd1][0]];
			NeuralPlayer p2 = parasites[(int)pFitnesses[pInd2][0]];
			NeuralPlayer child = parasites[(int)pFitnesses[popSize - 1 - i][0]];
			mate(p1, p2,child);
		}
		// mutate everyone except top percentage (matingPer)
		for (int i = 0; i < popSize*3*matingPer; i++)
		{
			mutate(parasites[(int)pFitnesses[popSize - 1 - i][0]]);
			mutate(hosts[(int)hFitnesses[popSize - 1 - i][0]]);
		}
		
		generations+=1;
		// every now and then reseed the population with a good individual
		if (generations%50 == 0)
		{
			hosts[indexG.nextInt(hosts.length)].net = (BasicNetwork)allTimeBestHost.net.clone();
			parasites[indexG.nextInt(parasites.length)].net = (BasicNetwork)allTimeBestPara.net.clone();
		}
			
	}
}