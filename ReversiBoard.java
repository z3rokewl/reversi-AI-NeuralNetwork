import java.util.ArrayList;
import java.util.Scanner;
import java.util.Random;

public class ReversiBoard {

	public int size; 
	public int[][] board;
	final static int[][] directions ={{-1,-1},{0,1},{1,0},{1,1},{-1,1},{1,-1},{0,-1},{-1,0}};
	 // 1 is white which is the first player to move, -1 is black, and 0 is an unoccupied square
	//public enum Color{WHITE, BLACK, UNOCCUPIED};
	public ReversiBoard(int size)
	{
		board = new int[size][size];
		if (size % 2 == 1)
		{
			System.out.println("Board size should be even\n");
			System.exit(-1);
		}
		this.size = size;
		
		for (int i = 0; i < size; i++)
			for (int j = 0; j< size; j++)
				board[j][i] = 0;
		
		board[size/2 - 1][size/2 - 1] = 1;
		board[size/2][size/2] = 1;
		board[size/2 - 1][size/2] = -1;
		board[size/2][size/2 - 1] = -1;	
	}
	
	public ReversiBoard(ReversiBoard b)
	{
		size = b.size;
		board = new int[b.size][b.size];
		for (int i = 0; i < size; i++)
			for (int j = 0; j < size; j++)
				board[j][i] = b.board[j][i];
	}
	
	public void reset()
	{
		for (int i = 0; i < size; i++)
			for (int j = 0; j< size; j++)
				board[j][i] = 0;
		
		board[size/2 - 1][size/2 - 1] = 1;
		board[size/2][size/2] = 1;
		board[size/2 - 1][size/2] = -1;
		board[size/2][size/2 - 1] = -1;	
	}
	
	public void printBoard()
	{
		System.out.print("  ");
		for (int i = 0; i< size; i++)
			System.out.print(i+ " ");
		System.out.println();
		for (int j = 0; j < size; j++)
		{
			System.out.print(j+" ");
			for (int i = 0; i< size; i++)
			{
				if (board[j][i] == 1)
					System.out.print("X ");
				else if(board[j][i] == -1)
					System.out.print("O ");
				else if (board[j][i] == 0)
					System.out.print("- ");
				else
					System.out.print("* ");
			}
			System.out.println();
		}
	}
	
	public boolean inBounds(int x, int y)
	{
		return x < size && x >=0 && y < size && y >=0;
	}
	
	public void move(int color, int x, int y)
	{
		board[y][x] = color;
		for (int dir = 0; dir < 8; dir++)
		{
			int j = y + directions[dir][1];
			int i = x + directions[dir][0];
			while (inBounds(i,j))
			{
				if (board[j][i] == 0)
					break;
				if (board[j][i] == color)
				{
					while (j != y || i != x)
					{
						board[j][i] = color;
						j = j - directions[dir][1];
						i = i - directions[dir][0];
					}
					break;
				}
				j = j + directions[dir][1];
				i = i + directions[dir][0];
			}
		}
		
	}
	public boolean isValid(int x, int y, int color)
	{
		boolean isLegal = false;
		if (x < 0 || y < 0 || x > board.length || y > board.length)
			return false;
		if (board[y][x] != 0)
			return false;
		for (int dir = 0; dir < 8; dir++)
		{
			int j = y + directions[dir][1];
			int i = x + directions[dir][0];
			while (inBounds(i,j))
			{
				if (board[j][i] == 0)
					break;
				if (board[j][i] == color)
				{
					if (j - directions[dir][1] != y || i - directions[dir][0]  != x)
						isLegal = true;
					break;
				}
				j = j + directions[dir][1];
				i = i + directions[dir][0];
			}
		}
		return isLegal;
	}
	public ArrayList<Integer> getMoves(int color)
	{
		ArrayList<Integer> a = new ArrayList<Integer>();
		for (int i=0; i<size; i++)
		{
			for (int j = 0; j<size; j++)
			{
				if (isValid(i,j,color))
				{
					a.add(i);
					a.add(j);
				}
			}
		}
		return a;		
	}
	
	public int getWinner()
	{
		return (getScore() >= 0 ? 1 : -1);
	}
	
	public int getScore()
	{
		int score = 0;
		for (int i = 0; i< size; i++)
			for (int j =0; j< size; j++)
				score+=board[j][i];
					
		return score;	
		
	}
	
	public boolean gameHasEnded()
	{
		return !(getMoves(1).size() > 0 || getMoves(-1).size() > 0);
	}
	
	public static void rotate(ReversiBoard b)
	{
		for (int row = 0; row < b.size/2; row++)
		{
			for (int col = row; col < b.size - 1 - row; col++)
			{
				int tmp = b.board[row][col];
				b.board[row][col] = b.board[b.size - 1 - col][row];
				b.board[b.size - 1 - col][row] = b.board[b.size - 1 - row][b.size - 1 - col];
				b.board[b.size - 1 - row][b.size - 1 - col] = b.board[col][b.size - 1 - row];
				b.board[col][b.size - 1 - row] = tmp;
			}
		}
	}
	
}
