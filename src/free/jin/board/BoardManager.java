/**
 * Jin - a chess client for internet chess servers.
 * More information is available at http://www.hightemplar.com/jin/.
 * Copyright (C) 2002 Alexander Maryanovsky.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package free.jin.board;

import free.jin.event.*;
import java.awt.*;
import javax.swing.*;
import free.jin.JinConnection;
import free.jin.Game;
import free.jin.User;
import free.jin.plugin.Plugin;
import free.jin.board.event.UserMoveListener;
import free.jin.board.event.UserMoveEvent;
import free.util.StringParser;
import free.util.StringEncoder;
import free.util.GraphicsUtilities;
import free.chess.JBoard;
import free.chess.PiecePainter;
import free.chess.DefaultPiecePainter;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.PropertyChangeEvent;


/**
 * The plugin responsible for displaying boards and handling all things related
 * to that.
 */

public class BoardManager extends Plugin implements GameListener, UserMoveListener, VetoableChangeListener, InternalFrameListener{



  /**
   * A Hashtable mapping Game objects to BoardPanel objects which are currently used.
   */

  protected final Hashtable gamesToBoardPanels = new Hashtable();




  /**
   * A list of JInternalFrames which are still on the screen but contain inactive
   * BoardPanels.
   */

  protected final Vector unusedInternalFrames = new Vector();




  /**
   * A Hashtable mapping JInternalFrame objects to BoardPanels they contain.
   */

  protected final Hashtable internalFramesToBoardPanels = new Hashtable();




  /**
   * A Hashtable mapping BoardPanels to their JInternalFrame containers.
   */

  protected final Hashtable boardPanelsToInternalFrames = new Hashtable();




  /**
   * A list of the JInternalFrames in the order they were created.
   */

  protected final Vector internalFrames = new Vector();




  /**
   * The current move input style.
   */

  protected int moveInputStyle;




  /**
   * The current dragged piece style.
   */

  protected int draggedPieceStyle;




  /**
   * Are we in auto promotion mode?
   */

  protected boolean autoPromote;




  /**
   * The current piece set.
   */

  protected PiecePainter piecePainter;




  /**
   * Starts this plugin.
   */

  public void start(){
    init();
    registerConnListeners();
  }



  /**
   * Stops this plugin.
   */

  public void stop(){
    saveState();
    unregisterConnListeners();
    removeBoards();
  }




  /**
   * Initializes all kinds of variables and other stuff. This is called from the
   * start() method.
   */

  protected void init(){
    moveInputStyle = "click'n'click".equals(getProperty("move-input-style")) ? JBoard.CLICK_N_CLICK : JBoard.DRAG_N_DROP;
    draggedPieceStyle = "target-cursor".equals(getProperty("dragged-piece-style")) ? JBoard.CROSSHAIR_DRAGGED_PIECE : JBoard.NORMAL_DRAGGED_PIECE;
    autoPromote = Boolean.valueOf(getProperty("auto-promote","false")).booleanValue();

    String piecePainterClassName = getProperty("piece-set-class-name");
    if (piecePainterClassName!=null){
      try{
        piecePainter = (PiecePainter)Class.forName(piecePainterClassName).newInstance();
      } catch (ClassNotFoundException e){
          System.err.println("Unable to find class "+piecePainterClassName+", will use the default piece painter.");
        }
        catch (InstantiationException e){
          System.err.println("Unable to instantiate class "+piecePainterClassName+", will use the default piece painter."); 
        }
        catch (IllegalAccessException e){
          System.err.println("Unable to instantiate class "+piecePainterClassName+" due to access restrictions, will use the default piece painter."); 
        }
        catch (ClassCastException e){
          System.err.println("Unable to cast "+piecePainterClassName+" into PiecePainter"); 
        }
    }
    if (piecePainter==null)
      piecePainter = new DefaultPiecePainter();
  }



  /**
   * Registers all the necessary listeners with the Connection.
   */

  protected void registerConnListeners(){
    JinConnection conn = getConnection();

    conn.addGameListener(this);
  }




  /**
   * Creates and returns the JMenu for this plugin.
   */

  public JMenu createPluginMenu(){
    JMenu myMenu = new JMenu(getName());
    
    myMenu.add(createMoveInputMenu());

    JMenu pieceSetsMenu = createPieceSetsMenu();
    if (pieceSetsMenu!=null)
      myMenu.add(pieceSetsMenu);

    return myMenu;
  }




  /**
   * Creates and returns the "Move Input" menu.
   */

  protected JMenu createMoveInputMenu(){
    JMenu moveInputMenu = new JMenu("Move-Input");

    final JCheckBoxMenuItem dragndrop = new JCheckBoxMenuItem("Drag'n'Drop", moveInputStyle==JBoard.DRAG_N_DROP);
    JCheckBoxMenuItem clicknclick = new JCheckBoxMenuItem("Click'n'Click", moveInputStyle==JBoard.CLICK_N_CLICK);
    ButtonGroup inputModeGroup = new ButtonGroup();
    inputModeGroup.add(dragndrop);
    inputModeGroup.add(clicknclick);
    dragndrop.addChangeListener(new ChangeListener(){
      
      public void stateChanged(ChangeEvent evt){
        moveInputStyle = dragndrop.isSelected() ? JBoard.DRAG_N_DROP : JBoard.CLICK_N_CLICK;

        Enumeration boardPanels = boardPanelsToInternalFrames.keys();
        while (boardPanels.hasMoreElements()){
          BoardPanel boardPanel = (BoardPanel)boardPanels.nextElement();
          boardPanel.getBoard().setMoveInputStyle(moveInputStyle);
        }
      }
      
    });


    final JCheckBoxMenuItem normalDraggedPieceStyle = new JCheckBoxMenuItem("Normal Dragged Piece", draggedPieceStyle==JBoard.NORMAL_DRAGGED_PIECE);
    JCheckBoxMenuItem targetDraggedPieceStyle = new JCheckBoxMenuItem("Target Cursor", draggedPieceStyle==JBoard.CROSSHAIR_DRAGGED_PIECE);
    ButtonGroup draggedPieceStyleGroup = new ButtonGroup();
    draggedPieceStyleGroup.add(normalDraggedPieceStyle);
    draggedPieceStyleGroup.add(targetDraggedPieceStyle);
    normalDraggedPieceStyle.addChangeListener(new ChangeListener(){
      
      public void stateChanged(ChangeEvent evt){
        draggedPieceStyle = normalDraggedPieceStyle.isSelected() ? JBoard.NORMAL_DRAGGED_PIECE : JBoard.CROSSHAIR_DRAGGED_PIECE;

        Enumeration boardPanels = boardPanelsToInternalFrames.keys();
        while (boardPanels.hasMoreElements()){
          BoardPanel boardPanel = (BoardPanel)boardPanels.nextElement();
          boardPanel.getBoard().setDraggedPieceStyle(draggedPieceStyle);
        }
      }
      
    });



    final JCheckBoxMenuItem alwaysQueen = new JCheckBoxMenuItem("Auto Queen");
    alwaysQueen.addChangeListener(new ChangeListener(){

      public void stateChanged(ChangeEvent evt){
        autoPromote = alwaysQueen.isSelected();

        Enumeration boardPanels = boardPanelsToInternalFrames.keys();
        while (boardPanels.hasMoreElements()){
          BoardPanel boardPanel = (BoardPanel)boardPanels.nextElement();
          boardPanel.getBoard().setManualPromote(!autoPromote);
        }
      }

    });

    moveInputMenu.add(dragndrop);
    moveInputMenu.add(clicknclick);
    moveInputMenu.addSeparator();
    moveInputMenu.add(normalDraggedPieceStyle);
    moveInputMenu.add(targetDraggedPieceStyle);
    moveInputMenu.addSeparator();
    moveInputMenu.add(alwaysQueen);

    return moveInputMenu;
  }




  /**
   * Creates and returns the piece "Piece Sets" menu. This may return null so
   * that no such menu is displayed. The default implementation will return null
   * if less than 2 piece sets are specified in parameters.
   */

  protected JMenu createPieceSetsMenu(){
    int pieceSetCount = Integer.parseInt(getProperty("piece-set-count", "0"));
    if (pieceSetCount<2)
      return null;

    ActionListener pieceSetListener = new ActionListener(){

      public void actionPerformed(ActionEvent evt){
        AbstractButton button = (AbstractButton)evt.getSource();
        String piecePainterClassName = button.getActionCommand();
        try{
          piecePainter = (PiecePainter)Class.forName(piecePainterClassName).newInstance();

          Enumeration boardPanels = boardPanelsToInternalFrames.keys();
          while (boardPanels.hasMoreElements()){
            BoardPanel boardPanel = (BoardPanel)boardPanels.nextElement();
            boardPanel.getBoard().setPiecePainter(piecePainter);
          }
        } catch (ClassNotFoundException e){
            System.err.println("Unable to find class "+piecePainterClassName);
          }
          catch (InstantiationException e){
            System.err.println("Unable to instantiate class "+piecePainterClassName); 
          }
          catch (IllegalAccessException e){
            System.err.println("Unable to instantiate class "+piecePainterClassName+" due to access restrictions"); 
          }
          catch (ClassCastException e){
            System.err.println("Unable to cast "+piecePainterClassName+" into PiecePainter"); 
          }
      } 
    };

    JMenu pieceSetsMenu = new JMenu("Piece Sets");
    ButtonGroup pieceSetsCheckBoxGroup = new ButtonGroup();
    for (int i=0;i<pieceSetCount;i++){
      String pieceSet = getProperty("piece-set-"+i);
      StringTokenizer tokenizer = new StringTokenizer(pieceSet,";");
      String pieceSetName = tokenizer.nextToken();
      String className = tokenizer.nextToken();
      if (pieceSet==null){
        System.err.println("Piece set with index "+i+" is not specified");
        continue;
      }
      JCheckBoxMenuItem menuCheckBox = new JCheckBoxMenuItem(pieceSetName);
      menuCheckBox.setActionCommand(className);
      if (className.equals(piecePainter.getClass().getName()))
        menuCheckBox.setSelected(true);
      menuCheckBox.addActionListener(pieceSetListener);
      pieceSetsCheckBoxGroup.add(menuCheckBox);
      pieceSetsMenu.add(menuCheckBox);
    }
    
    return pieceSetsMenu;
  }




  /**
   * Saves the current properties of the BoardManager into the user properties.
   */

  protected void saveState(){
    int oldMoveInputStyle = "click'n'click".equals(getProperty("move-input-style")) ? JBoard.CLICK_N_CLICK : JBoard.DRAG_N_DROP;
    if (oldMoveInputStyle!=moveInputStyle)
      setProperty("move-input-style", moveInputStyle==JBoard.CLICK_N_CLICK ? "click'n'click" : "drag'n'drop", true);


    int oldDraggedPieceStyle = "target-cursor".equals(getProperty("dragged-piece-style")) ? JBoard.CROSSHAIR_DRAGGED_PIECE : JBoard.NORMAL_DRAGGED_PIECE;
    if (oldDraggedPieceStyle!=draggedPieceStyle)
      setProperty("dragged-piece-style", draggedPieceStyle==JBoard.CROSSHAIR_DRAGGED_PIECE ? "target-cursor" : "normal-cursor", true);

    boolean oldAutoPromote = Boolean.valueOf(getProperty("auto-promote","false")).booleanValue();
    if (oldAutoPromote!=autoPromote)
      setProperty("auto-promote", String.valueOf(autoPromote), true);

    String oldPieceSetClassName = getProperty("piece-set-class-name", DefaultPiecePainter.class.getName());
    if (!piecePainter.getClass().getName().equals(oldPieceSetClassName))
      setProperty("piece-set-class-name", piecePainter.getClass().getName(), true);
  }




  /**
   * Unregisters all the necessary listeners from the Connection. 
   */

  protected void unregisterConnListeners(){
    JinConnection conn = getConnection();

    conn.removeGameListener(this);
  } 



  /**
   * Removes the JInternalFrames of all the displayed BoardPanels. Also loses
   * references to all the BoardPanels.
   */

  protected void removeBoards(){
    Enumeration games = gamesToBoardPanels.keys();
    while (games.hasMoreElements()){
      Game game = (Game)games.nextElement();
      BoardPanel boardPanel = getBoardPanel(game);
      JInternalFrame boardFrame = (JInternalFrame)boardPanelsToInternalFrames.remove(boardPanel);
      boardFrame.dispose();
      internalFramesToBoardPanels.remove(boardFrame);
    }

    gamesToBoardPanels.clear();

    int numUnusedFrames = unusedInternalFrames.size();
    for (int i=0;i<numUnusedFrames;i++){
      JInternalFrame frame = (JInternalFrame)unusedInternalFrames.elementAt(i);
      frame.dispose();
    }
    unusedInternalFrames.removeAllElements();
  }




  /**
   * Returns the BoardPanel displaying the given Game.
   */

  protected BoardPanel getBoardPanel(Game game){
    return (BoardPanel)gamesToBoardPanels.get(game);
  }





  /**
   * Gets called when a game starts. Creates a new BoardPanel and a JInternalFrame
   * to put it in and displays it.
   */

  public void gameStarted(GameStartEvent evt){
    Game game = evt.getGame();
    BoardPanel boardPanel = createBoardPanel(game);
    boardPanel.getBoard().setMoveInputStyle(moveInputStyle);
    boardPanel.getBoard().setDraggedPieceStyle(draggedPieceStyle);
    boardPanel.getBoard().setManualPromote(!autoPromote);
    boardPanel.getBoard().setPiecePainter(piecePainter);

    boardPanel.addUserMoveListener(this);
    getConnection().addGameListener(boardPanel);
    gamesToBoardPanels.put(game, boardPanel);

    JInternalFrame boardFrame;
    if (unusedInternalFrames.isEmpty()){
      boardFrame = createNewBoardFrame(boardPanel);
    }
    else{
      boardFrame = reuseBoardFrame(boardPanel);
    }

    boardFrame.setTitle(boardPanel.getTitle());
    boardFrame.repaint(); // It doesn't seem to repaint itself automatically.

    internalFramesToBoardPanels.put(boardFrame, boardPanel);
    boardPanelsToInternalFrames.put(boardPanel, boardFrame);
		
    boardFrame.toFront();
    try{
      boardFrame.setSelected(true);
    } catch (java.beans.PropertyVetoException e){} // Ignore.
  }




  /**
   * Creates and configures a new JInternalFrame for use.
   */

  private JInternalFrame createNewBoardFrame(BoardPanel boardPanel){
    JInternalFrame boardFrame = createBoardFrame(boardPanel);

    int index = internalFrames.size();
    for (int i=0;i<internalFrames.size();i++){
      if (internalFrames.elementAt(i)==null){
        index = i;
        break;
      }
    }
    internalFrames.insertElementAt(boardFrame, index);
    boardFrame.addInternalFrameListener(this);

    JDesktopPane desktop = getPluginContext().getMainFrame().getDesktop();
    desktop.add(boardFrame);

    configureBoardFrame(boardFrame, index);

    Container boardFrameContentPane = boardFrame.getContentPane();
    boardFrameContentPane.setLayout(new BorderLayout());
    boardFrameContentPane.add(boardPanel, BorderLayout.CENTER);

    boardFrame.setVisible(true);

    boardFrameContentPane.invalidate();
    boardFrameContentPane.validate();


    /* See http://developer.java.sun.com/developer/bugParade/bugs/4176136.html for the 
       reason I do this instead of adding an InternalFrameListener like a sane person. */
    boardFrame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
    boardFrame.addVetoableChangeListener(this);

    return boardFrame;
  }





  /**
   * Takes a JInternalFrame from the unused board frames list and configures it
   * to be reused.
   */

  private JInternalFrame reuseBoardFrame(BoardPanel boardPanel){
    JInternalFrame boardFrame = (JInternalFrame)unusedInternalFrames.lastElement();
    unusedInternalFrames.removeElementAt(unusedInternalFrames.size()-1);

    Container contentPane = boardFrame.getContentPane();
    contentPane.removeAll();
    contentPane.add(boardPanel, BorderLayout.CENTER);
    contentPane.invalidate();
    contentPane.validate();

    return boardFrame;
  }

  


  /**
   * Creates and configures a new BoardPanel for the given Game.
   */

  protected BoardPanel createBoardPanel(Game game){
    BoardPanel boardPanel = new BoardPanel(this, game);

    return boardPanel;
  }



  /**
   * Creates and configures a JInternalFrame to be used for displaying the given
   * BoardPanel. This method also adds the given BoardPanel to the returned
   * JInternalFrame.
   */

  protected JInternalFrame createBoardFrame(BoardPanel boardPanel){
    JInternalFrame boardFrame = new JInternalFrame();
    boardFrame.setResizable(true);
    boardFrame.setClosable(true);
    boardFrame.setMaximizable(true);
    boardFrame.setIconifiable(true);

    return boardFrame;
  }




  /**
   * Sets the various properties of the given JInternalFrame from the saved
   * properties of the JInternalFrame with the same index. If no JInternalFrame
   * with the given index ever existed, sets those properties to some reasonable
   * defaults.
   */

  private void configureBoardFrame(JInternalFrame boardFrame, int index){
    JDesktopPane desktop = getPluginContext().getMainFrame().getDesktop();

    Rectangle desktopBounds = new Rectangle(desktop.getSize());
    String boundsString = getProperty("frame-bounds-"+index);
    Rectangle bounds = null;
    if (boundsString!=null)
      bounds = StringParser.parseRectangle(boundsString);

    if (bounds==null)
      boardFrame.setBounds(desktopBounds.width/4, 0, desktopBounds.width*3/4, desktopBounds.height*3/4);
    else
      boardFrame.setBounds(bounds);

    boolean isMaximized = Boolean.valueOf(getProperty("maximized-"+index,"false")).booleanValue();
    if (isMaximized){
      try{
        boardFrame.setMaximum(true);
      } catch (java.beans.PropertyVetoException e){}
    }

    boolean isIconified = Boolean.valueOf(getProperty("iconified-"+index,"false")).booleanValue();
    if (isIconified){
      try{
        boardFrame.setIcon(true);
      } catch (java.beans.PropertyVetoException e){}
    }

    JComponent icon = boardFrame.getDesktopIcon();
    String iconBoundsString = getProperty("frame-icon-bounds-"+index);
    if (iconBoundsString!=null){
      Rectangle iconBounds = StringParser.parseRectangle(iconBoundsString);
      icon.setBounds(iconBounds);
    }
  }




  /**
   * GameListener implementation.
   */

  public void moveMade(MoveMadeEvent evt){

  }




  /**
   * GameListener implementation.
   */

  public void positionChanged(PositionChangedEvent evt){

  }




  /**
   * GameListener implementation.
   */

  public void takebackOccurred(TakebackEvent evt){

  }




  /**
   * GameListener implementation.
   */

  public void illegalMoveAttempted(IllegalMoveEvent evt){

  }





  /**
   * GameListener implementation.
   */

  public void clockAdjusted(ClockAdjustmentEvent evt){

  }




  /**
   * GameListener implementation.
   */

  public void boardFlipped(BoardFlipEvent evt){

  }




  /**
   * Gets called when a game ends. Notifies the BoardPanel displaying the board
   * marking it unused.
   */

  public void gameEnded(GameEndEvent evt){
    BoardPanel boardPanel = (BoardPanel)gamesToBoardPanels.remove(evt.getGame());
    if (boardPanel!=null){
      getConnection().removeGameListener(boardPanel);
      boardPanel.removeUserMoveListener(this);
      boardPanel.setInactive();
      JInternalFrame boardFrame = (JInternalFrame)boardPanelsToInternalFrames.remove(boardPanel);
      internalFramesToBoardPanels.remove(boardFrame);
      boardFrame.setTitle(boardPanel.getTitle());
      boardFrame.repaint(); // It doesn't seem to repaint itself.
      if (!boardFrame.isClosed())
        unusedInternalFrames.addElement(boardFrame);
    }
  }





  /**
   * Gets called when the user makes a move on the board.
   */

  public void userMadeMove(UserMoveEvent evt){
    Object src = evt.getSource();

    if (src instanceof BoardPanel){
      BoardPanel boardPanel = (BoardPanel)src;
      Game game = boardPanel.getGame();
      getConnection().makeMove(game, evt.getMove());
    }
  }




  /**
   * VetoableChangeListener implementation. See http://developer.java.sun.com/developer/bugParade/bugs/4176136.html
   * for the reason this is needed.
   */

  public void vetoableChange(PropertyChangeEvent pce) throws PropertyVetoException{
    Object source = pce.getSource();

    if (source instanceof JInternalFrame){
      JInternalFrame boardFrame =  (JInternalFrame)source;
      BoardPanel boardPanel = (BoardPanel)internalFramesToBoardPanels.get(boardFrame);
      if (boardPanel!=null){ // isActive()==true, otherwise, the user is just closing a "dead" frame.
        Game game = ((BoardPanel)internalFramesToBoardPanels.get(boardFrame)).getGame();
        if (pce.getPropertyName().equals(JInternalFrame.IS_CLOSED_PROPERTY)&&
              pce.getOldValue().equals(Boolean.FALSE)&&pce.getNewValue().equals(Boolean.TRUE)){

          int result = JOptionPane.showConfirmDialog(getPluginContext().getMainFrame(),"Are you sure you want to quit this game?","Select an option",JOptionPane.YES_NO_OPTION);
          if (result==JOptionPane.YES_OPTION)
            getConnection().quitGame(game);
          else
            throw new PropertyVetoException("Canceled closing", pce);
        }
      }
    }
  }



  /**
   * InternalFrameListener implementation.
   */

  public void internalFrameActivated(InternalFrameEvent e){

  }



  /**
   * InternalFrameListener implementation.
   */

  public void internalFrameClosed(InternalFrameEvent e){
    JInternalFrame frame = (JInternalFrame)e.getSource();

    int index = -1;
    for (int i=0;i<internalFrames.size();i++){
      if (internalFrames.elementAt(i)==frame){
        index = i;
        break;
      }
    }
    if (index==-1)
      throw new IllegalStateException("No matching frame found");

    User user = getUser();
    String prefix = getID()+".";

    boolean isMaximized = frame.isMaximum();
    user.setProperty(prefix+"maximized-"+index, String.valueOf(isMaximized), false);

    boolean isIconified = frame.isIcon();
    user.setProperty(prefix+"iconified-"+index, String.valueOf(isIconified), false);

    // This is the only way to retrieve the "normal" bounds of the frame under
    // JDK1.2 and earlier. JDK1.3 has a getNormalBounds() method.
    if (isMaximized){
      try{
        frame.setMaximum(false);
      } catch (java.beans.PropertyVetoException ex){}
    }

    Rectangle frameBounds = frame.getBounds();
    // If something bad happened, let's not save that state.
    if ((frameBounds.width>10)&&(frameBounds.height>10))
      user.setProperty(prefix+"frame-bounds-"+index,StringEncoder.encodeRectangle(frameBounds),false);

    Rectangle iconBounds = frame.getDesktopIcon().getBounds();
    user.setProperty(prefix+"frame-icon-bounds-"+index,StringEncoder.encodeRectangle(iconBounds),false);

    internalFrames.setElementAt(null, index);
    frame.removeInternalFrameListener(this);
    unusedInternalFrames.removeElement(frame);
  }



  /**
   * InternalFrameListener implementation.
   */

  public void internalFrameClosing(InternalFrameEvent e){

  }



  /**
   * InternalFrameListener implementation.
   */

  public void internalFrameDeactivated(InternalFrameEvent e){

  }



  /**
   * InternalFrameListener implementation.
   */

  public void internalFrameDeiconified(InternalFrameEvent e){

  }



  /**
   * InternalFrameListener implementation.
   */

  public void internalFrameIconified(InternalFrameEvent e){

  }



  /**
   * InternalFrameListener implementation.
   */

  public void internalFrameOpened(InternalFrameEvent e){

  }

}
