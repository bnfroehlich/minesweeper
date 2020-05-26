package bfroehlich.minesweeper;

import java.util.ArrayList;

public class Zone {

	private ArrayList<Block> sourceBlocks;
	private ArrayList<Block> blocks;
	private int mines;
	
	public Zone(ArrayList<Block> sourceBlocks, ArrayList<Block> blocks, int mines) {
		//a Zone is a group of blocks which are known to contain in total a certain number of mines
		//the precise location of the mines is unknown
		//mines = -1 means number of mines unknown
		this.sourceBlocks = sourceBlocks;
		this.blocks = blocks;
		this.mines = mines;
	}
	
	public ArrayList<Block> getSourceBlocks() {
		return sourceBlocks;
	}

	public void setSourceBlock(ArrayList<Block> sourceBlock) {
		this.sourceBlocks = sourceBlock;
	}

	public ArrayList<Block> getBlocks() {
		return blocks;
	}

	public void setBlocks(ArrayList<Block> blocks) {
		this.blocks = blocks;
	}

	public int getMines() {
		return mines;
	}

	public void setMines(int mines) {
		this.mines = mines;
	}
	
	public String toString() {
		String string = "Zone of ";
		for(Block block : sourceBlocks) {
			string += block.toString();
		}
		string += ": ";
		for(Block block : blocks) {
			string += block + ", "; 
		}
		string += mines;
		return string;
	}
}