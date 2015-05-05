package homecontrolserver;

public class GarageDoor
{
	private boolean bLeftDoorOpen, bRightDoorOpen;
	
	public GarageDoor(boolean bLeftDoorOpen, boolean bRightDoorOpen)
	{
		this.bLeftDoorOpen = bLeftDoorOpen;
		this.bRightDoorOpen = bRightDoorOpen;
	}
	
	//getters
	public boolean isLeftDoorOpen() { return bLeftDoorOpen; }
	public boolean isRightDoorOpen() { return bRightDoorOpen; }
	
	//setters
	public void setLeftDoorOpen(boolean tf) { bLeftDoorOpen = tf;}
	public void setRightDoorOpen(boolean tf) { bRightDoorOpen = tf;}
}

