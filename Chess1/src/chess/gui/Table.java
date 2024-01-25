package chess.gui;

import static javax.swing.SwingUtilities.isLeftMouseButton;
import static javax.swing.SwingUtilities.isRightMouseButton;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import com.google.common.collect.Lists;

import chess.engine.board.Board;
import chess.engine.board.BoardUtils;
import chess.engine.board.Move;
import chess.engine.board.Move.MoveFactory;
import chess.engine.board.MoveTransition;
import chess.engine.pieces.Piece;
import chess.engine.player.Player;

public final class Table {

    private static JFrame gameFrame;
    private final GameHistoryPanel gameHistoryPanel;
    private final TakenPiecesPanel takenPiecesPanel;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;
    private static Board chessBoard;
    private Piece sourceTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;
    private String pieceIconPath;
    private boolean highlightLegalMoves;
    private Color lightTileColor = Color.decode("#DCDCDC");
    private Color darkTileColor = Color.decode("#5F656C");
	private final Lobby lobby;
	private final ChessTimer chessTimer;

    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(600, 550);
    private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(400, 350);
    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);

    private static final Table INSTANCE = new Table();

    private Table() {
        Table.gameFrame = new JFrame("chess");
        final JMenuBar tableMenuBar = new JMenuBar();
        populateMenuBar(tableMenuBar);
        Table.gameFrame.setJMenuBar(tableMenuBar);
        Table.gameFrame.setLayout(new BorderLayout());
        Table.chessBoard = Board.createStandardBoard();
        this.boardDirection = BoardDirection.NORMAL;
        this.highlightLegalMoves = false;
        this.pieceIconPath = "src/image/simple/";
        this.gameHistoryPanel = new GameHistoryPanel();
        this.takenPiecesPanel = new TakenPiecesPanel();
        this.boardPanel = new BoardPanel();
        this.lobby = new Lobby();
        this.chessTimer = new ChessTimer();
        this.moveLog = new MoveLog();
        Table.gameFrame.add(this.takenPiecesPanel, BorderLayout.WEST);
        Table.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        Table.gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);
        Table.gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Table.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        center(Table.gameFrame);
        Table.gameFrame.setVisible(false);
    }

    public static Table get() {
        return INSTANCE;
    }

    private JFrame getGameFrame() {
        return Table.gameFrame;
    }

    private Board getGameBoard() {
        return Table.chessBoard;
    }

    private MoveLog getMoveLog() {
        return this.moveLog;
    }

    private BoardPanel getBoardPanel() {
        return this.boardPanel;
    }
    
    private Lobby getLobby() {
        return this.lobby;
    }
    
    private ChessTimer getChessTimer() {
        return this.chessTimer;
    }

    private GameHistoryPanel getGameHistoryPanel() {
        return this.gameHistoryPanel;
    }

    private TakenPiecesPanel getTakenPiecesPanel() {
        return this.takenPiecesPanel;
    }

    private boolean getHighlightLegalMoves() {
        return this.highlightLegalMoves;
    }

    public static void win() {
    	if(chessBoard.currentPlayer().isInCheckMate() && chessBoard.currentPlayer().getAlliance().toString().equals("White")) {
        JOptionPane.showMessageDialog(gameFrame, "Black Win!");
	    }else if(chessBoard.currentPlayer().isInCheckMate() && chessBoard.currentPlayer().getAlliance().toString().equals("Black")) {
	    	JOptionPane.showMessageDialog(gameFrame, "White Win!");
	    }
    }

    public void show() {
        Table.get().getMoveLog().clear();
        Table.get().getLobby();
        Table.get().getChessTimer();
        Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
        Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
    }

    private void populateMenuBar(final JMenuBar tableMenuBar) {
        tableMenuBar.add(createPreferencesMenu());
        tableMenuBar.add(createOptionsMenu());
    }

    private static void center(final JFrame frame) {
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        final int w = frame.getSize().width;
        final int h = frame.getSize().height;
        final int x = (dim.width - w) / 2;
        final int y = (dim.height - h) / 2;
        frame.setLocation(x, y);
    }

    private JMenu createOptionsMenu() {

        final JMenu optionsMenu = new JMenu("Options");

        final JMenuItem resetMenuItem = new JMenuItem("New Game");
        resetMenuItem.addActionListener(e -> undoAllMoves());
        optionsMenu.add(resetMenuItem);

        final JMenuItem legalMovesMenuItem = new JMenuItem("Current State");
        legalMovesMenuItem.addActionListener(e -> {
            System.out.println(chessBoard.getWhitePieces());
            System.out.println(chessBoard.getBlackPieces());
            System.out.println(playerInfo(chessBoard.currentPlayer()));
            System.out.println(playerInfo(chessBoard.currentPlayer().getOpponent()));
        });
        optionsMenu.add(legalMovesMenuItem);

        final JMenuItem undoMoveMenuItem = new JMenuItem("Undo last move");
        undoMoveMenuItem.addActionListener(e -> {
            if(Table.get().getMoveLog().size() > 0) {
                undoLastMove();
            }
        });
        optionsMenu.add(undoMoveMenuItem);
        
        final JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(e -> {
            Table.get().getGameFrame().dispose();
            System.exit(0);
        });
        optionsMenu.add(exitMenuItem);

        return optionsMenu;
    }

    private JMenu createPreferencesMenu() {

        final JMenu preferencesMenu = new JMenu("Preferences");

        final JMenu colorChooserSubMenu = new JMenu("Choose Colors");

        final JMenuItem chooseDarkMenuItem = new JMenuItem("Choose Dark Tile Color");
        colorChooserSubMenu.add(chooseDarkMenuItem);

        final JMenuItem chooseLightMenuItem = new JMenuItem("Choose Light Tile Color");
        colorChooserSubMenu.add(chooseLightMenuItem);

        chooseDarkMenuItem.addActionListener(e -> {
            final Color colorChoice = JColorChooser.showDialog(Table.get().getGameFrame(), "Choose Dark Tile Color",
                    Table.get().getGameFrame().getBackground());
            if (colorChoice != null) {
                Table.get().getBoardPanel().setTileDarkColor(chessBoard, colorChoice);
            }
        });

        chooseLightMenuItem.addActionListener(e -> {
            final Color colorChoice = JColorChooser.showDialog(Table.get().getGameFrame(), "Choose Light Tile Color",
                    Table.get().getGameFrame().getBackground());
            if (colorChoice != null) {
                Table.get().getBoardPanel().setTileLightColor(chessBoard, colorChoice);
            }
        });

        final JMenu chessMenChoiceSubMenu = new JMenu("Choose Chess Piece Set");

        final JMenuItem holyWarriorsMenuItem = new JMenuItem("Holy Warriors");
        chessMenChoiceSubMenu.add(holyWarriorsMenuItem);

        final JMenuItem simpleMenMenuItem = new JMenuItem("simple");
        chessMenChoiceSubMenu.add(simpleMenMenuItem);

        final JMenuItem fancyMenMenuItem = new JMenuItem("Fancy");
        chessMenChoiceSubMenu.add(fancyMenMenuItem);

        final JMenuItem fancyMenMenuItem2 = new JMenuItem("Fancy 2");
        chessMenChoiceSubMenu.add(fancyMenMenuItem2);

        holyWarriorsMenuItem.addActionListener(e -> {
            pieceIconPath = "src/image/holywarriors/";
            Table.get().getBoardPanel().drawBoard(chessBoard);
        });

        simpleMenMenuItem.addActionListener(e -> {
            pieceIconPath = "src/image/simple/";
            Table.get().getBoardPanel().drawBoard(chessBoard);
        });

        fancyMenMenuItem2.addActionListener(e -> {
            pieceIconPath = "src/image/fancy2/";
            Table.get().getBoardPanel().drawBoard(chessBoard);
        });

        fancyMenMenuItem.addActionListener(e -> {
            pieceIconPath = "src/image/fancy/";
            Table.get().getBoardPanel().drawBoard(chessBoard);
        });

        preferencesMenu.add(chessMenChoiceSubMenu);

        final JMenuItem flipBoardMenuItem = new JMenuItem("Flip board");

        flipBoardMenuItem.addActionListener(e -> {
            boardDirection = boardDirection.opposite();
            if(boardDirection.toString().equals("FLIPPED")) {
            	ChessTimer.normal();
            }else {
            	ChessTimer.flip();
            }
            boardPanel.drawBoard(chessBoard);
        });

        preferencesMenu.add(flipBoardMenuItem);
        preferencesMenu.addSeparator();


//        final JCheckBoxMenuItem cbLegalMoveHighlighter = new JCheckBoxMenuItem(
//                "Highlight Legal Moves", true);
//
//        cbLegalMoveHighlighter.addActionListener(e -> highlightLegalMoves = cbLegalMoveHighlighter.isSelected());
//
//        preferencesMenu.add(cbLegalMoveHighlighter);

        return preferencesMenu;

    }

    private static String playerInfo(final Player player) {
        return ("Player is: " +player.getAlliance() + "\nlegal moves (" +player.getLegalMoves().size()+ ") = " +player.getLegalMoves() + "\ninCheck = " +
                player.isInCheck() + "\nisInCheckMate = " +player.isInCheckMate() +
                "\nisCastled = " +player.isCastled())+ "\n";
    }

    private void undoAllMoves() {
        for(int i = Table.get().getMoveLog().size() - 1; i >= 0; i--) {
            final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
            Table.chessBoard = Table.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        }
        ChessTimer.newGame();
        Table.get().getMoveLog().clear();
        Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
        Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(chessBoard);
    }

    private void undoLastMove() {
        final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
        Table.chessBoard = Table.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        Table.get().getMoveLog().removeMove(lastMove);
        Table.get().getGameHistoryPanel().redo(chessBoard, Table.get().getMoveLog());
        Table.get().getTakenPiecesPanel().redo(Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(chessBoard);
    }

    enum PlayerType {
        HUMAN,
        COMPUTER
    }
    
    private class Lobby extends JFrame {
    	 
        private Lobby() {
     
            super("Start");
            JPanel jPanel = new JPanel();
            JButton startBtn = new JButton("Play Chess");
            setSize(OUTER_FRAME_DIMENSION);
            jPanel.add(startBtn);
            add(jPanel);
     
            Dimension frameSize = getSize();
     
            Dimension windowSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation((windowSize.width - frameSize.width) / 2,
                    (windowSize.height - frameSize.height) / 2);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setVisible(true);
     
            startBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ChessTimer.startWhiteTimer();
                	gameFrame.setVisible(true);
                    setVisible(false);
                }
            });
        }
    }
    
    private static class ChessTimer extends JPanel{
    	
    	private static final int TIMER_DURATION = 10 * 60;
    	private static final String DEFAULT_TIMER_TEXT = "10:00";
    	private static int whiteTime = TIMER_DURATION;
    	private static int blackTime = TIMER_DURATION;
    	private static Timer whiteTimer;
    	private static Timer blackTimer;
    	private static int timeGoing = -1;
    	private static int[] WPB = {395, 6, 70, 30};
    	private static int[] BPB = {60, 453, 70, 30};
    	private static int[] WLB = {400, -30, 100, 100};
    	private static int[] BLB = {65, 417, 100, 100};
    			
    	ChessTimer(){
    		JPanel whiteTimerPanel = new JPanel();
            JLabel whiteTimerLabel = new JLabel(DEFAULT_TIMER_TEXT);
            JPanel blackTimerPanel = new JPanel();
            JLabel blackTimerLabel = new JLabel(DEFAULT_TIMER_TEXT);
            
            whiteTimer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                	if(timeGoing==1) {
	                	whiteTime--;
	                	if (whiteTime == 0) {
                        ((Timer) e.getSource()).stop();
                        JOptionPane.showMessageDialog(gameFrame, "White Time Out!");
	                	}
                    }
                    whiteTimerLabel.setText(getFormattedTime(whiteTime));
                    
                }
            });
            
            blackTimer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                	if(timeGoing==0) {
	                	blackTime--;
	                	if (blackTime == 0) {
                        ((Timer) e.getSource()).stop();
                        JOptionPane.showMessageDialog(gameFrame, "Black Time Out!");
	                	}
	                }
                    blackTimerLabel.setText(getFormattedTime(blackTime));
                    
                }
            });
            
            whiteTimerPanel.setBackground(Color.decode("#DCDCDC"));
            whiteTimerPanel.setBounds(WPB[0], WPB[1], WPB[2], WPB[3]);
            whiteTimerLabel.setFont(new Font("serif", Font.PLAIN, 25));
            whiteTimerLabel.setBounds(WLB[0], WLB[1], WLB[2], WLB[3]);
            
            blackTimerPanel.setBackground(Color.decode("#5F656C"));
            blackTimerPanel.setBounds(BPB[0], BPB[1], BPB[2], BPB[3]);
            blackTimerLabel.setForeground(Color.decode("#ffffff"));
            blackTimerLabel.setFont(new Font("serif", Font.PLAIN, 25));
            blackTimerLabel.setBounds(BLB[0], BLB[1], BLB[2], BLB[3]);
            
            gameFrame.add(whiteTimerLabel);
            gameFrame.add(whiteTimerPanel);
            gameFrame.add(blackTimerLabel);
            gameFrame.add(blackTimerPanel);
            
            whiteTimer.start();
            blackTimer.start();
    	}
    	
    	public static void normal() {
    		WPB[0]=395;WPB[1]=6;WPB[2]=70;WPB[3]=30;
    		BPB[0]=60;BPB[1]=453;BPB[2]=70;BPB[3]=30;
    		WLB[0]=400;WLB[1]=-30;WLB[2]=100;WLB[3]=100;
    		BLB[0]=65;BLB[1]=417;BLB[2]=100;BLB[3]=100;
    	}
    	
    	public static void flip() {
    		BPB[0]=395;BPB[1]=6;BPB[2]=70;BPB[3]=30;
    		WPB[0]=60;WPB[1]=453;WPB[2]=70;WPB[3]=30;
    		BLB[0]=400;BLB[1]=-30;BLB[2]=100;BLB[3]=100;
    		WLB[0]=65;WLB[1]=417;WLB[2]=100;WLB[3]=100;
    	}
    	
    	public static void newGame() {
    		whiteTime = TIMER_DURATION;
    		blackTime = TIMER_DURATION;
    		timeGoing = 1;
    	}
    	
    	public static void startWhiteTimer() {
    		timeGoing = 1;
    	}
    	
    	public static void startBlackTimer() {
    		timeGoing = 0;
    	}
        
        private String getFormattedTime(int time) {
            int minutes = time / 60;
            int seconds = time % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
	}
    
    private class BoardPanel extends JPanel {
    	
    	private final List<TilePanel> boardTiles;

        BoardPanel() {
            super(new GridLayout(8,8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            setBorder(BorderFactory.createEmptyBorder(33, 10, 33, 10));
            setBackground(Color.decode("#000000"));
            validate();
        }

        void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel boardTile : boardDirection.traverse(boardTiles)) {
                boardTile.drawTile(board);
                add(boardTile);
            }
            validate();
            repaint();
        }

        void setTileDarkColor(final Board board,
                              final Color darkColor) {
            for (final TilePanel boardTile : boardTiles) {
                boardTile.setDarkTileColor(darkColor);
            }
            drawBoard(board);
        }

        void setTileLightColor(final Board board,
                                      final Color lightColor) {
            for (final TilePanel boardTile : boardTiles) {
                boardTile.setLightTileColor(lightColor);
            }
            drawBoard(board);
        }
    }

    enum BoardDirection {
        NORMAL {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return boardTiles;
            }

            @Override
            BoardDirection opposite() {
                return FLIPPED;
            }
        },
        FLIPPED {
            @Override
            List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                return Lists.reverse(boardTiles);
            }

            @Override
            BoardDirection opposite() {
                return NORMAL;
            }
        };

        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);
        abstract BoardDirection opposite();
    }

    public static class MoveLog {

        private final List<Move> moves;

        MoveLog() {
            this.moves = new ArrayList<>();
        }

        public List<Move> getMoves() {
            return this.moves;
        }

        void addMove(final Move move) {
            this.moves.add(move);
        }

        public int size() {
            return this.moves.size();
        }

        void clear() {
            this.moves.clear();
        }

        Move removeMove(final int index) {
            return this.moves.remove(index);
        }

        boolean removeMove(final Move move) {
            return this.moves.remove(move);
        }

    }

    private class TilePanel extends JPanel {

        private final int tileId;

        TilePanel(final BoardPanel boardPanel,
                  final int tileId) {
            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            assignTileColor();
            assignTilePieceIcon(chessBoard);
            highlightTileBorder(chessBoard);
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent event) {
                    if (isRightMouseButton(event)) {
                        sourceTile = null;
                        humanMovedPiece = null;
                    } else if (isLeftMouseButton(event)) {
                        if (sourceTile == null) {
                            sourceTile = chessBoard.getPiece(tileId);
                            humanMovedPiece = sourceTile;
                            if (humanMovedPiece == null) {
                                sourceTile = null;
                            }
                        } else {
                            final Move move = MoveFactory.createMove(chessBoard, sourceTile.getPiecePosition(),
                                    tileId);
                            final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                            if (transition.getMoveStatus().isDone()) {
                                chessBoard = transition.getToBoard();
                                moveLog.addMove(move);
                                if(chessBoard.currentPlayer().getAlliance().toString().equals("White")) {
                                	ChessTimer.startWhiteTimer();
                                } else if(chessBoard.currentPlayer().getAlliance().toString().equals("Black")) {
                                	ChessTimer.startBlackTimer();
                                }else {
                                	System.out.println(chessBoard.currentPlayer().getAlliance());
                                }
                            }
                            sourceTile = null;
                            humanMovedPiece = null;
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        gameHistoryPanel.redo(chessBoard, moveLog);
                        takenPiecesPanel.redo(moveLog);
                        boardPanel.drawBoard(chessBoard);
                    });
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                }

                @Override
                public void mouseReleased(final MouseEvent e) {
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                }
            });
            validate();
        }

        void drawTile(final Board board) {
            assignTileColor();
            assignTilePieceIcon(board);
            highlightTileBorder(board);
            highlightLegals(board);
            validate();
            repaint();
        }

        void setLightTileColor(final Color color) {
            lightTileColor = color;
        }

        void setDarkTileColor(final Color color) {
            darkTileColor = color;
        }

        private void highlightTileBorder(final Board board) {
            if(humanMovedPiece != null &&
               humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance() &&
               humanMovedPiece.getPiecePosition() == this.tileId) {
                setBorder(BorderFactory.createLineBorder(Color.cyan));
            } else {
                setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
        }

        private void highlightLegals(final Board board) {
            if (Table.get().getHighlightLegalMoves()) {
                for (final Move move : pieceLegalMoves(board)) {
                    if (move.getDestinationCoordinate() == this.tileId) {
                        try {
                            add(new JLabel(new ImageIcon(ImageIO.read(new File("src/image/misc/green_dot.png")))));
                        }
                        catch (final IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private Collection<Move> pieceLegalMoves(final Board board) {
            if(humanMovedPiece != null && humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance()) {
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }

        private void assignTilePieceIcon(final Board board) {
            this.removeAll();
            if(board.getPiece(this.tileId) != null) {
                try{
                    final BufferedImage image = ImageIO.read(new File(pieceIconPath +
                            board.getPiece(this.tileId).getPieceAllegiance().toString().substring(0, 1) + "" +
                            board.getPiece(this.tileId).toString() +
                            ".gif"));
                    add(new JLabel(new ImageIcon(image)));
                } catch(final IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void assignTileColor() {
            if (BoardUtils.INSTANCE.FIRST_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.THIRD_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.FIFTH_ROW.get(this.tileId) ||
                BoardUtils.INSTANCE.SEVENTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            } else if(BoardUtils.INSTANCE.SECOND_ROW.get(this.tileId) ||
                      BoardUtils.INSTANCE.FOURTH_ROW.get(this.tileId) ||
                      BoardUtils.INSTANCE.SIXTH_ROW.get(this.tileId)  ||
                      BoardUtils.INSTANCE.EIGHTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
            }
        }
    }
}

