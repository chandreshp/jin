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

package free.chess;


/**
 * This is the superclass of move implementations for various wild chess variants.
 */

public abstract class Move{


  /**
   * The square on which the moving piece was standing before the move.
   */

  protected final Square startingSquare;



  /**
   * The square to which the moving piece moved.
   */

  protected final Square endingSquare;



  /**
   * The player making the move.
   */

  protected final Player player;



  /**
   * The string representation of this move. This may be null.
   */

  private final String stringRepresentation;




  /**
   * Creates a new Move from the given starting Square to the given ending
   * Square, with the given moving Player. The given stringRepresentation will
   * be the string returned from the toString() method. It may be null, in which
   * case a string constructed from the move data will be returned by
   * toString().
   */

  public Move(Square startingSquare, Square endingSquare, Player player, String stringRepresentation){
    this.startingSquare = startingSquare;
    this.endingSquare = endingSquare;
    this.player = player;
    this.stringRepresentation = stringRepresentation;
  }




  /**
   * Returns the initial square of the moving piece.
   */

  public Square getStartingSquare(){
    return startingSquare;
  }


 
  /**
   * Returns the square to which the moving piece moved.
   */

  public Square getEndingSquare(){
    return endingSquare;
  }



  /**
   * Returns the Player making the move.
   */

  public Player getPlayer(){
    return player;
  }



  /**
   * Returns the string representation of the move passed in the constructor.
   * This may be null.
   */

  public final String getStringRepresentation(){
    return stringRepresentation;
  }



  /**
   * Returns a string representation of the move based on the move data.
   */

  public String getMoveString(){
    return getStartingSquare().toString()+getEndingSquare().toString();
  }



  /**
   * Returns the string representation of this Move. If the value returned by
   * {@link #getStringRepresentation()} is not null, that is returned. Otherwise
   * the value of getMoveString() is returned.
   */

  public String toString(){
    if (getStringRepresentation()==null)
      return getMoveString();
    else
      return getStringRepresentation();
  }

}