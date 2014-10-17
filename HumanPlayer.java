import java.util.Scanner;
public class HumanPlayer extends Player{

	Scanner s = new Scanner(System.in);
	public HumanPlayer(int color, String name)
	{
		super(color,name);
	}
	
	public HumanPlayer(ReversiBoard b, int color)
	{
		super(color,"Random Player");
	}
	
	public void playMove(ReversiBoard b)
	{
		String col = color == 1 ? "X" : "O";
		System.out.println();
		b.printBoard();
		int x=-1;
		int y=-1;
		String[] temp;
		String inputs;
		boolean moved = true;
		do {
			try
			{
				System.out.println("\nYour move for "+col+" (enter x y)\n");
				inputs = s.nextLine();
				
				if (inputs.equalsIgnoreCase("skip"))
				{
					moved = false;
					System.out.println();
					b.printBoard();
					return;
				}
				
				temp = inputs.split(" ");
				x = Integer.parseInt(temp[0]);
				y = Integer.parseInt(temp[1]);
			}
			catch(Exception e)
			{
				System.out.println("Please enter valid input");
				
			}
		} while (!b.isValid(x, y, color));
		if (moved)
			b.move(color, x, y);
		System.out.println();
		b.printBoard();
	}
}
