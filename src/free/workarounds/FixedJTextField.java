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

package free.workarounds;

import javax.swing.JTextField;
import javax.swing.text.Document;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;


/**
 * A fix of JTextField. Fixes the following bugs:
 * <UL>
 *   <LI> <A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4262163.html">
 *        Ibeam cursor not appearing on TextField or TextArea in editmode</A>.
 *   <LI> <A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4133908.html">
 *        <Enter> in JTextComponent should activate DefaultButton</A>.
 *   <LI> <A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4145324.html">
 *        JTextField displays multiple Line</A>.
 * </UL>
 */

public class FixedJTextField extends JTextField{
  

  public FixedJTextField(){
    super();
  }


  public FixedJTextField(Document doc, String text, int columns){
    super(doc, text, columns);
  }


  public FixedJTextField(String text){
    super(text);
  }


  public FixedJTextField(int columns){
    super(columns);
  }


  public FixedJTextField(String text, int columns){
    super(text, columns);
  }



  public void setEditable(boolean editable){
    super.setEditable(editable);

    // http://developer.java.sun.com/developer/bugParade/bugs/4262163.html 

    Cursor cursor = getCursor();
    if (isEditable()&&((cursor==null)||(cursor.getType()!=Cursor.TEXT_CURSOR)))
      setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    else if (!isEditable()&&((cursor==null)||(cursor.getType()!=Cursor.TEXT_CURSOR)))
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    
    // http://developer.java.sun.com/developer/bugParade/bugs/4262163.html 

  }


  protected void processComponentKeyEvent(KeyEvent evt){
    
    // http://developer.java.sun.com/developer/bugParade/bugs/4133908.html

    if ((evt.getKeyCode()==KeyEvent.VK_ENTER)&&(listenerList.getListenerCount(ActionListener.class)==0))
      return;

      // Removes AWT compatibility regarding pressing ENTER generating an 
      // ActionEvent if there are no registered listeners.
    

    super.processComponentKeyEvent(evt);

    // http://developer.java.sun.com/developer/bugParade/bugs/4133908.html

  }



  public void paste(){

    // http://developer.java.sun.com/developer/bugParade/bugs/4145324.html

    super.paste();
    String text = getText();
    int firstCR = text.indexOf('\n');
    int firstLF = text.indexOf('\r');
    int first = firstCR==-1 ? firstLF : (firstLF==-1 ? firstCR : (firstCR<firstLF ? firstCR : firstLF));

    if (first!=-1)
      setText(text.substring(0,first));

    // http://developer.java.sun.com/developer/bugParade/bugs/4145324.html

  }

}