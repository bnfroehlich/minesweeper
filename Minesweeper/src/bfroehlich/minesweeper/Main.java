package bfroehlich.minesweeper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;


public class Main extends JFrame {

	public enum GameStatus {
		Won, Lost, Ongoing;
	}

	public static void main(String[] args) {
		new Main().setVisible(true);
	}

	private JPanel field;
	private JRadioButton beginner;
	private JRadioButton medium;
	private JRadioButton expert;
	private JCheckBox alwaysSolvable;
	private JCheckBox keyboardControlsEnabled;
	private Random rand;
	private JLabel flagsRemainingLabel;
	private JLabel trophyLabel;
	private JLabel timerLabel;
	private JButton newGame;

	private JButton bigBrainButton;
	private JButton quickSolve;
	private JButton test;
	private JButton testQuickSolve;
	private JCheckBox stopOnComplexBoard;
	private JCheckBox stopOnNoSafeMoves;
	private boolean abort;
	private boolean next;
	private boolean resumeWithBruteForce;
	private boolean abortSafePointSearch;
	private boolean userClickedAbortSafePointSearch;
	private int maxRecommendedUnknowns;

	private ArrayList<ArrayList<Block>> blocks;
	private Block selectedBlock;
	private int minesNeeded;
	private boolean minesPlaced;
	private boolean awaitingClick;
	private GameStatus status;
	private Thread timerThread;
	private Timer timer;

	private FlowLayout flow;
	private Image trophy;
	private Image skull;
	private Image searching;

	public Main() {
		super("Minesweeper");
		
		maxRecommendedUnknowns = 16;
		rand = new Random();
		trophy = loadImage("trophy.png", 50, 50, false);
		skull = loadImage("skull.png", 50, 50, false);
		searching = loadImage("searching.png", 50, 50, false);
		status = null;
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		init();
	}

	private void init() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);

		JPanel top = new JPanel();
		flow = new FlowLayout(FlowLayout.CENTER, 10, 10);
		top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
		panel.add(top);
		
		JPanel topLeft = new JPanel();
		topLeft.setPreferredSize(new Dimension(200, 70));
		//topLeft.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.BLACK));
		topLeft.setLayout(new BoxLayout(topLeft, BoxLayout.Y_AXIS));
		top.add(topLeft);
		flagsRemainingLabel = new JLabel();
		flagsRemainingLabel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
		flagsRemainingLabel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		flagsRemainingLabel.setIcon(new ImageIcon(loadImage("flag5.png", 50, 50, false)));
		Font font = new Font("Times New Roman", Font.BOLD, 30);
		flagsRemainingLabel.setFont(font);
		topLeft.add(flagsRemainingLabel);

		JPanel topMid = new JPanel();
		topMid.setPreferredSize(new Dimension(200, 70));
		//topMid.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.BLACK));
		top.add(topMid);
		trophyLabel = new JLabel();
		topMid.add(trophyLabel);
		
		JPanel topRight = new JPanel();
		topRight.setPreferredSize(new Dimension(200, 70));
		//topRight.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.BLACK, Color.BLACK));
		topRight.setLayout(new BoxLayout(topRight, BoxLayout.Y_AXIS));
		top.add(topRight);
		timerLabel = new JLabel("0");
		timerLabel.setFont(font);
		timerLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		timerLabel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		topRight.add(timerLabel);

		field = new JPanel();
		panel.add(field);

		JPanel bottom = new JPanel();
		bottom.setLayout(flow);
		panel.add(bottom);

		JPanel difficultyPanel = new JPanel();
		difficultyPanel.setLayout(new BoxLayout(difficultyPanel, BoxLayout.Y_AXIS));
		bottom.add(difficultyPanel);
		ButtonGroup difficultyGroup = new ButtonGroup();
		beginner = new JRadioButton("Beginner");
		beginner.setSelected(true);
		difficultyPanel.add(beginner);
		difficultyGroup.add(beginner);
		medium = new JRadioButton("Medium");
		difficultyPanel.add(medium);
		difficultyGroup.add(medium);
		expert = new JRadioButton("Expert");
		difficultyPanel.add(expert);
		difficultyGroup.add(expert);
		
		alwaysSolvable = new JCheckBox("Always solvable");
		alwaysSolvable.setSelected(true);
		difficultyPanel.add(alwaysSolvable);
		
		keyboardControlsEnabled = new JCheckBox("Keyboard controls");
		keyboardControlsEnabled.setSelected(false);
		difficultyPanel.add(keyboardControlsEnabled);

		newGame = new JButton("New Game");
		newGame.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newGame();
			}
		});
		bottom.add(newGame);
		bigBrainButton = new JButton("Bigbrain");
		Main main = this;
		bigBrainButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					bigBrain(true, false, true);
				}
				catch(BadBoardException ex) {
					if(ex.getMessage().contains("large")) {
						int tryAnyway = JOptionPane.showConfirmDialog(main,
								"Board too complex.\n\n" + ex.getMessage() + "\n\nMax recommended unknowns per section = " + maxRecommendedUnknowns + ". Try anyway?",
								"Why?", JOptionPane.YES_NO_OPTION);
						if(tryAnyway == JOptionPane.YES_OPTION) {
							try {
								bigBrain(true, true, true);
							}
							catch(BadBoardException ex2) {
								JOptionPane.showMessageDialog(main, ex.getMessage());
							}
							catch(BugException ex3) {
								JOptionPane.showMessageDialog(main, ex.getMessage());
							}
						}
					}
					else {
						JOptionPane.showMessageDialog(main, ex.getMessage());
					}
				}
				catch(BugException ex) {
					JOptionPane.showMessageDialog(main, ex.getMessage());
				}
			}
		});
		bigBrainButton.setEnabled(false);
		bottom.add(bigBrainButton);
		
		quickSolve = new JButton("Quicksolve");
		quickSolve.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				quickSolveAndDisplayOutput();
			}
		});
		bottom.add(quickSolve);
		quickSolve.setEnabled(false);

		JPanel testPanel = new JPanel();
		testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.Y_AXIS));
		bottom.add(testPanel);

		stopOnComplexBoard = new JCheckBox("Stop on complex board");
		testPanel.add(stopOnComplexBoard);
		stopOnNoSafeMoves = new JCheckBox("Stop on no safe moves");
		testPanel.add(stopOnNoSafeMoves);

		JTextField testInput = new JTextField("100", 4);
		testPanel.add(testInput);	

		test = new JButton("Test Bigbrain");
		test.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Integer testNum = null;
				try {
					testNum = Integer.parseInt(testInput.getText());
					test(testNum, "Bigbrain");
				}
				catch(NumberFormatException ex) {}
			}
		});
		testPanel.add(test);	
		
		testQuickSolve = new JButton("Test Quicksolve");
		testQuickSolve.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Integer testNum = null;
				try {
					testNum = Integer.parseInt(testInput.getText());
					test(testNum, "Quicksolve");
				}
				catch(NumberFormatException ex) {}
			}
		});
		testPanel.add(testQuickSolve);	

		pack();
		
		KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		manager.addKeyEventDispatcher(new KeyEventDispatcher() {
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e.getID() == KeyEvent.KEY_PRESSED) {
					int code = e.getKeyCode();
					System.out.println("key released: " + code);
					keyReleased(code);
				}
				else if (e.getID() == KeyEvent.KEY_RELEASED) {
//					int code = e.getKeyCode();
//					System.out.println("key released: " + code);
//					keyReleased(code);
				}
				else if (e.getID() == KeyEvent.KEY_TYPED) {
				}
				return false;
			}
		});
	}
	
	private void makeTimerThread() {
		timerThread = new Thread(new Runnable() {
			public void run() {
				timer = new Timer();
				while(status == GameStatus.Ongoing) {
					try {
						Thread.sleep(100);
					}
					catch(InterruptedException ie) {
						break;
					}
					timerLabel.setText("" + (timer.read()/1000));
				}
			}
		});
		//System.out.println("Current thread: " + Thread.currentThread().getName() + ". starting timerThread: " + timerThread.getName());
		timerThread.setPriority(Thread.MIN_PRIORITY);
		timerThread.start();
	}

	private void newGame() {
		resetUI();
		if(timerThread != null && timerThread.isAlive()) {
			timerThread.interrupt();
		}
		makeTimerThread();

		int gridWidth = 9;
		int gridHeight = 9;
		int mines = 10;
		if(medium.isSelected()) {
			gridWidth = 16;
			gridHeight = 16;
			mines = 40;
		}
		else if(expert.isSelected()) {
			gridWidth = 30;
			gridHeight = 16;
			mines = 99;
		}
		field.removeAll();
		field.setLayout(new GridLayout(gridHeight, gridWidth));
		blocks = new ArrayList<ArrayList<Block>>();
		minesPlaced = false;
		minesNeeded = mines;
		flagsRemainingLabel.setText("" + minesNeeded);

		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		double screenHeight = (double) screenSize.getHeight();
		
		for(int x = 0; x < gridWidth; x++) {
			ArrayList<Block> col = new ArrayList<Block>();
			blocks.add(col);
			for(int y = 0; y < gridHeight; y++) {
				int xx = x;
				int yy = y;
				int blockSize = (int) ((screenHeight*0.6)/(double) gridHeight);
				Block block = new Block(xx, yy, blockSize);
				col.add(block);
				block.addMouseListener(new MouseListener() {
					public void mouseReleased(MouseEvent e) {
						blockClicked(block, e.getButton() == 1);
					}
					public void mousePressed(MouseEvent e) {}
					public void mouseExited(MouseEvent e) {}
					public void mouseEntered(MouseEvent e) {}
					public void mouseClicked(MouseEvent e) {}
				});
			}
		}
		for(int y = 0; y < gridHeight; y++) {
			for(int x = 0; x < gridWidth; x++) {
				field.add(blocks.get(x).get(y));
			}
		}

		ArrayList<Block> aBlocks = getBlocks();
		Block firstBlock = aBlocks.get(rand.nextInt(aBlocks.size()));
		setSelectedBlock(firstBlock);
		
		pack();
	}
	
	private void resetUI() {
		trophyLabel.setIcon(null);
		bigBrainButton.setEnabled(true);
		quickSolve.setEnabled(true);
		status = GameStatus.Ongoing;
		awaitingClick = true;
	}

	private void placeMines(Block firstClick) {
		
		int mines = minesNeeded;
		
		//the firstClick and its neighbors are forbidden to contain mines
		ArrayList<Block> neighbors = getNeighbors(firstClick);
		ArrayList<Point> forbidden = new ArrayList<Point>();
		forbidden.add(new Point(firstClick.getGridX(), firstClick.getGridY()));
		//ArrayList<Block> neighbors = getNeighbors(block);
		for(Block neighbor : neighbors) {
			forbidden.add(new Point(neighbor.getGridX(), neighbor.getGridY()));
		}
		
		JFrame msgFrame = new JFrame("Message");
		JPanel msgPanel = new JPanel();
		msgPanel.setLayout(flow);
		msgFrame.add(msgPanel);
		JLabel msgLabel = new JLabel("Placing mines. Always solvable: " + alwaysSolvable.isSelected());
		msgPanel.add(msgLabel);

		msgFrame.setPreferredSize(new Dimension(300, 200));
		msgFrame.pack();
		msgFrame.setLocation(getLocation().x + getSize().width/2 - msgFrame.getSize().width/2, getLocation().y + getSize().height/2 - msgFrame.getSize().height/2);
		msgFrame.setVisible(true);
		
		Thread minePlacerThread = new Thread(new Runnable() {
			public void run() {
				boolean stillNeedToPlaceMines = true;
				while(stillNeedToPlaceMines) {
					try {
						Thread.sleep(10);
					}
					catch(InterruptedException ie) {}
					int width = blocks.size();
					int height = blocks.get(0).size();
					ArrayList<Point> mineCoors = new ArrayList<Point>();
					for(int i = 0; i < mines; i++) {
						Point p = new Point(rand.nextInt(width), rand.nextInt(height));
						while(mineCoors.contains(p) || (forbidden != null && forbidden.contains(p))) {
							p = new Point(rand.nextInt(width), rand.nextInt(height));
						}
						mineCoors.add(p);
					}
					for(Point p : mineCoors) {
						blocks.get(p.x).get(p.y).setHasMine(true);
					}
					for(int x = 0; x < width; x++) {
						for(int y = 0; y < height; y++) {
							Block b = blocks.get(x).get(y);
							int neighborMines = 0;
							for(Block neighbor : getNeighbors(b)) {
								if(neighbor.hasMine()) {
									neighborMines++;
								}
							}
							b.setNeighborMines(neighborMines);
						}
					}
					if(alwaysSolvable.isSelected()) {
						reveal(firstClick);
						stillNeedToPlaceMines = !isBoardSolvable();
						if(stillNeedToPlaceMines) {
							for(Block b : getBlocks()) {
								b.setHasMine(false);
								b.setNeighborMines(0);
							}
						}
						for(Block b : getBlocks()) {
							b.setStatus(Block.Status.Unmarked);
						}
					}
					else {
						stillNeedToPlaceMines = false;
					}
				}
			}
		});
		//System.out.println("Current thread: " + Thread.currentThread().getName() + ".\t\t\tstarting minePlacerThread: " + minePlacerThread.getName());
		minePlacerThread.setPriority(Thread.MIN_PRIORITY);
		minePlacerThread.start();
		
		try {
			minePlacerThread.join();
		}
		catch(InterruptedException ie) {}

		msgFrame.dispatchEvent(new WindowEvent(msgFrame, WindowEvent.WINDOW_CLOSING));		
		
		minesPlaced = true;
		if(timerThread == null || !timerThread.isAlive()) {
			makeTimerThread();
		}
		if(timer != null) {
			timer.start();
		}
	}
	
	private boolean isBoardSolvable() {
		setBlocksVisible(false);
		boolean solvable = false;
		while(status == GameStatus.Ongoing) {
			int flagsRemainingBeforeBigBrain = flagsRemaining();
			ArrayList<Point> safePoints = null;
			try {
				safePoints = bigBrain(false, false, false);
			}
			catch(BugException be) {
				solvable = false;
				break;
			}
			catch(BadBoardException bbe) {
				//board too complex
				solvable = false;
				break;
			}
			if(safePoints.isEmpty() && flagsRemainingBeforeBigBrain == flagsRemaining()) {
				//if we couldn't find any safe points or place any flags
				solvable = false;
				break;
			}
			for(Point p : safePoints) {
				reveal(blocks.get(p.x).get(p.y));
			}
		}
		if(status == GameStatus.Won) {
			//game is won successfully
			solvable = true;
		}
		
		for(Block block : getBlocks()) {
			setFlagged(block, false);
			block.setStatus(Block.Status.Unmarked);
		}
		setBlocksVisible(true);
		resetUI();
		
		return solvable;
	}

	private void blockClicked(Block block, boolean leftClick) {
		ArrayList<Block> neighbors = getNeighbors(block);
		String neighCoor = "";
		for(Block n : neighbors) {
			neighCoor += "(" + n.getGridX() + ", " + n.getGridY() + "), ";
		}
		//System.out.println(block.getGridX() + ", " + block.getGridY() + ". " + neighCoor);
		if(!awaitingClick) {
			return;
		}
		if(!leftClick) {
			if(block.getStatus() == Block.Status.Unmarked) {
				setFlagged(block, true);
			}
			else if(block.getStatus() == Block.Status.Flagged) {
				setFlagged(block, false);
			}
		}
		else if(leftClick) {
			if(!(block.getStatus()== Block.Status.Flagged)) {
				if(!minesPlaced) {
					placeMines(block);
					setSelectedBlock(block);
				}
				reveal(block);
			}
		}
		flagsRemainingLabel.setText("" + flagsRemaining());
		if(status == GameStatus.Ongoing) {
			setSelectedBlock(block);
		}
			
		repaint();
	}
	
	private void keyReleased(int code) {
		if(selectedBlock != null) {
			if(code == KeyEvent.VK_ENTER) {
				//press enter same as left click (reveal)
				blockClicked(selectedBlock, true);
			}
			else if(code == KeyEvent.VK_1 || code == KeyEvent.VK_NUMPAD1) {
				//press 1 same as right click (flag)
				blockClicked(selectedBlock, false);
			}
			else if(code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
				Block newSelectedBlock = null;
				Point coors = new Point(selectedBlock.getGridX(), selectedBlock.getGridY());
				try {
					if(code == KeyEvent.VK_UP) {
						newSelectedBlock = blocks.get(coors.x).get(coors.y - 1);
					}
					else if(code == KeyEvent.VK_DOWN) {
						newSelectedBlock = blocks.get(coors.x).get(coors.y + 1);
					}
					else if(code == KeyEvent.VK_LEFT) {
						newSelectedBlock = blocks.get(coors.x - 1).get(coors.y);
					}
					else if(code == KeyEvent.VK_RIGHT) {
						newSelectedBlock = blocks.get(coors.x + 1).get(coors.y);
					}
					setSelectedBlock(newSelectedBlock);
				}
				catch(IndexOutOfBoundsException e) {}
			}
		}
	}

	private void win() {
		status = GameStatus.Won;
		gameOver();
		trophyLabel.setIcon(new ImageIcon(trophy));
		pack();
	}

	private void boom(Block detonated) {
		status = GameStatus.Lost;
		gameOver();
		for(Block block : getBlocks()) {
			if(block.hasMine() && !(block.getStatus() == Block.Status.Flagged)) {
				block.setStatus(Block.Status.Revealed);
			}
			else if(block.getStatus() == Block.Status.Flagged && !block.hasMine()) {
				block.setStatus(Block.Status.BadFlagged);
			}
		}
		detonated.setStatus(Block.Status.Detonated);
		trophyLabel.setIcon(new ImageIcon(skull));
		pack();
	}

	private void gameOver() {
		awaitingClick = false;
		bigBrainButton.setEnabled(false);
		quickSolve.setEnabled(false);
		setSelectedBlock(null);
	}

	private void reveal(Block block) {
		block.setStatus(Block.Status.Revealed);
		if(block.hasMine()) {
			boom(block);
		}
		else {
			if(!block.hasMine() && block.getNeighborMines() == 0) {
				//expand
				for(Block neighbor : getNeighbors(block)) {
					if(!(neighbor.getStatus() == Block.Status.Revealed) && !(neighbor.getStatus() == Block.Status.Flagged)) {
						reveal(neighbor);
					}
				}
			}

			int unmarkedBlocks = 0;
			for(Block b : getBlocks()) {
				if(b.getStatus() == Block.Status.Unmarked || b.getStatus() == Block.Status.HighlightRed || b.getStatus() == Block.Status.HighlightRed) {
					unmarkedBlocks++;
				}
			}
			if(unmarkedBlocks == flagsRemaining() && flagsRemaining() >= 0) {
				for(Block b : getBlocks()) {
					if(b.getStatus() == Block.Status.Unmarked || b.getStatus() == Block.Status.HighlightRed || b.getStatus() == Block.Status.HighlightRed) {
						setFlagged(b, true);
					}
				}
				win();
			}
			flagsRemainingLabel.setText("" + flagsRemaining());
		}
	}
	
	private void setBlocksVisible(boolean visible) {
		for(Block block : getBlocks()) {
			block.setVisible(visible);
		}
	}
	
	private void setSelectedBlock(Block newSelectedBlock) {
		if(selectedBlock != null) {
			selectedBlock.setSelected(false);
		}
		if(keyboardControlsEnabled.isSelected()) {
			if(newSelectedBlock != null) {
				newSelectedBlock.setSelected(true);
			}
			selectedBlock = newSelectedBlock;
		}
		else {
			selectedBlock = null;
		}
	}

	private int flagsRemaining() {
		int flagsPlaced = 0;
		for(Block b : getBlocks()) {
			if(b.getStatus() == Block.Status.Flagged) {
				flagsPlaced++;
			}
		}
		return minesNeeded - flagsPlaced;
	}

	private ArrayList<Block> getNeighbors(Block b) {
		ArrayList<Block> neighbors = new ArrayList<Block>();
		for(int i = -1; i <= 1; i++) {
			for(int j = -1; j <= 1; j++) {
				if(i == 0 && j == 0) {
					continue;
				}					
				try {
					Block neighbor = blocks.get(b.getGridX()+i).get(b.getGridY()+j);
					neighbors.add(neighbor);
				}
				catch(IndexOutOfBoundsException e) {}
			}
		}
		return neighbors;
	}
	
	private ArrayList<Block> getOrthogonalNeighbors(Block b) {
		ArrayList<Block> neighbors = new ArrayList<Block>();
		Point[] directions = {new Point(-1, 0), new Point(1, 0), new Point(0, 1), new Point(0, -1)};
		for(Point dir : directions) {								
			try {
				Block neighbor = blocks.get(b.getGridX()+dir.x).get(b.getGridY()+dir.y);
				neighbors.add(neighbor);
			}
			catch(IndexOutOfBoundsException e) {}
		}
		return neighbors;
	}
	
	private ArrayList<Block> getDiagonalNeighbors(Block b) {
		ArrayList<Block> neighbors = new ArrayList<Block>();
		Point[] directions = {new Point(-1, -1), new Point(-1, 1), new Point(1, -1), new Point(1, 1)};
		for(Point dir : directions) {								
			try {
				Block neighbor = blocks.get(b.getGridX()+dir.x).get(b.getGridY()+dir.y);
				neighbors.add(neighbor);
			}
			catch(IndexOutOfBoundsException e) {}
		}
		return neighbors;
	}

	private ArrayList<Block> getBlocks() {
		ArrayList<Block> aBlocks = new ArrayList<Block>();
		for(ArrayList<Block> col : blocks) {
			for(Block block : col) {
				aBlocks.add(block);
			}
		}
		return aBlocks;
	}

	private void solveStep() throws BadBoardException {
		if(status == GameStatus.Ongoing) {
			ArrayList<Block> frontier = getFrontier();
			String dist = new String(new char[frontier.size()]).replace("\0", "?");
			for(int i = 0; i < frontier.size(); i++) {
				if(frontier.get(i).getStatus() == Block.Status.Flagged) {
					dist = dist.substring(0, i) + '1' + dist.substring(i+1);
				}
			}

		}
	}

	public class BadBoardException extends Exception {
		public BadBoardException(String message) {
			super(message);
		}
	}

	private ArrayList<Block> getFrontier() {
		ArrayList<Block> frontier = new ArrayList<Block>();
		//frontier is all unrevealed blocks that have >= 1 revealed neighbor
		for(Block block : getBlocks()) {
			if(!(block.getStatus() == Block.Status.Revealed)) {
				for(Block neighbor : getNeighbors(block)) {
					if(neighbor.getStatus() == Block.Status.Revealed) {
						if(!frontier.contains(block)) {
							frontier.add(block);
						}
						break;
					}
				}
			}
		}
		return frontier;
	}
	
	private void quickSolveAndDisplayOutput() {
		ArrayList<Block> frontier = getFrontier();
		if(frontier.isEmpty()) {
			
		}
		ArrayList<Point> aSafePoints = null;
		try {
			aSafePoints = quickSolve();
		}
		catch(BadBoardException bbe) {
			JOptionPane.showMessageDialog(this, bbe.getMessage());
			return;
		}
		ArrayList<Point> safePoints = aSafePoints;
		
		JFrame quickSolveFrame = new JFrame("Quick solve");
		JPanel panel = new JPanel();
		quickSolveFrame.add(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JPanel safePointsLabelPanel = new JPanel();
		safePointsLabelPanel.setLayout(flow);
		panel.add(safePointsLabelPanel);
		JLabel safePointsLabel = new JLabel(pointsToString(safePoints));
		safePointsLabelPanel.add(safePointsLabel);
		
		JPanel showDistPanel = new JPanel();
		showDistPanel.setLayout(flow);
		panel.add(showDistPanel);
		JCheckBox showDist = new JCheckBox("Show safe points");
		showDistPanel.add(showDist);
		for(Point p : safePoints) {
			Block b = blocks.get(p.x).get(p.y);
			b.setStatus(Block.Status.HighlightGreen);
		}
		showDist.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(showDist.isSelected()) {
					for(int i = 0; i < frontier.size(); i++) {
						for(Point p : safePoints) {
							Block b = blocks.get(p.x).get(p.y);
							b.setStatus(Block.Status.HighlightGreen);
						}
					}
				}
				else {
					for(int i = 0; i < frontier.size(); i++) {
						Block b = frontier.get(i);
						if(b.getStatus() == Block.Status.HighlightGreen || b.getStatus() == Block.Status.HighlightRed) {
							b.setStatus(Block.Status.Unmarked);
						}
					}
				}
			}
		});
		showDist.setSelected(true);
		JButton quickSolveNext = new JButton("Solve");
		panel.add(quickSolveNext);
		quickSolveNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for(Point p : safePoints) {
					Block b = blocks.get(p.x).get(p.y);
					reveal(b);
				}
				quickSolveFrame.dispatchEvent(new WindowEvent(quickSolveFrame, WindowEvent.WINDOW_CLOSING));
				quickSolveAndDisplayOutput();
			}
		});
		quickSolveFrame.setPreferredSize(new Dimension(500, 300));
		quickSolveFrame.setLocation(getLocation().x + getSize().width, getLocation().y);
		quickSolveFrame.pack();
		quickSolveFrame.setVisible(true);
	}
	
	private ArrayList<Point> quickSolve() throws BadBoardException {
		String startingDist = "";
		ArrayList<Block> frontier = getFrontier();
		if(frontier.size() == 0) {
			throw new BadBoardException("Frontier too small: " + frontier.size());
		}
		for(int i = 0; i < frontier.size(); i++) {
			Block b = frontier.get(i);
			if(b.getStatus() == Block.Status.Flagged) {
				startingDist += '1';
			}
			else {
				startingDist += '?';
			}
		}
		String tempDist = null;
		ArrayList<Zone> allZones = null;
		boolean change = true;
		while(change) {
			ArrayList<Zone> anAllZones = new ArrayList<Zone>();
			String newTempDist = solveZone(null, null, tempDist, null, anAllZones);
			change = !newTempDist.equals(tempDist);
			tempDist = newTempDist;
			allZones = anAllZones;
		}
		if(tempDist.equals(startingDist)) {
			//if we're stuck
			tempDist = compareAllZones(allZones, frontier, tempDist);
		}
		
		//endgame conditions
		//special case: if all mines are implied to be on the frontier, any blocks not on frontier are safe
		int implied = minImpliedFrontierMines(frontier, tempDist);
		int numKnownMines = tempDist.length() - tempDist.replace("1", "").length();
		ArrayList<Point> safePoints = new ArrayList<Point>();
		if(numKnownMines + implied == this.minesNeeded) {
			for(Block b : getBlocks()) {
				if(!(b.getStatus() == Block.Status.Revealed) && !frontier.contains(b)) {
					safePoints.add(new Point(b.getGridX(), b.getGridY()));
				}
			}
		}
		
		for(int i = 0; i < frontier.size(); i++) {
			Block b = frontier.get(i);
			if(tempDist.charAt(i) == '1') {
				setFlagged(b, true);
			}
			else if(tempDist.charAt(i) == '0') {
				safePoints.add(new Point(b.getGridX(), b.getGridY()));
			}
		}
		return safePoints;
	}
	
	private String solveZone(ArrayList<Block> frontier, ArrayList<Block> revealedBorderNotConsideredYet, String dist, Zone rememberedZone, ArrayList<Zone> allZones) {
		if(frontier == null) {
			frontier = getFrontier();
		}
		if(dist == null) {
			dist = "";
			for(Block b : frontier) {
				if(b.getStatus() == Block.Status.Flagged) {
					dist += "1";
				}
				else {
					dist += "?";
				}
			}
		}
		if(revealedBorderNotConsideredYet == null) {
			revealedBorderNotConsideredYet = revealedBorder(frontier, true);
		}
		if(revealedBorderNotConsideredYet.isEmpty()) {
			return dist;
		}
		//compare each revealed border block's zone with that of the next one, or with a remembered zone
		Zone currentZone = null;
		Zone nextZone = null;
		if(rememberedZone != null) {
			currentZone = rememberedZone;
			Block nextBlock = revealedBorderNotConsideredYet.get(0);
			nextZone = makeZone(nextBlock, frontier, dist);
		}
		else {
			Block nextBlock = revealedBorderNotConsideredYet.remove(0);
			currentZone = makeZone(nextBlock, frontier, dist);
			if(revealedBorderNotConsideredYet.isEmpty()) {
				if(currentZone != null) {
					dist = processZone(currentZone, frontier, dist);
				}
				return dist;
			}
			Block nextNextBlock = revealedBorderNotConsideredYet.get(0);
			nextZone = makeZone(nextNextBlock, frontier, dist);
		}
		Zone toRemember = null;
		if(currentZone != null) {
			//process currentZone
			dist = processZone(currentZone, frontier, dist);
			//consider currentZone together with next zone
			
			if(nextZone != null) {
				//if one zone is a strict subset of the other
				Zone diff1 = subtractZone(currentZone, nextZone);
				if(diff1 != null) {
					dist = processZone(diff1, frontier, dist);
				}
				Zone diff2 = subtractZone(nextZone, currentZone);
				if(diff2 != null) {
					dist = processZone(diff2, frontier, dist);
				}
				
				//if they overlap but neither is a strict subset
				Zone intersectionZone = intersectionZone(currentZone, nextZone);
				if(intersectionZone != null && intersectionZone.getMines() != -1) {
					Zone currentMinusIntersection = subtractZone(currentZone, intersectionZone);
					dist = processZone(currentMinusIntersection, frontier, dist);
					Zone nextMinusIntersection = subtractZone(nextZone, intersectionZone);
					dist = processZone(nextMinusIntersection, frontier, dist);
					if(nextMinusIntersection.getBlocks().size() >= 2 && rememberedZone == null) {
						System.out.println("keeping zone : " + nextMinusIntersection);
						toRemember = nextMinusIntersection;
					}
				}
			}
		}
		if(currentZone != null && !allZones.contains(currentZone)) {
			allZones.add(currentZone);
		}
		if(nextZone != null && !allZones.contains(nextZone)) {
			allZones.add(nextZone);
		}
		if(toRemember != null && !allZones.contains(toRemember)) {
			allZones.add(toRemember);
		}
		return solveZone(frontier, revealedBorderNotConsideredYet, dist, toRemember, allZones);
	}
	
	private String processZone(Zone zone, ArrayList<Block> frontier, String dist) {
		int size = zone.getBlocks().size();
		if(zone.getMines() == size) {
			//all mines
			for(Block b : zone.getBlocks()) {
				int index = frontier.indexOf(b);
				dist = dist.substring(0, index) + '1' + dist.substring(index+1);
			}
		}
		else if(zone.getMines() == 0) {
			//all safe
			for(Block b : zone.getBlocks()) {
				int index = frontier.indexOf(b);
				dist = dist.substring(0, index) + '0' + dist.substring(index+1);
			}
		}
		return dist;
	}
	
	private String compareAllZones(ArrayList<Zone> zone, ArrayList<Block> frontier, String dist) {
		for(Zone zone1 : zone) {
			for(Zone zone2 : zone) {
				if(!intersection(zone1.getBlocks(), zone2.getBlocks()).isEmpty()) {
					//if one zone is a strict subset of the other
					Zone diff1 = subtractZone(zone1, zone2);
					if(diff1 != null) {
						dist = processZone(diff1, frontier, dist);
					}
					Zone diff2 = subtractZone(zone2, zone1);
					if(diff2 != null) {
						dist = processZone(diff2, frontier, dist);
					}
					
					//if they overlap but neither is a strict subset
					Zone intersectionZone = intersectionZone(zone1, zone2);
					if(intersectionZone != null && intersectionZone.getMines() != -1) {
						Zone currentMinusIntersection = subtractZone(zone1, intersectionZone);
						dist = processZone(currentMinusIntersection, frontier, dist);
						Zone nextMinusIntersection = subtractZone(zone2, intersectionZone);
						dist = processZone(nextMinusIntersection, frontier, dist);
					}
				}
			}
		}
		return dist;
	}

	private ArrayList<Point> bigBrain(boolean solveCompletelyAndShowOutput, boolean attemptComplexBoard, boolean bugsAllowed) throws BadBoardException, BugException {
		//either solve completely (finding ALL safe points and all possible mine location solutions) and display output
		//or find any number of safe points or mines and return as soon as we learn anything
		//returns list of safe points, flags known mines

		ArrayList<Block> frontier = getFrontier();

		String frontList = "";
		for(Block b : frontier) {
			frontList += "(" + b.getGridX() + ", " + b.getGridY() + "), ";
		}
		if(frontier.size() == 0) {
			throw new BadBoardException("Frontier too small: " + frontier.size());
		}

		String dist = new String(new char[frontier.size()]).replace("\0", "?");
		for(int i = 0; i < frontier.size(); i++) {
			if(frontier.get(i).getStatus() == Block.Status.Flagged) {
				dist = dist.substring(0, i) + '1' + dist.substring(i+1);
			}
		}
		String startingDist = new String(dist);

		//apply 1st and 2nd order logic
		dist = fullyExtrapolateDistribution(frontier, dist, !solveCompletelyAndShowOutput);

		//if not solving completely, return as soon as we learn anything
		if(!dist.equals(startingDist) && !solveCompletelyAndShowOutput) {
			for(int i = 0; i < frontier.size(); i++) {
				if(dist.charAt(i) == '1') {
					setFlagged(frontier.get(i), true);
				}
			}
			return listSafePoints(dist, frontier);
		}

		//place imaginary mines or safes, extrapolate, see if that causes a contradiction
		for(int i = 0; i < frontier.size(); i++) {
			if(dist.charAt(i) == '?') {
				String distMineAdded = dist.substring(0, i) + '1' + dist.substring(i+1);;
				distMineAdded = fullyExtrapolateDistribution(frontier, distMineAdded, false);
				if(!isLegal(frontier, distMineAdded)) {
					//then that square cannot be a mine
					dist = dist.substring(0, i) + '0' + dist.substring(i+1);;
				}
				else {
					String distSafeAdded = dist.substring(0, i) + '0' + dist.substring(i+1);;
					distSafeAdded = fullyExtrapolateDistribution(frontier, distSafeAdded, false);
					if(!isLegal(frontier, distSafeAdded)) {
						//then that square cannot be a safe
						dist = dist.substring(0, i) + '1' + dist.substring(i+1);;
					}
				}
			}
		}

		if(!dist.equals(startingDist) && !solveCompletelyAndShowOutput) {
			for(int i = 0; i < frontier.size(); i++) {
				if(dist.charAt(i) == '1') {
					setFlagged(frontier.get(i), true);
				}
			}
			return listSafePoints(dist, frontier);
		}

		ArrayList<Point> safePoints = new ArrayList<Point>();
		ArrayList<Point> knownMines = new ArrayList<Point>();
		ArrayList<Point> newKnownMines = new ArrayList<Point>();

		for(int i = 0; i < frontier.size(); i++) {
			Block b = frontier.get(i);
			Point p = new Point(b.getGridX(), b.getGridY());
			if(dist.charAt(i) == '0') {
				safePoints.add(p);
			}
			else if(dist.charAt(i) == '1') {
				knownMines.add(new Point(b.getGridX(), b.getGridY()));
				if(!(startingDist.charAt(i) == '1')) {
					newKnownMines.add(p);
				}
			}
		}
		

		//special case: if all mines are implied to be on the frontier, any blocks not on frontier are safe
		int implied = minImpliedFrontierMines(frontier, dist);
		int numKnownMines = dist.length() - dist.replace("1", "").length();
		if(numKnownMines + implied == this.minesNeeded) {
			for(Block b : getBlocks()) {
				if(!(b.getStatus() == Block.Status.Revealed) && !frontier.contains(b)) {
					safePoints.add(new Point(b.getGridX(), b.getGridY()));
				}
			}
			if(!safePoints.isEmpty() && !solveCompletelyAndShowOutput) {
				for(int i = 0; i < frontier.size(); i++) {
					if(dist.charAt(i) == '1') {
						setFlagged(frontier.get(i), true);
					}
				}
				return safePoints;
			}
		}

		//only use brute force if we haven't learned anything by other methods (or if asked to solve completely)
		
		final String distBeforeBF = new String(dist);
		final int numUnknownsBeforeBF = dist.length() - dist.replace("?", "").length();

		if(!distBeforeBF.equals(startingDist) && !solveCompletelyAndShowOutput) {
			for(int i = 0; i < frontier.size(); i++) {
				if(dist.charAt(i) == '1') {
					setFlagged(frontier.get(i), true);
				}
			}
			return listSafePoints(dist, frontier);
		}

		//break the frontier into independent sections (whose solutions don't affect each other)
		//brute force those individually
		ArrayList<ArrayList<Block>> independentFrontierSections = makeIndependentFrontierSections(frontier, distBeforeBF);
		ArrayList<ArrayList<ArrayList<Point>>> allSectionSolutions = new ArrayList<ArrayList<ArrayList<Point>>>();

		String s = "";
		boolean tooLarge = false;
		//do not use brute force if too many unknowns (unless special permission)
		for(ArrayList<Block> section : independentFrontierSections) {
			s += section.size() + ", ";
			if(section.size() > maxRecommendedUnknowns && !attemptComplexBoard) {
				tooLarge = true;
			}
		}
		String sectionSizes = s.trim().replaceAll(",+$", "");
		if(tooLarge) {
			//System.out.println("Nothing learned without brute force, unknowns " + numUnknownsBeforeBF);
			throw new BadBoardException("Unknowns sections too large: " + sectionSizes + 
					"\nTotal unknowns: " + numUnknownsBeforeBF + 
					"\nFrontier size: " + frontier.size());
		}

		//proceed to brute force search
		//System.out.println("brute forcing. unknowns " + numUnknownsBeforeBF);
		JFrame progressFrame = new JFrame("Waiting");
		progressFrame.setLocation(getLocation().x, getLocation().y);
		JPanel progressPanel = new JPanel();
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
		
		JPanel textPanel = new JPanel();
		textPanel.setLayout(flow);
		progressPanel.add(textPanel);
		JLabel progressText = new JLabel("Patience is a virtue");
		textPanel.add(progressText);
		
		progressFrame.add(progressPanel);
		JPanel barPanel = new JPanel();
		barPanel.setLayout(flow);
		progressPanel.add(barPanel);
		JProgressBar progressBar = new JProgressBar();
		barPanel.add(progressBar);
		
		JPanel abortPanel = new JPanel();
		abortPanel.setLayout(flow);
		progressPanel.add(abortPanel);
		JButton abortButton = new JButton("Abort");
		abortPanel.add(abortButton);
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abortSafePointSearch = true;
				userClickedAbortSafePointSearch = true;
			}
		});
		abortSafePointSearch = false;
		progressFrame.addWindowListener(new WindowListener() {
			public void windowOpened(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
			public void windowClosing(WindowEvent e) {
				abortSafePointSearch = true;
			}
			public void windowClosed(WindowEvent e) {}
			public void windowActivated(WindowEvent e) {}
		});
		progressFrame.pack();
		progressFrame.setVisible(true);
		Main main = this;
		ArrayList<ArrayList<Point>> solutions = new ArrayList<ArrayList<Point>>();
		ArrayList<String> bugs = new ArrayList<String>();

		Thread safePointThread = new Thread(new Runnable() {
			public void run() {

				/**
				ArrayList<Block> unknownBlocks = new ArrayList<Block>();
				for(int i = 0; i < frontier.size(); i++) {
					if(distBeforeBF.charAt(i) == '?') {
						unknownBlocks.add(frontier.get(i));
					}
				}

				//span the unknown blocks with zones, which have known numbers of mines and thus limited possible distributions
				ArrayList<Zone> spanningZones = listZonesSpanningSet(unknownBlocks, makeZones(frontier, distBeforeBF));
				ArrayList<ArrayList<String>> zoneDists = new ArrayList<ArrayList<String>>();
				ArrayList<Integer> zoneDistLengths = new ArrayList<Integer>();
				for(Zone zone : spanningZones) {
					ArrayList<String> zoneDist = getPossibleZoneDistributions(zone);
					zoneDists.add(zoneDist);
					zoneDistLengths.add(zoneDist.size());
				}

				//each String is a list of indexes indicating which distribution to use for the zones
				ArrayList<String> indexLists = null;
				try {
					indexLists = combinePossibleIndexes(zoneDistLengths);
				}
				catch(AbortException ae) {
					progressFrame.dispatchEvent(new WindowEvent(progressFrame, WindowEvent.WINDOW_CLOSING));
					return;
				}
				 **/

				ArrayList<Integer> sectionSolutionQuantities = new ArrayList<Integer>();

				int minMinesFoundTotalAllSections = 0;
				for(int i = 0; i < independentFrontierSections.size(); i++) {
					ArrayList<Block> section = independentFrontierSections.get(i);

					String thisSectionPreDist = segmentDist(frontier, section, distBeforeBF);
					int sectionPreUnkowns = thisSectionPreDist.length() - thisSectionPreDist.replace("?", "").length();
					progressText.setText("section " + (i+1) + "/" + (independentFrontierSections.size()) + ". unknowns " + sectionPreUnkowns);
					
					if(abortSafePointSearch) {
						progressFrame.dispatchEvent(new WindowEvent(progressFrame, WindowEvent.WINDOW_CLOSING));
						return;
					}

					
					ArrayList<ArrayList<Point>> thisSectionSolutions = null;
					try {
						thisSectionSolutions = bruteForceSection(section, frontier, sectionPreUnkowns, distBeforeBF, progressBar);
					}
					catch(AbortException ae) {
						progressFrame.dispatchEvent(new WindowEvent(progressFrame, WindowEvent.WINDOW_CLOSING));
						return;
					}
					allSectionSolutions.add(thisSectionSolutions);
					sectionSolutionQuantities.add(thisSectionSolutions.size());

					int minMinesFoundThisSection = 0;
					if(!thisSectionSolutions.isEmpty()) {
						minMinesFoundThisSection = thisSectionSolutions.get(0).size();
					}
					for(ArrayList<Point> solution : thisSectionSolutions) {
						if(solution.size() < minMinesFoundThisSection) {
							minMinesFoundThisSection = solution.size();
						}
					}
					minMinesFoundTotalAllSections += minMinesFoundThisSection;

					if(thisSectionSolutions.isEmpty()) {
						bugs.add("no solutions");
						if(!solveCompletelyAndShowOutput) {
							progressFrame.dispatchEvent(new WindowEvent(progressFrame, WindowEvent.WINDOW_CLOSING));
							return;
						}
					}
				}
				ArrayList<String> sectionIndexCombinations = null;
				try {
					sectionIndexCombinations = combinePossibleIndexes(sectionSolutionQuantities);
				}
				catch(AbortException ae) {
					bugs.add("frontier section index combination aborted");
					if(!solveCompletelyAndShowOutput) {
						progressFrame.dispatchEvent(new WindowEvent(progressFrame, WindowEvent.WINDOW_CLOSING));
						return;
					}
				}
				int minesBeforeBF = distBeforeBF.length() - distBeforeBF.replace("1", "").length();
				//stitch together solutions from all possible combinations of solutions to subsections
				for(String combo : sectionIndexCombinations) {
					String[] indexStrings = combo.split(",");
					ArrayList<Point> solution = new ArrayList<Point>();
					for(int i = 0; i < indexStrings.length; i++) {
						String indexString = indexStrings[i];
						Integer index = null;
						try {
							index = Integer.parseInt(indexString);
						}
						catch(NumberFormatException nfe) {}
						solution.addAll(allSectionSolutions.get(i).get(index));
					}

					//exclude solutions with too many mines
					if(solution.size() + minesBeforeBF > minesNeeded) {
						//System.out.println("solution excluded: " + solution.size() + "/" + minesNeeded + "  " + solution);
						continue;
					}
					int numUnmarkedBlocksNotOnFrontier = 0;
					for(Block b : getBlocks()) {
						if(!(b.getStatus() == Block.Status.Revealed) && !frontier.contains(b)) {
							numUnmarkedBlocksNotOnFrontier++;
						}
					}
					if(solution.size() + minesBeforeBF + numUnmarkedBlocksNotOnFrontier < minesNeeded) {
						//System.out.println("solution excluded: " + solution.size() + "+" + minesBeforeBF + "+" + numUnmarkedBlocksNotOnFrontier + "<" + minesNeeded);
						//if there is not enough empty space left beyond the frontier to put the needed number of additional mines
						continue;
					}

					solutions.add(solution);
				}
				//a safe point is a (previously unknown) frontier block that does not contain a mine in any possible solution
				ArrayList<Point> safePointsFoundByBF = new ArrayList<Point>();
				for(int i = 0; i < frontier.size(); i++) {
					Block block = frontier.get(i);
					if(distBeforeBF.charAt(i) == '?') {
						Point p = new Point(block.getGridX(), block.getGridY());
						safePointsFoundByBF.add(p);
					}
				}
				Iterator<Point> it = safePointsFoundByBF.iterator();
				while(it.hasNext()) {
					Point p = it.next();
					for(ArrayList<Point> solution : solutions) {
						if(solution.contains(p)) {
							it.remove();
							break;
						}
					}
				}
				for(Point p : safePointsFoundByBF) {
					if(!safePoints.contains(p)) {
						safePoints.add(p);
					}
				}

				ArrayList<Point> minesFoundByBF = new ArrayList<Point>();
				//a new known mine is a mine in every possible solution
				for(int i = 0; i < frontier.size(); i++) {
					Block block = frontier.get(i);
					if(distBeforeBF.charAt(i) == '?') {
						Point p = new Point(block.getGridX(), block.getGridY());
						minesFoundByBF.add(p);
					}
				}
				Iterator<Point> it2 = minesFoundByBF.iterator();
				while(it2.hasNext()) {
					Point p = it2.next();
					for(ArrayList<Point> solution : solutions) {
						if(!solution.contains(p)) {
							it2.remove();
							break;
						}
					}
				}
				for(Point p : minesFoundByBF) {
					if(!newKnownMines.contains(p)) {
						newKnownMines.add(p);
					}
					if(!knownMines.contains(p)) {
						knownMines.add(p);
					}
				}


				int numKnownMinesBeforeBF = distBeforeBF.length() - distBeforeBF.replace("1", "").length();
				boolean allSolutionsShowAllMines = minMinesFoundTotalAllSections + numKnownMinesBeforeBF == minesNeeded;

				//special case: if all solutions indicate the full number of mines on the frontier, any blocks not on frontier are safe
				if(allSolutionsShowAllMines) {
					for(Block b : getBlocks()) {
						if(!(b.getStatus() == Block.Status.Revealed) && !frontier.contains(b)) {
							safePoints.add(new Point(b.getGridX(), b.getGridY()));
						}
					}
				}


				String distAfterBF = new String(distBeforeBF);
				for(int i = 0; i < frontier.size(); i++) {
					Block block = frontier.get(i);
					if(distAfterBF.charAt(i) == '?') {
						if(safePoints.contains(new Point(block.getGridX(), block.getGridY()))) {
							distAfterBF = distAfterBF.substring(0, i) + '0' + distAfterBF.substring(i+1);;
						}
						else if(knownMines.contains(new Point(block.getGridX(), block.getGridY()))) {
							distAfterBF = distAfterBF.substring(0, i) + '1' + distAfterBF.substring(i+1);;
						}
					}
				}

				int numUnknownsAfterBF = distAfterBF.length() - distAfterBF.replace("?", "").length();
				int numUnknownsRevealedByBF = (numUnknownsBeforeBF - numUnknownsAfterBF);
				if(numUnknownsRevealedByBF != 0 && !solveCompletelyAndShowOutput) {
					progressFrame.dispatchEvent(new WindowEvent(progressFrame, WindowEvent.WINDOW_CLOSING));
					return;
				}

				for(Point p : knownMines) {
					Block block = blocks.get(p.x).get(p.y);
					block.setStatus(Block.Status.Flagged);
				}
				for(Block block : frontier) {
					//block.setFlagged(false);
				}

				if(solveCompletelyAndShowOutput) {
					//show results in a popup window
					String solutionsString = "";
					for(int i = 0; i < solutions.size(); i++) {
						ArrayList<Point> solution = solutions.get(i);
						solutionsString += (i+1) + ". " + solution.size() + " mines: " + pointsToString(solution) + "\n";
					}
					String safePointsString = pointsToString(safePoints);
					String knownMinesString = pointsToString(knownMines);

					String output = "Unkowns identified by brute force: " + numUnknownsRevealedByBF + "\n" + 
							"\nFrontier size: " + frontier.size() +
							"\n" + distBeforeBF +
							"\nUnknown sections: " + sectionSizes + 
							"\n" + numUnknownsBeforeBF + " total unknowns " + 
							"\nDistributions tested: " + (int) Math.pow(2, numUnknownsBeforeBF) +
							"\n" +
							"\nSolutions: " + solutions.size() +
							"\n" + solutionsString +
							"\nKnown Mines: " + knownMines.size() +
							"\n" + knownMinesString + "\n" +
							"\nSafe Points: " + safePoints.size() +
							"\n" + safePointsString;


					JFrame outFrame = new JFrame("Output");
					outFrame.setLocation(main.getLocation().x + main.getSize().width, main.getLocation().y);
					JPanel outPanel = new JPanel();
					outPanel.setLayout(new BoxLayout(outPanel, BoxLayout.Y_AXIS));
					outFrame.add(outPanel);
					JTextArea outArea = new JTextArea(output);
					outArea.setEditable(false);
					JScrollPane textScroll = new JScrollPane(outArea);
					textScroll.setPreferredSize(new Dimension(300, 300));
					outPanel.add(textScroll);

					JPanel outBottom = new JPanel();
					outBottom.setLayout(flow);
					outPanel.add(outBottom);
					JPanel buttonPanel = new JPanel();
					buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
					JScrollPane buttonScroll = new JScrollPane(buttonPanel);
					buttonScroll.setPreferredSize(new Dimension(120, 200));
					outBottom.add(buttonScroll);
					ButtonGroup group = new ButtonGroup();
					for(int i = 0; i < solutions.size(); i++) {
						ArrayList<Point> solution = solutions.get(i);
						JRadioButton radio = new JRadioButton("Solution " + (i+1));
						radio.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								for(int j = 0; j < frontier.size(); j++) {
									Block b = frontier.get(j);
									Point p = new Point(b.getGridX(), b.getGridY());
									if(!knownMines.contains(p) && !safePoints.contains(p)) {
										if(!(b.getStatus() == Block.Status.Flagged)) {
											b.setStatus(Block.Status.Unmarked);
										}
									}
								}
								for(Point p : solution) {
									Block b = blocks.get(p.x).get(p.y);
									if(!(b.getStatus() == Block.Status.Flagged)) {
										b.setStatus(Block.Status.HighlightRed);
									}
								}
							}
						});
						group.add(radio);
						buttonPanel.add(radio);
						if(i == 0) {
							radio.setSelected(true);
						}
					}
					if(!solutions.isEmpty()) {
						//display 1st solution
						for(Point p : solutions.get(0)) {
							Block b = blocks.get(p.x).get(p.y);
							if(!(b.getStatus() == Block.Status.Flagged)) {
								b.setStatus(Block.Status.HighlightRed);
							}
						}
					}

					JPanel checkPanel = new JPanel();
					checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
					outBottom.add(checkPanel);
					JCheckBox flagMines = new JCheckBox("Flag known mines");
					flagMines.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							for(Point p : newKnownMines) {
								Block b = blocks.get(p.x).get(p.y);
								if(!(b.getStatus() == Block.Status.Revealed)) {
									if(flagMines.isSelected()) {
										setFlagged(b, true);
									}
									else {
										setFlagged(b, false);
										b.setStatus(Block.Status.HighlightRed);
									}
								}
							}
						}
					});
					flagMines.setSelected(true);
					checkPanel.add(flagMines);
					JCheckBox showSafePoints = new JCheckBox("Show safe points");
					for(Point p : safePoints) {
						Block b = blocks.get(p.x).get(p.y);
						if(!(b.getStatus() == Block.Status.Revealed)) {
							b.setStatus(Block.Status.HighlightGreen);
						}
					}
					showSafePoints.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							System.out.println("safe points: " + pointsToString(safePoints));
							for(Point p : safePoints) {
								Block b = blocks.get(p.x).get(p.y);
								if(!(b.getStatus() == Block.Status.Revealed)) {
									if(showSafePoints.isSelected()) {
										b.setStatus(Block.Status.HighlightGreen);
									}
									else {
										b.setStatus(Block.Status.Unmarked);
									}
								}
							}
						}
					});
					showSafePoints.setSelected(true);
					checkPanel.add(showSafePoints);

					JButton iterate = new JButton("Iterate");
					outBottom.add(iterate);
					iterate.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							for(Point p : safePoints) {
								reveal(blocks.get(p.x).get(p.y));
							}
							outFrame.dispatchEvent(new WindowEvent(outFrame, WindowEvent.WINDOW_CLOSING));
							if(status == GameStatus.Ongoing) {
								try {
									bigBrain(true, false, true);
								}
								catch(BadBoardException ex) {
									JOptionPane.showMessageDialog(main, ex.getMessage());
								}
								catch(BugException ex) {
									JOptionPane.showMessageDialog(main, ex.getMessage());
								}
							}
						}
					});

					outFrame.pack();
					outFrame.setVisible(true);
					newGame.setEnabled(false);
					bigBrainButton.setEnabled(false);
					quickSolve.setEnabled(false);
					outFrame.addWindowListener(new WindowListener() {
						public void windowOpened(WindowEvent e) {}
						public void windowIconified(WindowEvent e) {}
						public void windowDeiconified(WindowEvent e) {}
						public void windowDeactivated(WindowEvent e) {}
						public void windowClosing(WindowEvent e) {
							for(Block block : getBlocks()) {
								if(block.getStatus() == Block.Status.HighlightGreen || block.getStatus() == Block.Status.HighlightRed) {
									setFlagged(block, false);
								}
							}
							newGame.setEnabled(true);
							if(status == GameStatus.Ongoing) {
								bigBrainButton.setEnabled(true);
								quickSolve.setEnabled(true);
							}
						}
						public void windowClosed(WindowEvent e) {}
						public void windowActivated(WindowEvent e) {}
					});
				}
				progressFrame.dispatchEvent(new WindowEvent(progressFrame, WindowEvent.WINDOW_CLOSING));
			}
		});
		//System.out.println("Current thread: " + Thread.currentThread().getName() + ".\t\t\tstarting safePointThread: " + safePointThread.getName());
		safePointThread.setPriority(Thread.MIN_PRIORITY);
		safePointThread.start();
		
		if(!solveCompletelyAndShowOutput) {
			//if not showing output, we are returning output, and must wait for brute force algorithm to finish before returning
			try {
				safePointThread.join();
			}
			catch(InterruptedException ie) {}

			if(!bugs.isEmpty()) {
				String bugString = "";
				for(String bug : bugs) {
					bugString += bug + "\n";
				}
				if(bugsAllowed) {
					throw new BugException(bugString);
				}
			}

			String distAfterBF = new String(distBeforeBF);
			for(int i = 0; i < frontier.size(); i++) {
				Block block = frontier.get(i);
				if(distAfterBF.charAt(i) == '?') {
					if(safePoints.contains(new Point(block.getGridX(), block.getGridY()))) {
						distAfterBF = distAfterBF.substring(0, i) + '0' + distAfterBF.substring(i+1);;
					}
					else if(knownMines.contains(new Point(block.getGridX(), block.getGridY()))) {
						distAfterBF = distAfterBF.substring(0, i) + '1' + distAfterBF.substring(i+1);;
					}
				}
			}

			int numUnknownsAfterBF = distAfterBF.length() - distAfterBF.replace("?", "").length();
			//System.out.println("unknowns after BF: " + numUnknownsAfterBF);
			int numUnknownsRevealedByBF = (numUnknownsBeforeBF - numUnknownsAfterBF);
			if(numUnknownsRevealedByBF != 0) {
				if(bugsAllowed) {
					throw new BugException("Unknowns revealed by BF: " + numUnknownsRevealedByBF + " (" + numUnknownsBeforeBF + " to " + numUnknownsAfterBF + ")");
				}
			}
			int numSafeBeforeBF = distBeforeBF.length() - distBeforeBF.replace("0", "").length();
			int diff = safePoints.size()-numSafeBeforeBF;
			if(diff > 0 && numSafeBeforeBF == 0) {
				if(bugsAllowed) {
					throw new BugException("safe points " + numSafeBeforeBF + " -> " + safePoints.size() + " (diff " + diff + ") with brute force");
				}
			}
			for(int i = 0; i < frontier.size(); i++) {
				if(distAfterBF.charAt(i) == '1') {
					setFlagged(frontier.get(i), true);
				}
			}
			return safePoints;
		}
		return null;
	}

	public class SafeFoundException extends Exception {
		private String dist;
		public SafeFoundException(String dist) {
			this.dist = dist;
		}

		public String getDist() {
			return dist;
		}
	}
	
	private String segmentDist(ArrayList<Block> frontier, ArrayList<Block> section, String dist) {
		String segmentDist = "";
		for(Block block : section) {
			segmentDist += dist.charAt(frontier.indexOf(block));
		}
		return segmentDist;
	}
	
	private ArrayList<ArrayList<Block>> makeIndependentFrontierSections(ArrayList<Block> frontier, String dist) {
		//an independent section is a (maximally large) set of unknownBlocks
		//who all shared revealedBorder neighbor block with another unknownBlock in the section
		ArrayList<ArrayList<Block>> independentFrontierSections = new ArrayList<ArrayList<Block>>();
		ArrayList<Block> unknownBlocks = new ArrayList<Block>();
		for(int i = 0; i < frontier.size(); i++) {
			if(dist.charAt(i) == '?') {
				unknownBlocks.add(frontier.get(i));
			}
		}
		ArrayList<Block> revealedBorder = revealedBorder(frontier, false);
		for(Block block : unknownBlocks) {
			boolean inExistingSection = false;
			for(ArrayList<Block> section : independentFrontierSections) {
				if(section.contains(block)) {
					inExistingSection = true;
				}
			}
			if(!inExistingSection) {
				ArrayList<Block> section = new ArrayList<Block>();
				section.add(block);
				boolean change = true;
				while(change) {
					change = expandIndependentSection(section, unknownBlocks, revealedBorder);
				}
				independentFrontierSections.add(section);
			}
		}
		return independentFrontierSections;
	}
	
	private boolean expandIndependentSection(ArrayList<Block> section, ArrayList<Block> unknownBlocks, ArrayList<Block> revealedBorder) {
		Iterator<Block> sectionIt = section.iterator();
		ArrayList<Block> newElements = new ArrayList<Block>();
		while(sectionIt.hasNext()) {
			Block sectionBlock = sectionIt.next();
			ArrayList<Block> revealedBorderNeighbors = adjBlocks(sectionBlock, revealedBorder);
			Iterator<Block> rbnIt = revealedBorderNeighbors.iterator();
			while(rbnIt.hasNext()) {
				Block revealedBorderNeighbor = rbnIt.next();
				ArrayList<Block> adjUnknowns = adjBlocks(revealedBorderNeighbor, unknownBlocks);
				for(Block adjUnk : adjUnknowns) {
					if(!section.contains(adjUnk) && !newElements.contains(adjUnk)) {
						newElements.add(adjUnk);
					}
				}
				//only need to add the adjBlocks of a given revealedBorder block once
				rbnIt.remove();
			}
		}
		section.addAll(newElements);
		return newElements.size() > 0;
	}

	private ArrayList<ArrayList<Point>> bruteForceSection(ArrayList<Block> section, ArrayList<Block> frontier, int thisSectionPreUnkowns, String frontierDist,
			JProgressBar progressBar) throws AbortException {
		
		ArrayList<ArrayList<Point>> solutions = new ArrayList<ArrayList<Point>>();
		for(long i = 0; i < Math.pow(2, thisSectionPreUnkowns); i++) {
			progressBar.setValue((int) (((float) i)/((float) Math.pow(2, thisSectionPreUnkowns))*100.0) );
			
			if(abortSafePointSearch) {
				throw new AbortException("abort");
			}

			//generate all possible sequences of 1s and 0s that could replace the ?s
			String s = Long.toBinaryString(i);
			while(s.length() < thisSectionPreUnkowns) {
				s = "0" + s;
			}
			String sectionTestDist = s;
			
			//insert the test sequence of 1s and 0s to replace the ?s corresponding to the section
			String frontierTestDist = frontierDist;
			for(int j = 0; j < section.size(); j++) {
				Block block = section.get(j);
				int index = frontier.indexOf(block);
				frontierTestDist = frontierTestDist.substring(0, index) + s.charAt(j) + frontierTestDist.substring(index+1);
			}

			/**
			String[] indexes = indexLists.get(i).split(",");
			//stitch together a possible distribution for each zone, to get a complete possible distribution
			for(int j = 0; j < spanningZones.size(); j++) {
				Zone zone = spanningZones.get(j);
				Integer index = -1;
				try {
					index = Integer.parseInt(indexes[j]);
				}
				catch(NumberFormatException e) {
					System.out.println(indexes.length + "\n" + new ArrayList<String>(Arrays.asList(indexes)));
				}
				ArrayList<String> strings = zoneDists.get(j);
				String chosenZoneDist = strings.get(index);
				//insert the zone dist into the appropriate slots in testDist, based on where the zone blocks are actually located in the frontier
				for(int k = 0; k < zone.getBlocks().size(); k++) {
					Block block = zone.getBlocks().get(k);
					int blockIndex = frontier.indexOf(block);
					testDist = testDist.substring(0, blockIndex) + chosenZoneDist.charAt(k) + testDist.substring(blockIndex+1);
				}
			}
			 **/

			//generate list of mine locations from the constructed sequence of 1s and 0s
			ArrayList<Point> frontierMineLocations = new ArrayList<Point>();
			ArrayList<Point> sectionMineLocations = new ArrayList<Point>();
			for(int j = 0; j < frontierTestDist.length(); j++) {
				char c = frontierTestDist.charAt(j);
				Block block = frontier.get(j);
				if(c == '1') {
					Point p = new Point(block.getGridX(), block.getGridY());
					frontierMineLocations.add(p);
					if(section.contains(block)) {
						sectionMineLocations.add(p);
					}
				}
			}

			//check if it works
			if(satisfiesNeighborRevealedBlocks(frontier, section, frontierMineLocations)) {
				solutions.add(sectionMineLocations);
			}
		}
		return solutions;
	}

	private int minImpliedFrontierMines(ArrayList<Block> frontier, String dist) {
		//calculate the minimum number of mines that must be hidden on the frontier

		ArrayList<Zone> zones = makeZones(frontier, dist);
		int numImplied = 0;
		//give each zone a turn to be highest priority, see which yields highest number of implied mines
		for(int startIndex = 0; startIndex < zones.size(); startIndex++) {
			ArrayList<Block> tempFrontier = new ArrayList<Block>(frontier);
			int n = 0;
			for(int i = 0; i < zones.size(); i++) {
				Zone zone = zones.get((startIndex+i) % zones.size());
				ArrayList<Block> zoneBlocks = zone.getBlocks();
				if(tempFrontier.containsAll(zoneBlocks)) {
					tempFrontier.removeAll(zoneBlocks);
					n += zone.getMines();
				}
			}
			if(n > numImplied) {
				numImplied = n;
			}
		}
		return numImplied;
	}

	private ArrayList<Zone> listZonesSpanningSet(ArrayList<Block> set, ArrayList<Zone> zones) {
		//make a list of non-overlapping zones which covers most of the given set
		set = new ArrayList<Block>(set);
		ArrayList<Zone> spanningZones = new ArrayList<Zone>();
		for(Zone zone : zones) {
			if(set.containsAll(zone.getBlocks())) {
				spanningZones.add(zone);
				set.removeAll(zone.getBlocks());
			}
		}
		//the gaps form a zone with unknown number of mines
		Zone remainder = new Zone(null, set, -1);
		spanningZones.add(remainder);
		return spanningZones;
	}

	private ArrayList<String> getPossibleZoneDistributions(Zone zone) {
		int length = zone.getBlocks().size();

		if(zone.getMines() < 0) {
			//unknown mine quantity
			ArrayList<String> dists = new ArrayList<String>();
			for(int i = 0; i < Math.pow(2, length); i++) {
				String s = Long.toBinaryString(i);
				while(s.length() < length) {
					s = "0" + s;
				}
				dists.add(s);
			}
			return dists;
		}

		String dist = new String(new char[zone.getMines()]).replace("\0", "1");
		dist += new String(new char[length - dist.length()]).replace("\0", "0");

		ArrayList<String> dists = permute(dist);

		return dists;
	}

	public class BugException extends Exception {
		public BugException(String message) {
			super(message);
		}
	}

	public class AbortException extends Exception {
		public AbortException(String message) {
			super(message);
		}
	}

	public ArrayList<String> combinePossibleIndexes(ArrayList<Integer> maximums)  throws AbortException {
		//System.out.println("Combining indexes " + maximums.size() + " : " + maximums);
		ArrayList<String> combinations = new ArrayList<String>();
		generateCombinations(combinations, maximums);
		return combinations;
	}

	private void generateCombinations(ArrayList<String> combinations, ArrayList<Integer> maximums) throws AbortException {
		generateCombinations(combinations, maximums, 0, "");
	}

	private void generateCombinations(ArrayList<String> combinations, ArrayList<Integer> maximums, int intpos,
			String lastPerm) throws AbortException {

		if(abortSafePointSearch) {
			throw new AbortException("Abort");
		}

		if (intpos == maximums.size())
			return;

		for (int i = 0; i < maximums.get(intpos); i++) {
			if (intpos == maximums.size() - 1) {
				String s = lastPerm + "," + i;
				combinations.add(s.replaceAll("^,+", ""));
			}
			generateCombinations(combinations, maximums, intpos + 1, lastPerm + "," + i);
		}
	}

	private String fullyExtrapolateDistribution(ArrayList<Block> frontier, String dist, boolean quitASAP) {
		String startingDist = new String(dist);

		//using 1st order logic
		//deduce all we can about frontier, yielding a String of 1s, 0s, and ?s
		//for mines, safes, and unknowns
		boolean changed = true;
		while(changed) {
			String newDist = updateFrontierDistribution(frontier, dist, 1);
			changed = !newDist.equals(dist);
			dist = newDist;
		}

		if(quitASAP && !dist.equals(startingDist)) {
			//quitASAP means return after any info (mines or safes) found, don't keeping looking for more
			return dist;
		}

		//if still no safes, apply 2nd order logic before resorting to brute force
		//only do this if we have to so as not to not waste time
		int safes = dist.length() - dist.replace("0", "").length();
		if(safes == 0) {
			changed = true;
			while(changed) {
				String newDist = updateFrontierDistribution(frontier, dist, 1);
				newDist = updateFrontierDistribution(frontier, newDist, 2);
				changed = !newDist.equals(dist);
				dist = newDist;
			}
		}

		if(quitASAP && !dist.equals(startingDist)) {
			//quitASAP means return after any info (mines or safes) found, don't keeping looking for more
			return dist;
		}

		//if still no safes, apply 3rd order logic before resorting to brute force
		//only do this if we have to so as not to not waste time
		safes = dist.length() - dist.replace("0", "").length();
		if(safes == 0) {
			changed = true;
			while(changed) {
				String newDist = updateFrontierDistribution(frontier, dist, 1);
				newDist = updateFrontierDistribution(frontier, newDist, 2);
				newDist = updateFrontierDistribution(frontier, newDist, 3);
				changed = !newDist.equals(dist);
				dist = newDist;
			}
		}

		return dist;
	}

	private ArrayList<Point> listSafePoints(String dist, ArrayList<Block> frontier) {
		ArrayList<Point> safePoints = new ArrayList<Point>();
		for(int i = 0; i < dist.length(); i++) {
			if(dist.charAt(i) == '0') {
				Block block = frontier.get(i);
				Point p = new Point(block.getGridX(), block.getGridY());
				safePoints.add(p);
			}
		}
		return safePoints;
	}

	private void test(int trials, String algorithm) {
		JFrame testFrame = new JFrame("Test");
		testFrame.setLocation(this.getLocation().x + this.getSize().width, this.getLocation().y);
		JPanel testPanel = new JPanel();

		testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.Y_AXIS));
		testFrame.add(testPanel);
		JTextArea testArea = new JTextArea();
		testArea.setEditable(false);
		JScrollPane textScroll = new JScrollPane(testArea);
		textScroll.setPreferredSize(new Dimension(300, 200));
		testPanel.add(textScroll);

		JPanel progressPanel = new JPanel();
		testPanel.add(progressPanel);
		progressPanel.setLayout(flow);
		JProgressBar progressBar = new JProgressBar();
		progressPanel.add(progressBar);

		JPanel abortPanel = new JPanel();
		testPanel.add(abortPanel);
		JButton nextButton = new JButton("Next");
		JButton useBruteForce = new JButton("Brute force");
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				next = true;
				nextButton.setEnabled(false);
				useBruteForce.setEnabled(false);
			}
		});
		nextButton.setEnabled(false);
		abortPanel.add(nextButton);
		JButton abortButton = new JButton("Abort");
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abort = true;
				next = true;
				nextButton.setEnabled(false);
				useBruteForce.setEnabled(false);
			}
		});
		abortPanel.add(abortButton);
		useBruteForce.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				next = true;
				resumeWithBruteForce = true;
				nextButton.setEnabled(false);
				useBruteForce.setEnabled(false);
			}
		});
		useBruteForce.setEnabled(false);
		abortPanel.add(useBruteForce);

		testFrame.pack();
		testFrame.setVisible(true);
		testFrame.addWindowListener(new WindowListener() {
			public void windowOpened(WindowEvent e) {}
			public void windowIconified(WindowEvent e) {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowDeactivated(WindowEvent e) {}
			public void windowClosing(WindowEvent e) {
				abort = true;
				test.setEnabled(true);
				testQuickSolve.setEnabled(true);
			}
			public void windowClosed(WindowEvent e) {}
			public void windowActivated(WindowEvent e) {}
		});

		Thread tester = new Thread(new Runnable() {
			public void run() {
				Timer timer = new Timer();
				timer.start();
				test.setEnabled(false);
				testQuickSolve.setEnabled(false);
				abort = false;
				double n = trials;
				int numWins = 0;
				int numLargeUnknowns = 0;
				int numNoSafePoints = 0;
				int numBoom = 0;
				int numBugs = 0;
				double i = 0;
				for(i = 0; i < n; i++) {
					progressBar.setValue((int) (100.0*i/(n-1)));
					if(abort) {
						break;
					}
					newGame();
					ArrayList<Block> aBlocks = getBlocks();
					Block firstBlock = aBlocks.get(rand.nextInt(aBlocks.size()));
					placeMines(firstBlock);
					reveal(firstBlock);
					while(status == GameStatus.Ongoing) {
						try {
							int flagsRemaining = flagsRemaining();
							ArrayList<Point> safePoints = null;
							if(algorithm.equals("Bigbrain")) {
								try {
									safePoints = bigBrain(false, resumeWithBruteForce, true);
								}
								catch(BugException be) {
									testArea.setText("Bug\n\n" + be.getMessage());
									next = false;
									nextButton.setEnabled(true);
									int waiting = 0;
									timer.pause();
									while(waiting < 10000 && !next) {
										waiting++;
										try {
											Thread.sleep(100);
										}
										catch(InterruptedException ex) {}
									}
									timer.resume();
									if(!next) {
										abort = true;
									}
									else {
										testArea.setText("");
									}
									numBugs++;
									break;
								}
							}
							else if(algorithm.endsWith("Quicksolve")) {
								safePoints = quickSolve();
							}
							resumeWithBruteForce = false;
							if(userClickedAbortSafePointSearch) {
								userClickedAbortSafePointSearch = false;
								numLargeUnknowns++;
								break;
							}
							if(safePoints.isEmpty() && flagsRemaining == flagsRemaining()) {
								//if we couldn't find any safe points or place any flags
								if(stopOnNoSafeMoves.isSelected()) {
									testArea.setText("No safe moves");
									next = false;
									nextButton.setEnabled(true);
									int waiting = 0;
									timer.pause();
									while(waiting < 10000 && !next) {
										waiting++;
										try {
											Thread.sleep(100);
										}
										catch(InterruptedException ex) {}
									}
									timer.resume();
									if(!next) {
										abort = true;
									}
									else {
										testArea.setText("");
									}
								}
								numNoSafePoints++;
								break;
							}
							for(Point p : safePoints) {
								reveal(blocks.get(p.x).get(p.y));
							}
						}
						catch(BadBoardException ex) {
							//abort game
							String reason = ex.getMessage();
							if(stopOnComplexBoard.isSelected()) {
								testArea.setText("Board too complex\n\n" + ex.getMessage());
								next = false;
								nextButton.setEnabled(true);
								useBruteForce.setEnabled(true);
								int waiting = 0;
								timer.pause();
								while(waiting < 1000 && !next) {
									waiting++;
									try {
										Thread.sleep(100);
									}
									catch(InterruptedException iex) {}
								}
								timer.resume();
								if(!next) {
									abort = true;
								}
								else {
									testArea.setText("");
								}
							}
							if(!resumeWithBruteForce) {
								if(reason.contains("large")) {
									numLargeUnknowns++;
								}
								break;
							}
						}
					}
					if(status == GameStatus.Won) {
						numWins++;
					}
					else if(status == GameStatus.Lost) {
						numBoom++;
						testArea.setText("Boom");
						next = false;
						nextButton.setEnabled(true);
						int waiting = 0;
						timer.pause();
						while(waiting < 10000 && !next) {
							waiting++;
							try {
								Thread.sleep(100);
							}
							catch(InterruptedException ex) {}
						}
						timer.resume();
						if(!next) {
							abort = true;
						}
						else {
							testArea.setText("");
						}
						break;
					}
				}
				int secondsElapsed = (int) (timer.read()/1000);
				String output = "Trials: " + (int) i +
						"\nSeconds elapsed: " + secondsElapsed + "\n" + 
						"\nWin: " + numWins +
						"\nBoard too complex to evaluate: " + numLargeUnknowns +
						"\nNo safe moves: " + numNoSafePoints +
						"\nBoom: " + numBoom +
						"\nBugs: " + numBugs;
				testArea.setText(output);
				testFrame.pack();
				abortButton.setEnabled(false);
				nextButton.setEnabled(false);
				useBruteForce.setEnabled(false);
				test.setEnabled(true);
				testQuickSolve.setEnabled(true);
			}
		});
		//System.out.println("Current thread: " + Thread.currentThread().getName() + ". starting testerThread: " + tester.getName());
		tester.setPriority(Thread.MIN_PRIORITY);;
		tester.start();
	}

	private String pointsToString(ArrayList<Point> points) {
		String string = "";
		for(Point p : points) {
			string += "(" + p.x + ", " + p.y + "), ";
		}
		if(string.length() > 2) {
			string = string.substring(0, string.length()-2);
		}
		return string;
	}

	private String blocksToString(ArrayList<Block> blocks) {
		String string = "";
		for(Block b : blocks) {
			string += b + ", ";
		}
		if(string.length() > 2) {
			string = string.substring(0, string.length()-2);
		}
		return string;
	}

	public ArrayList<String> permute(String s) {
		char a[]= s.toCharArray();
		ArrayList<String> solutions = new ArrayList<String>();
		getPermutations(solutions, a, 0, a.length);
		return solutions;
	}

	private void getPermutations(ArrayList<String> solutions, char[] a, int startIndex, int endIndex) {
		if (startIndex == endIndex) {
			//reached end of recursion, print the state of a
			solutions.add(new String(a));
		}
		else {
			//try to move the swap window from start index to end index
			//i.e 0 to a.length-1
			for (int x = startIndex; x < endIndex; x++) {
				swap(a, startIndex, x);
				getPermutations(solutions, a, startIndex + 1, endIndex);
				swap(a, startIndex, x);
			}
		}
	}

	private void swap(char[] a, int i, int j) {
		char temp = a[i];
		a[i] = a[j];
		a[j] = temp;
	}

	private boolean isLegal(ArrayList<Block> frontier, String dist) {
		//if the proposed mine locations do not contradict the numbers on any REVEALED squares
		//they need not fully satisfy the numbers
		int numMines = dist.length() - dist.replace("1", "").length();
		if(numMines > this.minesNeeded) {
			return false;
		}
		int numUnmarkedBlocksNotOnFrontier = 0;
		for(Block b : getBlocks()) {
			if(!(b.getStatus() == Block.Status.Revealed) && !frontier.contains(b)) {
				numUnmarkedBlocksNotOnFrontier++;
			}
		}
		if(numMines + numUnmarkedBlocksNotOnFrontier < minesNeeded) {
			//if not enough additional empty spaces to place required mines 
			//return false;
		}
		for(Block block : getBlocks()) {
			if(block.getStatus() == Block.Status.Revealed && block.getNeighborMines() != 0) {
				int adjMines = 0;
				int adjSafes = 0;
				int adjUnk = 0;
				for(int i = 0; i < frontier.size(); i++) {
					Block frontBlock = frontier.get(i);
					if(adjacent(block, frontBlock)) {
						char c = dist.charAt(frontier.indexOf(frontBlock));
						if(c == '1' || frontBlock.getStatus() == Block.Status.Flagged) {
							adjMines++;
						}
						else if(c == '0') {
							adjSafes++;
						}
						else {
							adjUnk++;
						}
					}
				}
				if(adjMines > block.getNeighborMines()) {
					return false;
				}
				if(adjMines + adjUnk < block.getNeighborMines()) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean satisfiesNeighborRevealedBlocks(ArrayList<Block> frontier, ArrayList<Block> section, ArrayList<Point> mineLocations) {
		//if the proposed mine locations fully satisfy the numbers on all REVEALED squares

		for(Point mineLoc : mineLocations) {
			Block block = blocks.get(mineLoc.x).get(mineLoc.y);
			if(block.getStatus() == Block.Status.Revealed) {
				return false;
			}
		}
		for(Block block : getBlocks()) {
			if(block.getStatus() == Block.Status.Revealed && block.getNeighborMines() != 0) {
				if(section != null && adjacent(block, section)) {
					int proposedNeighborMines = 0;
					for(Point loc : mineLocations) {
						if(Math.abs(loc.x - block.getGridX()) <= 1 && Math.abs(loc.y - block.getGridY()) <= 1) {
							proposedNeighborMines++;
						}
					}
					if(proposedNeighborMines != block.getNeighborMines()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private String updateFrontierDistribution(ArrayList<Block> frontier, String dist, int logicOrder) {
		ArrayList<Block> revealedBorder = revealedBorder(frontier, false);
		if(logicOrder == 1) {
			//1st order logic
			for(Block block : revealedBorder) {
				//consider all revealed blocks (which are numbered) adjacent to the frontier
				dist = updateFrontierDistByBlock(block, block.getNeighborMines(), frontier, dist);
			}
		}

		else if(logicOrder == 2 || logicOrder == 3) {
			ArrayList<Zone> zones = null;
			if(logicOrder == 2) {
				zones = makeZones(frontier, dist);
			}
			if(logicOrder == 3) {
				zones = getCompoundZones(frontier, dist);
			}
			//a zone of unknown blocks around each revealedborder block has a known number of mines

			Block dummy = new Block(-2, -2, 0);
			for(Block block : revealedBorder) {
				//2nd order logic: considering implied location of mines in zones,
				//construct a revised frontier excluding that zone and its mines, infer
				for(Zone zone : zones) {
					ArrayList<Block> adjFrontBlocks = adjBlocks(block, frontier);

					//proceed only if the zone is a subset of the neighbors of the block
					if(!zone.getSourceBlocks().contains(block) && adjFrontBlocks.containsAll(zone.getBlocks()) && adjFrontBlocks.size() > zone.getBlocks().size()) {
						//						System.out.println(zone);
						//						System.out.println("Revealed border block: " + block);
						//						System.out.println("AdjFrontBlocks: " + blocksToString(adjFrontBlocks));
						//						System.out.println("proceeding");
						//						System.out.println();


						ArrayList<Block> revisedFrontier = new ArrayList<Block>(frontier);

						//revised frontier removes the zone's blocks and replaces with dummies to preserve ordering
						for(Block zb : zone.getBlocks()) {
							int index = revisedFrontier.indexOf(zb);
							revisedFrontier.set(index, dummy);
						}
						int revisedNeighborMines = block.getNeighborMines() - zone.getMines();
						String oldDist = dist;
						dist = updateFrontierDistByBlock(block, revisedNeighborMines, revisedFrontier, dist);
						//						if(!oldDist.equals(dist)) {
						//							System.out.println(oldDist + " to " + dist);
						//							System.out.println();
						//						}
					}
				}
			}
		}
		return dist;
	}

	private ArrayList<Block> revealedBorder(ArrayList<Block> frontier, boolean orderMatters) {
		Timer timer = new Timer();
		timer.start();
		ArrayList<Block> revealedBorder = new ArrayList<Block>();
		//revealedBorder is all revealed blocks that have >= 1 frontier neighbor
		if(!orderMatters) {
			for(Block block : getBlocks()) {
				if(block.getStatus() == Block.Status.Revealed && adjacent(block, frontier)) {
					revealedBorder.add(block);
				}
			}
			//System.out.println("time to find RB (ordermatters " + orderMatters + "): " + ((int) timer.read()));
			return revealedBorder;
		}
		
		//assemble it neighbor-wise so it is in order
		boolean newBlocksFound = true;
		while(newBlocksFound) {
			Block newBlock = null;
			for(Block block : getBlocks()) {
				if(block.getStatus() == Block.Status.Revealed && adjacent(block, frontier) && !revealedBorder.contains(block)) {
					newBlock = block;
					break;
				}
			}
			if(newBlock != null) {
				addToRevealedBorder(newBlock, revealedBorder, frontier, new ArrayList<Block>());
			}

			newBlocksFound = false;
			for(Block block : getBlocks()) {
				if(block.getStatus() == Block.Status.Revealed && adjacent(block, frontier) && !revealedBorder.contains(block)) {
					newBlocksFound = true;
				}
			}
		}
		System.out.println("time to find RB (ordermatters " + orderMatters + "): " + ((int) timer.read()));
		return revealedBorder;
	}
	
	private void addToRevealedBorder(Block block, ArrayList<Block> revealedBorder, ArrayList<Block> frontier, ArrayList<Block> traveled) {
		if(!revealedBorder.contains(block)) {
			revealedBorder.add(block);
		}
		traveled.add(block);
		ArrayList<Block> neighborsToAdd = new ArrayList<Block>();
		for(Block neighbor : getOrthogonalNeighbors(block)) {
			if(neighbor.getStatus() == Block.Status.Revealed && adjacent(neighbor, frontier) && !revealedBorder.contains(neighbor)) {
				neighborsToAdd.add(neighbor);
			}
		}
		for(Block neighbor : getDiagonalNeighbors(block)) {
			if(neighbor.getStatus() == Block.Status.Revealed && adjacent(neighbor, frontier) && !revealedBorder.contains(neighbor)) {
				neighborsToAdd.add(neighbor);
			}
		}
		for(Block neighbor : neighborsToAdd) {
			revealedBorder.add(neighbor);
		}
		for(Block neighbor : neighborsToAdd) {
			addToRevealedBorder(neighbor, revealedBorder, frontier, traveled);
		}
	}

	private ArrayList<Zone> makeZones(ArrayList<Block> frontier, String dist) {
		ArrayList<Block> revealedBorder = revealedBorder(frontier, false);
		ArrayList<Zone> zones = new ArrayList<Zone>();
		for(Block block : revealedBorder) {
			Zone zone = makeZone(block, frontier, dist);
			if(zone != null) {
				zones.add(zone);
			}
		}
		return zones;
	}
	
	private Zone makeZone(Block sourceBlock, ArrayList<Block> frontier, String dist) {
		ArrayList<Block> adjFrontBlocks = adjBlocks(sourceBlock, frontier);
		int zoneMines = sourceBlock.getNeighborMines();
		Iterator<Block> afbIt = adjFrontBlocks.iterator();
		while(afbIt.hasNext()) {
			Block adjFrontBlock = afbIt.next();
			int index = frontier.indexOf(adjFrontBlock);
			if(adjFrontBlock.getStatus() == Block.Status.Flagged || dist.charAt(index) == '1') {
				//remove already flagged blocks from the zone
				afbIt.remove();
				zoneMines--;

			}
			else if(dist.charAt(index) == '0') {
				//remove already safe blocks from the zone
				afbIt.remove();
			}
		}
		ArrayList<Block> sourceBlocks = new ArrayList<Block>();
		sourceBlocks.add(sourceBlock);
		if(!adjFrontBlocks.isEmpty()) {
			return new Zone(sourceBlocks, adjFrontBlocks, zoneMines);
		}
		return null;
	}
	
	private Zone subtractZone(Zone zone1, Zone zone2) {
		ArrayList<Block> zone1Blocks = new ArrayList<Block>(zone1.getBlocks());
		ArrayList<Block> zone2Blocks = zone2.getBlocks();
		if(!zone1Blocks.containsAll(zone2Blocks)) {
			return null;
		}
		zone1Blocks.removeAll(zone2Blocks);
		return new Zone(combineSets(zone1Blocks, zone2Blocks), zone1Blocks, zone1.getMines() - zone2.getMines());
	}
	
	private Zone intersectionZone(Zone zone1, Zone zone2) {
		ArrayList<Block> intersection = intersection(zone1.getBlocks(), zone2.getBlocks());
		if(intersection.size() == 0 || intersection.size() == 1) {
			//assuming both zones are unknown, can't infer anything from overlap unless at least 2
			return null;
		}
		int minMinesInIntersection = Math.max(zone1.getMines() - (zone1.getBlocks().size() - intersection.size()), 
				zone2.getMines() - (zone2.getBlocks().size() - intersection.size()));
		int maxMinesInIntersection = Math.max(intersection.size(), Math.max(zone1.getMines(), zone2.getMines()));
		int intersectionMines = -1;
		if(minMinesInIntersection == maxMinesInIntersection) {
			intersectionMines = minMinesInIntersection;
		}
		if(minMinesInIntersection == zone1.getMines() || minMinesInIntersection == zone2.getMines()) {
			intersectionMines = minMinesInIntersection;
		}
		Zone intersectionZone = new Zone(combineSets(zone1.getSourceBlocks(), zone2.getSourceBlocks()), intersection, intersectionMines);
		return intersectionZone;
	}
	
	private <T> ArrayList<T> combineSets(ArrayList<T> set1, ArrayList<T> set2) {
		ArrayList<T> combined = new ArrayList<T>();
		combined.addAll(set1);
		for(T element : set2) {
			if(!combined.contains(element)) {
				combined.add(element);
			}
		}
		return combined;
	}

	private ArrayList<Zone> getCompoundZones(ArrayList<Block> frontier, String dist) {
		ArrayList<Zone> zones = makeZones(frontier, dist);
		ArrayList<Zone> compoundZones = new ArrayList<Zone>();
		for(Zone zone1 : zones) {
			for(Zone zone2 : zones) {
				if(!zone1.equals(zone2) && intersection(zone1.getBlocks(), zone2.getBlocks()).isEmpty() && closestDistance(zone1.getBlocks(), zone2.getBlocks()) <= 2) {
					//add two non-overlapping zones together
					ArrayList<Block> sourceBlocks = combineSets(zone1.getSourceBlocks(), zone2.getSourceBlocks());
					ArrayList<Block> blocks = combineSets(zone1.getBlocks(), zone2.getBlocks());
					compoundZones.add(new Zone(sourceBlocks, blocks, zone1.getMines() + zone2.getMines()));
				}
			}
		}
		return compoundZones;
	}

	private int closestDistance(ArrayList<Block> arr1, ArrayList<Block> arr2) {
		//the distance is the number of steps required to move between the two block sets
		int closestDistance = 1000;
		for(Block b1 : arr1) {
			for(Block b2 : arr2) {
				int distance = Math.max(Math.abs(b1.getGridX() - b2.getGridX()), Math.abs(b1.getGridY() - b2.getGridY()));
				if(distance < closestDistance) {
					closestDistance = distance;
				}
			}
		}
		return closestDistance;
	}

	private ArrayList<Block> intersection(ArrayList<Block> arr1, ArrayList<Block> arr2) {
		ArrayList<Block> intersection = new ArrayList<Block>();
		for(Block b1 : arr1) {
			if(arr2.contains(b1)) {
				intersection.add(b1);
			}
		}
		return intersection;
	}

	private String updateFrontierDistByBlock(Block block, int neighborMines, ArrayList<Block> frontier, String dist) {
		//look at a numbered block adjacent to the frontier and see if it can be used
		//to label frontier blocks as mines or safe

		ArrayList<Integer> adjIndices = adjIndices(block, frontier);
		int frontierBlocksAdj = numAdjacent(block, frontier);

		if(frontierBlocksAdj == 0) {
			return dist;
		}

		int flagsAdj = 0;
		int safeAdj = 0;

		//count known mines or safes adj to them
		for(int index : adjIndices) {
			Block frontierBlock = frontier.get(index);
			char status = dist.charAt(index);
			if(adjacent(frontierBlock, block)) {
				if(status == '1') {
					flagsAdj++;
				}
				else if(status == '0') {
					safeAdj++;
				}
			}
		}
		//compare number of unk adj to the number of expected mines, mark unk as mines or safes
		int unknownAdj = frontierBlocksAdj - flagsAdj - safeAdj;
		if(neighborMines == unknownAdj + flagsAdj) {
			//if missing mines = unknowns
			//then all unknown must be flagged
			for(int index : adjIndices) {
				if(dist.charAt(index) == '?') {
					dist = dist.substring(0, index) + '1' + dist.substring(index+1);
				}
			}
		}
		else if(neighborMines == flagsAdj) {
			//if missing mines = 0
			//then all unknown must be safed
			for(int index : adjIndices) {
				if(dist.charAt(index) == '?') {
					dist = dist.substring(0, index) + '0' + dist.substring(index+1);
				}
			}
		}
		return dist;
	}

	private ArrayList<Integer> adjIndices(Block b, ArrayList<Block> set) {
		ArrayList<Integer> adjIndices = new ArrayList<Integer>();
		for(int i = 0; i < set.size(); i++) {
			if(adjacent(b, set.get(i))) {
				adjIndices.add(i);
			}
		}
		return adjIndices;
	}

	private ArrayList<Block> adjBlocks(Block b, ArrayList<Block> set) {
		ArrayList<Block> adjIndices = new ArrayList<Block>();
		for(int i = 0; i < set.size(); i++) {
			if(adjacent(b, set.get(i))) {
				adjIndices.add(set.get(i));
			}
		}
		return adjIndices;
	}

	private int numAdjacent(Block b, ArrayList<Block> set2) {
		int numAdjacent = 0;
		for(Block b2 : set2) {
			if(adjacent(b, b2)) {
				numAdjacent++;
			}
		}
		return numAdjacent;
	}

	private boolean adjacent(Block b1, Block b2) {
		if(b1.equals(b2)) {
			return false;
		}
		return Math.abs(b1.getGridX() - b2.getGridX()) <= 1 && Math.abs(b1.getGridY() - b2.getGridY()) <= 1;
	}
	
	private boolean adjacent(Block b, ArrayList<Block> set2) {
		for(Block b2 : set2) {
			if(adjacent(b, b2)) {
				return true;
			}
		}
		return false;
	}

	private void setFlagged(Block b, boolean flagged) {
		if(flagged) {
			b.setStatus(Block.Status.Flagged);
		}
		else {
			b.setStatus(Block.Status.Unmarked);
		}
		flagsRemainingLabel.setText("" + flagsRemaining());
	}

	public static Image loadImage(String path, int width, int height, boolean allowScreenScaling) {
		if(path == null) {
			return null;
		}
		Image image = null;
		try {
			image = ImageIO.read(Main.class.getResource("/" + path));
		}
		catch(IOException e) {
			System.err.println(e.getClass());
			System.err.println(e.getMessage());
		}
		image = resize(image, width, height, allowScreenScaling);
		return image;
	}

	public static Image resize(Image image, int width, int height, boolean allowScreenScaling) {
		//assume default screen height of 1000 and shrink images if screen is smaller
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		double screenHeight = (double) screenSize.getHeight();
		if(screenHeight < 1000 && allowScreenScaling) {
			width = (width*((int) screenHeight))/1000;
			height = (height*((int) screenHeight))/1000;
		}

		if(width > 0 && height > 0) {
			image = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
		}
		return image;
	}
	
	public static BufferedImage rotate(Image img, double angle)
	{
	    double sin = Math.abs(Math.sin(Math.toRadians(angle))),
	           cos = Math.abs(Math.cos(Math.toRadians(angle)));

	    int w = img.getWidth(null), h = img.getHeight(null);

	    int neww = (int) Math.floor(w*cos + h*sin),
	        newh = (int) Math.floor(h*cos + w*sin);

	    BufferedImage bimg = new BufferedImage(neww, newh, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = bimg.createGraphics();

	    g.translate((neww-w)/2, (newh-h)/2);
	    g.rotate(Math.toRadians(angle), w/2, h/2);
	    g.drawRenderedImage(toBufferedImage(img), null);
	    g.dispose();

	    return bimg;
	}
	
	public static BufferedImage toBufferedImage(Image img)
	{
	    if (img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }

	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();

	    // Return the buffered image
	    return bimage;
	}
}