import java.util.ArrayList;

import org.encog.neural.networks.BasicNetwork;


public class NeuralPlayer extends Player{
	
	BasicNetwork net;
	
	public NeuralPlayer(int color, String name, BasicNetwork net)
	{
		super(color,name);
		this.net = net;
	}
	
	public void playMove(ReversiBoard b)
	{
		double[] output = new double[1];
		
		ArrayList<Integer> moves = b.getMoves(this.color);
		double max = -Double.MAX_VALUE;
		int[] bestMove = new int[2];
		
		for (int move = 0; move <moves.size(); move+=2)
		{
			double score = 0;
			ReversiBoard copy = new ReversiBoard(b);
			copy.move(color, moves.get(move), moves.get(move+1));
			// each square on the board is represented by 2 input neurons
			double[] input = new double[b.size*b.size*2];
			
			// rotate board 4 times, average the nn score of all 4 positions
			// this is not necessary and greatly slows things down but it does
			// improve the consistency of play and overall fitness
			for (int rot = 0; rot < 4; rot++)
			{
				int c = 0;
				for (int i = 0; i <b.size; i++)
				{
					for (int j = 0; j<b.size; j++)
					{
						if (copy.board[j][i] == this.color)
						{
							input[c] = 1;
							input[c+1] = 0;
						}
						else if (copy.board[j][i] == -this.color)
						{
							input[c] = 0;
							input[c+1] = 1;
						}
						else
						{
							input[c] = 0;
							input[c+1] = 0;
						}
						c+=2;
					}
				}
			
		
				net.compute(input,output);
				score+=output[0];
			
				if (rot != 3)
					ReversiBoard.rotate(copy);
			}
			
			if (score >= max)
			{
				max = score;
				bestMove[0] = moves.get(move);
				bestMove[1] = moves.get(move+1);
			}
		}
		
		if (moves.size() > 0)
			b.move(color, bestMove[0], bestMove[1]);
	}
	
}
