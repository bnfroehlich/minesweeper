package bfroehlich.minesweeper;

import java.util.ArrayList;

public class FrontierSection {

	private ArrayList<Block> blocks;
	private String distribution;
	
	public FrontierSection(ArrayList<Block> blocks, String distribution) {
		this.blocks = blocks;
		this.distribution = distribution;
	}
	
	public ArrayList<Block> getBlocks() {
		return blocks;
	}
	
	public void setBlocks(ArrayList<Block> blocks) {
		this.blocks = blocks;
	}
	
	public String getDistribution() {
		return distribution;
	}
	
	public void setDistribution(String distribution) {
		this.distribution = distribution;
	}
	
}