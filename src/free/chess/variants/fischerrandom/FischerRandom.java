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

package free.chess.variants.fischerrandom;

import java.io.*;
import free.chess.*;
import java.util.StringTokenizer;
import free.util.IOUtilities;


/**
 * Implements the Fischer Random wild variant. Quoting an ICC help file:
 * <PRE>
 * The usual set of pieces is arranged randomly
 * on the first and eighth ranks, with bishops on opposite colors, and the
 * king between the two rooks, and Black's arrangement a mirror of White's.
 * Castling O-O puts the king at g1 (g8 for Black), the rook at f1 (f8).
 * Castling O-O-O puts the king at c1 (c8), rook at d1 (d8). 
 * </PRE>
 * More information is available at
 * <A HREF="http://www.chessclub.com/help/Fischer-random">http://www.chessclub.com/help/Fischer-random</A>
 */

public class FischerRandom extends ChesslikeGenericVariant{



  /**
   * The sole instance of this class.
   */

  private static final FischerRandom instance = new FischerRandom();



  /**
   * Returns an instance of this class.
   */

  public static FischerRandom getInstance(){
    return instance;
  }



  /**
   * Creates an instance of FischerRandom.
   */

  private FischerRandom(){
    super("----------------------------------------------------------------", "Fischer Random");
  }




  /**
   * Returns <code>true</code> if the move defined by the given arguments is a 
   * short castling move according to the rules of the "fischer random" variant.
   * Returns <code>false</code> otherwise. The result for an illegal move is
   * undefined, but it should throw no exceptions.
   */

  public boolean isShortCastling(Position pos, Square startingSquare,
      Square endingSquare, ChessPiece promotionTarget){

    if (promotionTarget!=null)
      return false;

    ChessPiece movingPiece = (ChessPiece)pos.getPieceAt(startingSquare);
    ChessPiece takenPiece = (ChessPiece)pos.getPieceAt(endingSquare);

    if (movingPiece == ChessPiece.WHITE_KING){
      if (startingSquare.getRank()!=0)
        return false;
      else if ((takenPiece!=null)&&(takenPiece!=ChessPiece.WHITE_ROOK))
        return false;
      else if (!endingSquare.equals("g1"))
        return false;
      else if (startingSquare.equals("f1"))
        return false; // This *could* be a castling, but there is currently 
                      // no way to indicate whether it is one.

      int rank = startingSquare.getRank();
      int file = startingSquare.getFile()+1;
      while (file<=7){
        Piece piece = pos.getPieceAt(file, rank);
        if (piece != null){
          if (piece == ChessPiece.WHITE_ROOK){
            if ((takenPiece == ChessPiece.WHITE_ROOK)&&(!endingSquare.equals(file, rank)))
              return false;
            else
              return true;
          }
          else
            return false;
        }
        file++;
      }
    }
    else if (movingPiece == ChessPiece.BLACK_KING){
      if (startingSquare.getRank()!=7)
        return false;
      else if ((takenPiece!=null)&&(takenPiece!=ChessPiece.BLACK_ROOK))
        return false;
      else if (!endingSquare.equals("g8"))
        return false;
      else if (startingSquare.equals("f8"))
        return false; // This *could* be a castling, but there is currently 
                      // no way to indicate whether it is one.

      int rank = startingSquare.getRank();
      int file = startingSquare.getFile()+1;
      while (file<=7){
        Piece piece = pos.getPieceAt(file, rank);
        if (piece != null){
          if (piece == ChessPiece.BLACK_ROOK){
            if ((takenPiece == ChessPiece.BLACK_ROOK)&&(!endingSquare.equals(file, rank)))
              return false;
            else
              return true;
          }
          else
            return false;
        }
        file++;
      }
    }

    return false;
  }




  /**
   * Returns <code>true</code> if the move defined by the given arguments is a 
   * long castling move. Returns <code>false</code> otherwise. The result for
   * an illegal move is undefined, but it should throw no exceptions.
   */

  public boolean isLongCastling(Position pos, Square startingSquare,
      Square endingSquare, ChessPiece promotionTarget){

    if (promotionTarget!=null)
      return false;

    ChessPiece movingPiece = (ChessPiece)pos.getPieceAt(startingSquare);
    ChessPiece takenPiece = (ChessPiece)pos.getPieceAt(endingSquare);

    if (movingPiece == ChessPiece.WHITE_KING){
      if (startingSquare.getRank()!=0)
        return false;
      else if ((takenPiece!=null)&&(takenPiece!=ChessPiece.WHITE_ROOK))
        return false;
      else if (!endingSquare.equals("c1"))
        return false;
      else if (startingSquare.equals("b1")||startingSquare.equals("d1"))
        return false; // This *could* be a castling, but there is currently 
                      // no way to indicate whether it is one.

      int rank = startingSquare.getRank();
      int file = startingSquare.getFile()-1;
      while (file>=0){
        Piece piece = pos.getPieceAt(file, rank);
        if (piece != null){
          if (piece == ChessPiece.WHITE_ROOK){
            if ((takenPiece == ChessPiece.WHITE_ROOK)&&(!endingSquare.equals(file, rank)))
              return false;
            else
              return true;
          }
          else
            return false;
        }
        file--;
      }
    }
    else if (movingPiece == ChessPiece.BLACK_KING){
      if (startingSquare.getRank()!=7)
        return false;
      else if ((takenPiece!=null)&&(takenPiece!=ChessPiece.BLACK_ROOK))
        return false;
      else if (!endingSquare.equals("c8"))
        return false;
      else if (startingSquare.equals("b8")||startingSquare.equals("d8"))
        return false; // This *could* be a castling, but there is currently 
                      // no way to indicate whether it is one.

      int rank = startingSquare.getRank();
      int file = startingSquare.getFile()-1;
      while (file>=0){
        Piece piece = pos.getPieceAt(file, rank);
        if (piece != null){
          if (piece == ChessPiece.BLACK_ROOK){
            if ((takenPiece == ChessPiece.BLACK_ROOK)&&(!endingSquare.equals(file, rank)))
              return false;
            else
              return true;
          }
          else
            return false;
        }
        file--;
      }
    }

    return false;
  }




  /**
   * Initializes the given position to a random state subject to the constraints
   * specified in the rules.
   *
   * @throws IllegalArgumentException If the given Position's wild variant is
   * not FischerRandom.
   */

  public void init(Position pos){
    checkPosition(pos);

    pos.setLexigraphic(createRandomInitialLexigraphic());
  }




  /**
   * Creates a random initial position subject to the constraints specified in
   * the rules of Fischer Random. The position is encoded and returned in
   * lexigraphic format.
   */

  private static String createRandomInitialLexigraphic(){
    StringBuffer base = new StringBuffer("--------pppppppp--------------------------------PPPPPPPP--------");

    int bishop1Pos, bishop2Pos;
    while(true){
      int pos = randomInt(8);
      if ((pos%2==0)&&(base.charAt(pos)=='-')){
        bishop1Pos = pos;
        break;
      }
    } 
    while(true){
      int pos = randomInt(8);
      if ((pos%2==1)&&(base.charAt(pos)=='-')){
        bishop2Pos = pos;
        break;
      }
    } 
    base.setCharAt(bishop1Pos, 'b');
    base.setCharAt(bishop2Pos, 'b');


    int knight1Pos;
    while (true){
      int pos = randomInt(8);
      if (base.charAt(pos)=='-'){
        knight1Pos = pos;
        break;
      }
    }
    base.setCharAt(knight1Pos, 'n');

    int knight2Pos;
    while (true){
      int pos = randomInt(8);
      if (base.charAt(pos)=='-'){
        knight2Pos = pos;
        break;
      }
    }
    base.setCharAt(knight2Pos, 'n');

    int queenPos;
    while (true){
      int pos = randomInt(8);
      if (base.charAt(pos)=='-'){
        queenPos = pos;
        break;
      }
    }
    base.setCharAt(queenPos, 'q');


    int pos = 0;
    while (pos<6){
      if (base.charAt(pos)=='-')
        break;
      pos++;
    }
    int rook1Pos = pos++;
    base.setCharAt(rook1Pos, 'r');

    while (pos<7){
      if (base.charAt(pos)=='-')
        break;
      pos++;
    }
    int kingPos = pos++;
    base.setCharAt(kingPos, 'k');

    while (pos<8){
      if (base.charAt(pos)=='-')
        break;
      pos++;
    }
    int rook2Pos = pos;
    base.setCharAt(rook2Pos, 'r');


    base.setCharAt(56+kingPos, 'K');
    base.setCharAt(56+queenPos, 'Q');
    base.setCharAt(56+rook1Pos, 'R');
    base.setCharAt(56+rook2Pos, 'R');
    base.setCharAt(56+bishop1Pos, 'B');
    base.setCharAt(56+bishop2Pos, 'B');
    base.setCharAt(56+knight1Pos, 'N');
    base.setCharAt(56+knight2Pos, 'N');

    return base.toString();
  }




  /**
   * Returns a random int, in the range [0..max).  This method is used
   * when creating the initial, random position.
   */

  private static int randomInt(int max){
    return (int)(Math.random()*max);
  }




  /**
   * Makes the given ChessMove on the given position.
   */

  public void makeMove(Move move, Position pos, Position.Modifier modifier){
    checkPosition(pos);

    if (!(move instanceof ChessMove))
      throw new IllegalArgumentException("The given move must be an instance of "+ChessMove.class.getName());

    ChessMove cmove = (ChessMove)move;
    Square startingSquare = cmove.getStartingSquare();
    Square endingSquare = cmove.getEndingSquare();
    ChessPiece movingPiece = (ChessPiece)pos.getPieceAt(startingSquare);

    if (cmove.isCastling()){
      int dir = cmove.isShortCastling() ? 1 : -1;
      int file = startingSquare.getFile() + dir;
      int rank = startingSquare.getRank();

      while ((file>=0)&&(file<=7)){
        ChessPiece piece = (ChessPiece)pos.getPieceAt(file, rank);
        if (piece!=null){
          if (!piece.isRook() || !piece.isSameColorAs(movingPiece))
            throw new IllegalArgumentException("The given move may not be a castling move");
          else
            break;
        }
        file--;
      }

      int rookStartFile = file;
      int rookEndFile = cmove.isShortCastling() ? 2 : 4;

      Square rookStartingSquare = Square.getInstance(rookStartFile, startingSquare.getRank());
      Square rookEndingSquare = Square.getInstance(rookEndFile, startingSquare.getRank());
      ChessPiece rook = (ChessPiece)pos.getPieceAt(rookStartingSquare);

      modifier.setPieceAt(null, startingSquare);
      modifier.setPieceAt(null, rookStartingSquare);

      modifier.setPieceAt(movingPiece, endingSquare);
      modifier.setPieceAt(rook, rookEndingSquare);
    }
    else
      super.makeMove(move, pos, modifier);
  }

}
