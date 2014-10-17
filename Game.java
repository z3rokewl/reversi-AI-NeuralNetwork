
public class Game {

		ReversiBoard board;
		Player p2;
		Player p1;
		public Game(Player p1, Player p2, ReversiBoard b)
		{
			if (p1.color == p2.color)
				System.out.println("------------Players must be of different colors-----------------");
			this.p1 = p1;
			this.p2 = p2;
			this.board = b;
		}
		
		public int[] playGames(int n,boolean printScore)
		{
			int p1Wins = 0;
			int p2Wins = 0;
			for (int game = 0; game < n; game++)
			{			
				while (!board.gameHasEnded())
				{
					p1.playMove(board);
					p2.playMove(board);
				}
				if (board.getWinner() == p1.color)
					p1Wins++;
				else
					p2Wins++;
				board.reset();
			}
			if (printScore)
			{
				System.out.println(n + " games completed");
				System.out.println(p1.getName()+ " won " + p1Wins);
				System.out.println(p2.getName()+ " won " + p2Wins);
			}
			int[] ret = {p1Wins,p2Wins};
			return ret;
		}
}
