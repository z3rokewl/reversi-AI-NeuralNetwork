import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.structure.NetworkCODEC;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.engine.network.activation.ActivationLinear;

public class Main {
	static int boardSize = 6;
	static int generations = 500;
	static int layers = 3;
	static int populationSize = 20;
	static int updates = 10;
	static String l = "";
	// write a player's weights to a text file
	public static void savePlayer(NeuralPlayer p, int score)
	{
		try
		{
		  String filename = "board"+boardSize+" color"+p.color+" layers"+l+" gen"+generations+" pop"+populationSize+" up"+updates+" score"+score+".txt";
		  filename = filename.replace(" ", "_");
		  FileWriter fstream = new FileWriter(filename);
		  System.out.println("Wrote player to "+ filename);
		  BufferedWriter out = new BufferedWriter(fstream);
		  double[] genome = NetworkCODEC.networkToArray(p.net);
		  String genes = "";
		  for (int i = 0; i<genome.length; i++)
			  genes = genes + Double.toString(genome[i])+" ";
		  genes=genes.trim();
		  out.write(genes);
		  out.close();
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
	}
	// read in a players weights from a text file, have it play random 1000 times
	// and then have you play it... yes this is terrible code
	public static void readPlayer(String filename)
	{
		int color = 0;
		Scanner s = new Scanner(System.in);
		System.out.println("Enter the board size: ");
		boardSize = Integer.parseInt(s.next());
		System.out.println("Enter number of hidden layers: ");
		layers = Integer.parseInt(s.next());
		System.out.println("Enter color: ");
		color = Integer.parseInt(s.next());
		
		BasicNetwork n = new BasicNetwork();
		n.addLayer(new BasicLayer(null,false,boardSize*boardSize*2));
		
		for (int i =0; i< layers; i++)
		{
			System.out.println("Enter # of neurons in hidden layer "+i+": ");
			int neurons = Integer.parseInt(s.next());
			l = l + " " + Integer.toString(neurons);
			n.addLayer(new BasicLayer(new ActivationTANH(),true,neurons));//new ActivationSigmoid(),true,neurons));
		}
		
		n.addLayer(new BasicLayer(new ActivationLinear(),false,1));
		n.getStructure().finalizeStructure();
		n.reset();
		String w="";
		try
		{
			FileInputStream fstream = new FileInputStream(filename);
		    DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			 
			  while ((strLine = br.readLine()) != null)
				  w = w+strLine;
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		String[] weights = w.split(" ");
		double[] net = new double[weights.length];
		for (int i = 0; i<net.length; i++)
		{
			net[i] = Double.parseDouble(weights[i]);
		}
		NetworkCODEC.arrayToNetwork(net, n);
		NeuralPlayer player = new NeuralPlayer(color,"awesomeness",n);
		while (true)
		{
			Game gg;
			ReversiBoard b = new ReversiBoard(boardSize);
			if (color == 1)
			{
				HumanPlayer p1 = new HumanPlayer(-color,"Sean Rocks");
				RandomPlayer r = new RandomPlayer(-1,"Random");
				gg = new Game(player,r,b);
				gg.playGames(1000,true);
				gg = new Game(player,p1,b);
			}
			else
			{
				HumanPlayer p1 = new HumanPlayer(-color,"Sean Rocks");
				RandomPlayer r = new RandomPlayer(1,"Random");
				gg = new Game(r,player,b);
				gg.playGames(1000,true);
				gg = new Game(p1,player,b);
			}
			gg.playGames(1,true);
		}
	}
	// main prompts user to either evolve a population of reversi players
	// or play a player from file
	
	// Basically this algorithm has 2 populations of players, one for the 1st player
	// and one for the 2nd player. These populations play each other and each individual in
	// each population is assigned a fitness based on how many players it beats in the other 
	// population + the number of "champion" players it beats in the Hall of Fame (multiplied by
	// some constant). Those with the highest fitness breed and replace the worst players with
	// the offspring (1 point crossovers and mutation are used).
	// Every update # of generations, the populations play the random player and we can
	// see how they do against it... which basically measures absolute fitness of the players
	public static void main(String args[]) throws IOException
	{
		Scanner s = new Scanner(System.in);
		System.out.println("Play player from file(y/n)?");
		if (s.next().equals("y"))
		{
			System.out.println("Enter file name");
			String tmp = s.next();
			readPlayer(tmp);
		}
			
		System.out.println("Enter the board size: ");
		boardSize = Integer.parseInt(s.next());
		System.out.println("Enter number of hidden layers: ");
		layers = Integer.parseInt(s.next());
		System.out.println("Enter number of generations: ");
		generations = Integer.parseInt(s.next());
		System.out.println("Enter population size: ");
		populationSize = Integer.parseInt(s.next());
		System.out.println("Updates every: ");
		updates = Integer.parseInt(s.next());
		
		BasicNetwork n = new BasicNetwork();
		n.addLayer(new BasicLayer(null,false,boardSize*boardSize*2));
		
		for (int i =0; i< layers; i++)
		{
			System.out.println("Enter # of neurons in hidden layer "+i+": ");
			int neurons = Integer.parseInt(s.next());
			l = l + " " + Integer.toString(neurons);
			n.addLayer(new BasicLayer(new ActivationTANH(),true,neurons));
		}
		
		n.addLayer(new BasicLayer(new ActivationLinear(),false,1));
		n.getStructure().finalizeStructure();
		n.reset();
	
		CoEvolution co = new CoEvolution(n,populationSize,0.005,.25,boardSize);
		long start = System.currentTimeMillis();
		long last = start;
		int i;
		for (i =0; i <generations; i++)
		{
			System.out.println("---------------- Generation "+i+"-------------------\n");
			System.out.println(layers+" layers "+l+"\n");
			co.iterate();
			System.out.println("Running for "+(System.currentTimeMillis()-start)/1000 + " seconds");
			System.out.println("Generation took "+(System.currentTimeMillis()-last)/1000+ " seconds");
			last = System.currentTimeMillis();
			if (i%updates == 0)
				co.printStats(true); // test populations against random and update stats
			else
				co.printStats(false); // dont play random but print out stats for both populations
			if (System.in.available()!=0)
			{
				s.nextLine(); //prematurely end the evolution by entering input
				break;
			}
		}
		generations = i;
		System.out.println("Done\n");
		// get the best player as measured against the random player for both the 1st and 
		// 2nd players
		NeuralPlayer[] bestPlayers = co.bestIndividual(true);
		NeuralPlayer bestPlayer = bestPlayers[0];
		NeuralPlayer bestPPlayer = bestPlayers[1];
		
		// hosts are 1st player population
	    System.out.println("\nHost Results!");
		System.out.println("Neural Playing as Player 1 and Color 1 (how it was trained)");
		
		ReversiBoard b = new ReversiBoard(boardSize);
		RandomPlayer p3 = new RandomPlayer(-1,"Random");
		Game gg = new Game(bestPlayer,p3,b);
		int best = gg.playGames(1000,true)[0];
		savePlayer(bestPlayer, best);
		// perfectHosts are those players that at some point were able to beat random 100% of the
		// time... this is likely to happen if you have a large enough population or run for enough
		// generations even if your players only beat random 93% of the time on average
		for (NeuralPlayer p: co.perfectHosts)
		{
			b = new ReversiBoard(boardSize);
			gg = new Game(p,p3,b);
			int result = gg.playGames(1000,true)[0];
			savePlayer(p,result);
			if (result > best)
			{
				best = result;
				bestPlayer = p;
			}
		}
		// paraistes are 2nd player population
		System.out.println("\nParasite Results!");
		System.out.println("Neural Playing as Player 2 and Color -1 (how it was trained)");
		
		b = new ReversiBoard(boardSize);
		p3 = new RandomPlayer(1,"Random");
		gg = new Game(p3,bestPPlayer,b);
		best = gg.playGames(1000,true)[1];
		savePlayer(bestPPlayer, best);
		for (NeuralPlayer p: co.perfectParasites)
		{
			b = new ReversiBoard(boardSize);
			gg = new Game(p3,p,b);
			int result = gg.playGames(1000,true)[1];
			savePlayer(p,result);
			if (result > best)
			{
				best = result;
				bestPPlayer = p;
			}
		}
		
		co.printHistory(); // print list of average fitnesses against random
		while (true) // play the human player
		{
			System.out.println("\nPlay host");
			bestPlayer.color = 1;
			b = new ReversiBoard(boardSize);
			HumanPlayer p1 = new HumanPlayer(-1,"Sean Rocks");
			gg = new Game(bestPlayer,p1,b);
			gg.playGames(1,true);
			
			System.out.println("\nPlay parasite");
			b = new ReversiBoard(boardSize);
			p1 = new HumanPlayer(1,"Sean Rocks");
			gg = new Game(p1,bestPPlayer,b);
			gg.playGames(1,true);
		}
	
	}
	
}
