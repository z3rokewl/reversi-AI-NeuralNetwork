import java.util.ArrayList;
import java.util.Random;


public class RandomPlayer extends Player{

	public RandomPlayer(int color, String name)
	{
		super(color,name);
	}
	
	public RandomPlayer(int color)
	{
		super(color,"Random Player");
	}
	
	public void playMove(ReversiBoard b)
	{
		ArrayList<Integer> a = new ArrayList<Integer>();
		a = b.getMoves(color);
		Random g = new Random();
		if (a.size() == 0)
			return;
		int index = 2*g.nextInt(a.size()/2);
		b.move(color, a.get(index) ,a.get(index+1));
	}
}
