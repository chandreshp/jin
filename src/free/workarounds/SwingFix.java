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

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.*;
import java.awt.*;

/**
 * A class which statically applies various fixes/workarounds for swing. To use
 * this, simply include Class.forName("free.workarounds.SwingFix") somewhere
 * in your application.
 */

public class SwingFix{


  /**
   * Adds key bindings for ctrl+c, ctrl+x, ctrl+v, and COPY, CUT and PASTE keys
   * on Sun keyboards which are missing from the Motif Look and Feel.
   * <A HREF="http://developer.java.sun.com/developer/bugParade/bugs/4106281.html">
   * http://developer.java.sun.com/developer/bugParade/bugs/4106281.html</A>
   */

  static{
    Keymap defaultKeyMap = JTextComponent.getKeymap(JTextComponent.DEFAULT_KEYMAP);
    JTextComponent.KeyBinding CUT = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(0xFFD1, 0),DefaultEditorKit.cutAction);
    JTextComponent.KeyBinding COPY = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(0xFFCD, 0),DefaultEditorKit.copyAction);
    JTextComponent.KeyBinding PASTE = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(0xFFCF, 0),DefaultEditorKit.pasteAction);
    JTextComponent.KeyBinding ctrl_x = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.CTRL_MASK),DefaultEditorKit.cutAction);
    JTextComponent.KeyBinding ctrl_c = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK),DefaultEditorKit.copyAction);
    JTextComponent.KeyBinding ctrl_v = new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK),DefaultEditorKit.pasteAction);

    JTextComponent.KeyBinding[] extraBindings = new JTextComponent.KeyBinding[]{CUT,COPY,PASTE,ctrl_x,ctrl_c,ctrl_v};
    JTextComponent tempC = new JTextField();
    JTextComponent.loadKeymap(defaultKeyMap, extraBindings, tempC.getActions());
  }





  /**
   * Sets some acceptable margins on JTextField and JPasswordField because
   * they're 0,0,0,0 by default.
   */

  static{
    UIManager.put("TextField.margin", new Insets(0,2,0,2));
    UIManager.put("PasswordField.margin", new Insets(0,2,0,2));
  }


  
  /**
   * Fix the color of the scrollbar track to something different than the
   * default panel color - otherwise it's invisible sometimes.
   * This changes the color on both Motif and Windows, breaking Motif, but
   * fixing Windows, so will change it only if we're on windows.
   */

  static{
    String osName = System.getProperty("os.name");
    if (osName.indexOf("Windows") != -1){
      UIManager.put("ScrollBar.track", new Color(230,230,230));
    }
  }

}