
public abstract class Player {

	int color;
	String name;
	public Player(int color, String name)
	{
		this.color = color;
		this.name = name;
	}
	
	abstract void playMove(ReversiBoard b);
	
	public String getName()
	{
		return this.name;
	}
	
}
