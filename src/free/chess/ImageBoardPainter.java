/**
 * The chess framework library.
 * More information is available at http://www.jinchess.com/.
 * Copyright (C) 2002 Alexander Maryanovsky.
 * All rights reserved.
 *
 * The chess framework library is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * The chess framework library is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the chess framework library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package free.chess;

import java.awt.*;
import free.util.ImageUtilities;


/**
 * An implementation of BoardPainter which uses an Image, or two images (one for
 * light squares and one for dark) to draw the board.
 */

public class ImageBoardPainter implements BoardPainter{


  /**
   * The image of the entire board. This may be null if we're using two square
   * images.
   */

  private final Image boardImage;



  /**
   * The image scaled to the size given in the last call to scaleImages.
   */

  private Image scaled = null;




  /**
   * The image of the light square. This may be null if we're using one big
   * image for the entire board.
   */

  private final Image lightImage;




  /**
   * The image of the dark square. This may be null if we're using one big
   * image for the entire board.
   */

  private final Image darkImage;




  /**
   * The scaled version of lightImage.
   */

  private Image scaledLight = null;




  /**
   * The scaled version of darkImage.
   */

  private Image scaledDark = null;




  /**
   * True if the light and dark images should be scaled, false if sliced.
   * Only relevant if boardImage is null.
   */

  private final boolean isScaled;





  /**
   * Creates a new ImageBoardPainter which paints the board with the given Image.
   */

  public ImageBoardPainter(Image boardImage){
    this.boardImage = boardImage;
    this.lightImage = null;
    this.darkImage = null;
    isScaled = false;
  }




  /**
   * Creates a new ImageBoardPainter which paints light squares by using the
   * given light image and dark squares by using the given light image.
   * There are two options for using the images. One is having them scaled and
   * fitted into each square - this means that all squares will look the same.
   * For this option, you should provide relatively small images, around 60x60.
   * The second option is to have the images used by pieces - each square will
   * contain a different, unscaled portion of the large given image. For this
   * option you should provide a large image - 200x200 to 300x300 is a good
   * idea. If you don't, all the squares will look about the same. Since in the
   * case the images are too small, the implementation will tile them, the
   * second option can also be used if you have very small images with
   * repeating, tileable patterns. The <code>isScaled</code> argument specified
   * which option is chosen, <code>true</code> for scaling (first option) and
   * <code>false</code> for slicing/tiling.
   */

  public ImageBoardPainter(Image lightImage, Image darkImage, boolean isScaled){
    this.lightImage = lightImage;
    this.darkImage = darkImage;
    this.isScaled = isScaled;
    this.boardImage = null;
  }




  /**
   * Preloads the given image and returns it. This method is here to make it
   * easier to create specific subclasses with no-args constructors and allowing
   * them to preload images.
   */

  protected static Image preload(Image image){
    try{
      ImageUtilities.preload(image);
    } catch (InterruptedException e){}
    return image;
  }




  /**
   * Returns the Image this ImageBoardPainter uses to draw the board, or null
   * if it uses two images (one for light squares and one for dark ones)
   * instead.
   */

  public Image getImage(){
    return boardImage;
  }




  /**
   * Returns the image used for drawing light squares, or null if one big image
   * is used for drawing the whole board.
   */

  public Image getLightImage(){
    return lightImage;
  }




  /**
   * Returns the image used for drawing dark squares, or null if one big image
   * is used for drawing the whole board.
   */

  public Image getDarkImage(){
    return darkImage;
  }




  /**
   * Returns <code>true</code> if the light and dark images are scaled, false
   * if they are sliced/tiled. This is only applicable if we're in the two-image
   * mode.
   */

  public boolean isScaled(){
    return isScaled;
  }




  /**
   * Scales the board image.
   */

  private void scaleImages(int width, int height){
    if (boardImage == null){
      if ((scaledLight != null) && (scaledLight.getWidth(null) == width/8) && (scaledLight.getHeight(null) == height/8))
        return;

      if (isScaled){
        scaledLight = lightImage.getScaledInstance(width/8, height/8, Image.SCALE_FAST);
        scaledDark = darkImage.getScaledInstance(width/8, height/8, Image.SCALE_FAST);
        try{
          ImageUtilities.preload(scaledLight);
          ImageUtilities.preload(scaledDark);
        } catch (InterruptedException e){}
      }
    }
    else{
      if ((scaled != null) && (scaled.getWidth(null) == width) && (scaled.getHeight(null) == height))
        return;

      scaled = boardImage.getScaledInstance(width, height, Image.SCALE_FAST);
      try{
        ImageUtilities.preload(scaled);
      } catch (InterruptedException e){}
    } 
  }



  /**
   * Paints the board at the given location on the given Graphics scaled to
   * the given size.
   */

  public void paintBoard(Graphics g, Component component, int x, int y, int width, int height){
    scaleImages(width, height);

    if (boardImage == null){
      Rectangle clipRect = g.getClipBounds();
      Rectangle drawnRect = new Rectangle(x, y, width/8, height/8);
      int lwidth = lightImage.getWidth(null);
      int lheight = lightImage.getHeight(null);
      int dwidth = darkImage.getWidth(null);
      int dheight = darkImage.getHeight(null);
      for (int file = 0; file < 8; file++, drawnRect.x += width/8){
        drawnRect.y = y;
        for (int rank = 7; rank >= 0; rank--, drawnRect.y += height/8){
          if (!drawnRect.intersects(clipRect))
            continue;
          
          if ((file+rank) % 2 == 0){
            if (isScaled)
              g.drawImage(scaledDark, drawnRect.x, drawnRect.y, component);
            else{
              g.setClip(clipRect.intersection(drawnRect));
              int imgX = Math.max(0, file * (dwidth - drawnRect.width)/7);
              int imgY = Math.max(0, ((7-rank)/2) * (dheight - drawnRect.height)/3);
              for (int offx = drawnRect.x; offx < drawnRect.x+drawnRect.width; offx += dwidth)
                for (int offy = drawnRect.y; offy < drawnRect.y+drawnRect.height; offy += dheight)
                  g.drawImage(darkImage, offx - imgX, offy - imgY, component);
            }
          }
          else{
            if (isScaled)
              g.drawImage(scaledLight, drawnRect.x, drawnRect.y, component);
            else{
              g.setClip(clipRect.intersection(drawnRect));
              int imgX = Math.max(0, file * (lwidth - drawnRect.width)/7);
              int imgY = Math.max(0, ((7-rank)/2) * (lheight - drawnRect.height)/3);
              for (int offx = drawnRect.x; offx < drawnRect.x+drawnRect.width; offx += lwidth)
                for (int offy = drawnRect.y; offy < drawnRect.y+drawnRect.height; offy += lheight)
                  g.drawImage(lightImage, offx - imgX, offy - imgY, component);
            }
          }
        }
      }

      g.setClip(clipRect);
    }
    else{
      g.drawImage(scaled, x, y, component);
    }
  } 

  

}
