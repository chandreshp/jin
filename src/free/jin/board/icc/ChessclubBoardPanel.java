/**
 * Jin - a chess client for internet chess servers.
 * More information is available at http://www.jinchess.com/.
 * Copyright (C) 2002, 2003 Alexander Maryanovsky.
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

package free.jin.board.icc;

import javax.swing.*;
import free.jin.event.*;
import java.awt.Color;
import free.jin.board.BoardPanel;
import free.jin.board.BoardManager;
import free.jin.board.JinBoard;
import free.jin.Game;
import free.chess.Player;
import free.chess.Square;
import free.jin.chessclub.event.ChessclubGameListener;
import free.jin.chessclub.event.ArrowEvent;
import free.jin.chessclub.event.CircleEvent;
import free.jin.board.event.ArrowCircleListener;


/**
 * Extends BoardPanel to provide chessclub.com specific functionalities.
 */

public class ChessclubBoardPanel extends BoardPanel implements ChessclubGameListener,
    ArrowCircleListener{



  /**
   * We set this to true when we're handling a circle/arrow adding event from
   * the server to avoid responding to the board's event when we add the
   * circle/arrow to it.
   */

  private boolean handlingArrowCircleEvent = false;



  /**
   * Creates a new <code>ChessclubBoardPanel</code> with the given
   * <code>BoardManager</code> and <code>Game</code>.
   */

  public ChessclubBoardPanel(BoardManager boardManager, Game game){
    super(boardManager, game);
  }




  /**
   * Overrides createBoard(Game game) to return an instance of ChessclubJBoard.
   */

  protected JinBoard createBoard(Game game){
    return new ChessclubJBoard(game.getInitialPosition());
  }




  /**
   * Override configureBoard(Game, JinBoard) to add ourselves as an
   * ArrowCircleListener to the board.
   */

  protected void configureBoard(Game game, JinBoard board){
    super.configureBoard(game, board);

    if ((game.getGameType() == Game.MY_GAME) && !game.isPlayed()){
      board.addArrowCircleListener(this);
      board.setArrowCircleEnabled(true);
    }
    else
      ((ChessclubJBoard)board).setArrowCircleEnabled(false);
  }




  /**
   * Overrides moveMade(MoveMadeEvent) to clear the board of any arrows/circles.
   */

  public void moveMade(MoveMadeEvent evt){
    super.moveMade(evt);

    if (evt.getGame() != game)
      return;

    ((ChessclubJBoard)board).removeAllArrows();
    ((ChessclubJBoard)board).removeAllCircles();
  }




  /**
   * Overrides positionChanged(PositionChangedEvent) to clear the board of any
   * arrows/circles.
   */

  public void positionChanged(PositionChangedEvent evt){
    super.positionChanged(evt);

    if (evt.getGame() != game)
      return;

    ((ChessclubJBoard)board).removeAllArrows();
    ((ChessclubJBoard)board).removeAllCircles();
  }





  /**
   * Overrides takebackOccurred(TakebackEvent) to clear the board of any
   * arrows/circles.
   */

  public void takebackOccurred(TakebackEvent evt){
    super.takebackOccurred(evt);

    if (evt.getGame() != game)
      return;

    ((ChessclubJBoard)board).removeAllArrows();
    ((ChessclubJBoard)board).removeAllCircles();
  }





  /**
   * Overrides illegalMoveAttempted(IllegalMoveEvent) to clear the board of any
   * arrows/circles.
   */

  public void illegalMoveAttempted(IllegalMoveEvent evt){
    super.illegalMoveAttempted(evt);

    if (evt.getGame() != game)
      return;

    ((ChessclubJBoard)board).removeAllArrows();
    ((ChessclubJBoard)board).removeAllCircles();
  }



  /**
   * Overrides BoardPanel.createWhiteLabelText(Game) to return a chessclub.com
   * specific version.
   */

  protected String createWhiteLabelText(Game game){
    int rating = game.getWhiteRating();
    String ratingString = (rating > 0) ? (" "+rating) : "";
    return game.getWhiteName() + game.getWhiteTitles() + ratingString;
  }




  /**
   * Overrides BoardPanel.createBlackLabel(Game) to return a chessclub.com
   * specific version.
   */

  protected String createBlackLabelText(Game game){
    int rating = game.getBlackRating();
    String ratingString = (rating > 0) ? (" "+rating) : "";
    return game.getBlackName() + game.getBlackTitles() + ratingString;
  }




  /**
   * Overrides BoardPanel.createGameLabelText(Game) to return a chessclub.com
   * specific version.
   */

  protected String createGameLabelText(Game game){
    free.chess.WildVariant variant = game.getVariant();
    String category = variant.equals(free.chess.Chess.getInstance()) ?
      game.getRatingCategoryString() : variant.getName();
    return (game.isRated() ? "Rated" : "Unrated") + " " + game.getTCString()+ " " + category;
  }




  /**
   * Gets called when an arrow is added to the board (by the server).
   */

  public void arrowAdded(ArrowEvent evt){
    if (evt.getGame() != game)
      return;

    handlingArrowCircleEvent = true;
    ((ChessclubJBoard)board).removeArrow(evt.getFromSquare(), evt.getToSquare());
    ((ChessclubJBoard)board).addArrow(evt.getFromSquare(), evt.getToSquare(), Color.blue);
    handlingArrowCircleEvent = false;
  }




  /**
   * Gets called when a circle is added to the board (by the server).
   */

  public void circleAdded(CircleEvent evt){
    if (evt.getGame() != game)
      return;

    handlingArrowCircleEvent = true;
    ((ChessclubJBoard)board).removeCircle(evt.getCircleSquare());
    ((ChessclubJBoard)board).addCircle(evt.getCircleSquare(), Color.blue);
    handlingArrowCircleEvent = false;
  }




  /**
   * Gets called when an arrow is added on the board (on the client, not server). 
   */

  public void arrowAdded(JinBoard board, Square fromSquare, Square toSquare){
    if (handlingArrowCircleEvent)
      return;

    boardManager.getConn().sendCommand("arrow " + fromSquare + " " + toSquare);
  }



  /**
   * Gets called when an arrow is removed on the board (on the client, not the
   * server.
   */

  public void arrowRemoved(JinBoard board, Square fromSquare, Square toSquare){}




  /**
   * Gets called when a circle is added (on the client, not the server).
   */

  public void circleAdded(JinBoard board, Square circleSquare){
    if (handlingArrowCircleEvent)
      return;

    boardManager.getConn().sendCommand("circle " + circleSquare);
  }



  /**
   * Gets called when a circle is removed (on the client, not the server).
   */

  public void circleRemoved(JinBoard board, Square circleSquare){}



}
