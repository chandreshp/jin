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

import javax.swing.JComboBox;
import javax.swing.ComboBoxModel;
import java.awt.*;
import java.util.Vector;


/**
 * A fix of JTextField. Fixes the following bugs:
 * <UL>
 *   <LI> <A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4262163.html">
 *        Ibeam cursor not appearing on JTextComponents in editmode</A>.
 *   <LI> <A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4269430.html">
 *        JComboBox - Should match preferred height of JTextField</A>
 * </UL>
 */

public class FixedJComboBox extends JComboBox{
  

  public FixedJComboBox(){
    super();
    fixBugs();
  }


  public FixedJComboBox(ComboBoxModel model){
    super(model);
    fixBugs();
  }


  public FixedJComboBox(Object [] items){
    super(items);
    fixBugs();
  }


  public FixedJComboBox(Vector items){
    super(items);
    fixBugs();
  }




  /**
   * Fixes various bugs that can be fixed in the constructor. This method is
   * called from all the constructors.
   */

  protected void fixBugs(){

    // http://developer.java.sun.com/developer/bugParade/bugs/4269430.html //

    Dimension maximumSize = getPreferredSize();
    maximumSize.width = Short.MAX_VALUE;
    setMaximumSize(maximumSize);

    // http://developer.java.sun.com/developer/bugParade/bugs/4269430.html //
  }




  public void setEditable(boolean editable){
    super.setEditable(editable);

    // http://developer.java.sun.com/developer/bugParade/bugs/4262163.html //

    Component editorComponent = getEditor().getEditorComponent();
    Cursor cursor = editorComponent.getCursor();
    if (isEditable()&&((cursor==null)||(cursor.getType()!=Cursor.TEXT_CURSOR)))
      editorComponent.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    else if (!isEditable()&&((cursor==null)||(cursor.getType()!=Cursor.TEXT_CURSOR)))
      editorComponent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    
    // http://developer.java.sun.com/developer/bugParade/bugs/4262163.html //

  }

}