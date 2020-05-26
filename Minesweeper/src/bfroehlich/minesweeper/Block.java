package bfroehlich.minesweeper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

public class Block extends JLabel {

	public enum Status {
		Revealed, Unmarked, Flagged, BadFlagged, HighlightRed, HighlightGreen, Detonated
	}
	
	private static HashMap<Integer, Image> mineImages;
	private static HashMap<Integer, Image> flagImages;
	private static HashMap<Integer, Image> brokenFlagImages;	
	private static HashMap<Integer, HashMap<Integer, Image>> numberImages;
	private static Border raisedBorder;
	private static Border sunkenBorder;
	private static Border raisedSelectedBorder;
	private static Border sunkenSelectedBorder;
	
	private boolean hasMine;
	private int neighborMines;
	private int gridX;
	private int gridY;
	private int size;
	private Status status;
	
	private boolean visible;
	private boolean selected;
	
	public Block(int gridX, int gridY, int size) {
		this.gridX = gridX;
		this.gridY = gridY;
		this.size = size;
		
		visible = true;
		
		setStatus(Status.Unmarked);

		if(raisedBorder == null) {
			raisedBorder = BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.WHITE, Color.BLACK);
		}
		if(sunkenBorder == null) {
			sunkenBorder = BorderFactory.createLineBorder(Color.BLACK);
		}
		if(raisedSelectedBorder == null) {
			raisedSelectedBorder = BorderFactory.createLineBorder(Color.BLUE, 5);
			//raisedSelectedBorder = BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.CYAN, Color.BLUE);
		}
		if(sunkenSelectedBorder == null) {
			sunkenSelectedBorder = BorderFactory.createLineBorder(Color.BLUE, 5);
		}
		setBorder(raisedBorder);
		setPreferredSize(new Dimension(size, size));
		setOpaque(true);
		setHorizontalAlignment(CENTER);
		setFont(new Font("Times New Roman", Font.BOLD, 20));

		if(mineImages == null) {
			mineImages = new HashMap<Integer, Image>();
		}
		if(!mineImages.containsKey(size)) {
			mineImages.put(size, Main.loadImage("mine.png", size, size, false));
		}
		
		if(flagImages == null) {
			flagImages = new HashMap<Integer, Image>();
		}
		if(!flagImages.containsKey(size)) {
			flagImages.put(size, Main.loadImage("flag5.png", size, size, false));
		}
		
		if(brokenFlagImages == null) {
			brokenFlagImages = new HashMap<Integer, Image>();
		}
		if(!brokenFlagImages.containsKey(size)) {
			brokenFlagImages.put(size, Main.loadImage("xflag5.png", size, size, false));
		}
		
		if(numberImages == null) {
			numberImages = new HashMap<Integer, HashMap<Integer, Image>>();
		}
		if(!numberImages.containsKey(size)) {
			HashMap<Integer, Image> sizeNumberImages = new HashMap<Integer, Image>();
			for(int i = 1; i <= 8; i++) {
				sizeNumberImages.put(i, Main.loadImage(i + ".png", (int) (0.75*((double) size)), (int) (0.75*((double) size)), false));
			}
			numberImages.put(size, sizeNumberImages);
		}
	}
	
	public int getGridX() {
		return gridX;
	}
	
	public int getGridY() {
		return gridY;
	}
	
	public boolean hasMine() {
		return hasMine;
	}
	
	public void setHasMine(boolean hasMine) {
		this.hasMine = hasMine;
	}
	
	public int getNeighborMines() {
		return neighborMines;
	}
	
	public void setNeighborMines(int neighborMines) {
		this.neighborMines = neighborMines;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
		updateAppearance();
	}
	
	public void setVisible(boolean visible) {
		this.visible = visible;
		updateAppearance();
	}
	
	public boolean getVisible() {
		return visible;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
		updateAppearance();
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	private void updateAppearance() {
		boolean raised = false;
		
		if(!visible) {
			//not visible appears unmarked
			setText("");
			setIcon(null);
			raised = true;
			setBackground(Color.LIGHT_GRAY);
		}
		else {
			if(status == Status.Flagged) {
				setText("");
				setIcon(new ImageIcon(flagImages.get(size)));
				raised = true;
				setBackground(Color.LIGHT_GRAY);
			}
			else if(status == Status.Unmarked) {
				setText("");
				setIcon(null);
				raised = true;
				setBackground(Color.LIGHT_GRAY);
			}
			else if(status == Status.Revealed) {
				if(hasMine) {
					setText("");
					raised = false;
					setIcon(new ImageIcon(mineImages.get(size)));
					setBackground(Color.WHITE);
				}
				else {
					setBackground(Color.WHITE);
					setIcon(null);
					raised = false;
					if(neighborMines > 0) {
						setText("");
						raised = false;
						setIcon(new ImageIcon(numberImages.get(size).get(neighborMines)));
						setBackground(Color.WHITE);
	//					setText("" + neighborMines);
	//					Color c = Color.BLACK;
	//					switch(neighborMines) {
	//					case(1) : c = new Color(1, 0, 254); break;
	//					case(2) : c = new Color(1, 127, 1); break;
	//					case(3) : c = new Color(254, 0, 0); break;
	//					case(4) : c = new Color(0, 0, 127); break;
	//					case(5) : c = new Color(128, 0, 0); break;
	//					case(6) : c = new Color(0, 128, 129); break;
	//					case(7) : c = new Color(0, 0, 0); break;
	//					case(8) : c = new Color(128, 128, 128);
	//					}
	//					setForeground(c);
					}
				}
			}
			else if(status == Status.Detonated) {
				setText("");
				raised = false;
				setIcon(new ImageIcon(mineImages.get(size)));
				setBackground(Color.RED);
				//setBackground(new Color(255,69,0));
			}
			else if(status == Status.BadFlagged) {
				setText("");
				setBackground(Color.LIGHT_GRAY);
				raised = true;
				setIcon(new ImageIcon(brokenFlagImages.get(size)));
			}
			else if(status == Status.HighlightRed) {
				setText("");
				setIcon(null);
				raised = true;
				setBackground(Color.RED);
			}
			else if(status == Status.HighlightGreen) {
				setText("");
				setIcon(null);
				raised = true;
				setBackground(Color.GREEN);
			}
		}
		
		if(selected) {
			if(raised) {
				setBorder(raisedSelectedBorder);
			}
			else {
				setBorder(sunkenSelectedBorder);
			}
		}
		else {
			if(raised) {
				setBorder(raisedBorder);
			}
			else {
				setBorder(sunkenBorder);
			}
		}
	}
	
	public String toString() {
		return "(" + gridX + ", " + gridY + ")";
	}

//	public boolean isFlagged() {
//		return flagged;
//	}
//
//	public void setFlagged(boolean flagged) {
//		this.flagged = flagged;
//	}
//
//	public boolean isRevealed() {
//		return revealed;
//	}
//
//	public void setRevealed(boolean revealed) {
//		this.revealed = revealed;
//	}
}