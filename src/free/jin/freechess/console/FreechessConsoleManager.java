/**
 * Jin - a chess client for internet chess servers.
 * More information is available at http://www.jinchess.com/.
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

package free.jin.freechess.console;

import free.jin.console.*;
import free.jin.event.ChatEvent;
import free.jin.console.ChannelConsolePreferencesPanel;
import free.jin.plugin.PreferencesPanel;


/**
 * An extension of the default ConsoleManager for the freechess.org server.
 */

public class FreechessConsoleManager extends ConsoleManager{



  /**
   * Creates a FreechessConsole.
   */

  protected Console createConsole(){
    return new FreechessConsole(this);
  }




  /**
   * Overrides <code>chatMessageReceived(ChatEvent)</code> to notify the
   * <code>Console</code> when tells are received.
   */

  public void chatMessageReceived(ChatEvent evt){
    super.chatMessageReceived(evt);

    String type = evt.getType();
    if (type.equals("tell") || type.equals("say") || type.equals("ptell"))
      console.tellReceived(evt.getSender());
  }



  
  /**
   * Returns the string that should be displayed according to the given
   * ChatEvent.
   */

  protected String translateChat(ChatEvent evt){
    String type = evt.getType();
    String sender = evt.getSender();
    String title = evt.getSenderTitle();
    String message = evt.getMessage();
    Object forum = evt.getForum();

    // Tells
    if (type.equals("tell"))
      return sender+title+" tells you: "+message;
    else if (type.equals("say"))
      return sender+title+" says: "+message;
    else if (type.equals("ptell"))
      return sender+title+" (your partner) tells you: "+message;
    else if (type.equals("qtell"))
      return ":"+message;

    // Channel tells
    else if (type.equals("channel-tell"))
      return sender+title+"("+forum+"): "+message;

    // Kibitzes and whispers
    else if (type.equals("kibitz"))
      return sender+title+"["+forum+"] kibitzes: "+message;
    else if (type.equals("whisper"))
      return sender+title+"["+forum+"] whispers: "+message;

    // Shouts
    else if (type.equals("shout"))
      return sender+title+" shouts: "+message;
    else if (type.equals("ishout"))
      return "--> "+sender+title+" "+message;
    else if (type.equals("tshout"))
      return ":"+sender+title+" t-shouts: "+message;
    else if (type.equals("cshout"))
      return sender+title+" c-shouts: "+message;
    else if (type.equals("announcement"))
      return "    **ANNOUNCEMENT** from "+sender+": "+message; 

    return evt.toString();
  }



  /**
   * Return a PreferencesPanel for changing the console manager's settings.
   */

  public PreferencesPanel getPreferencesUI(){
    return new ChannelConsolePreferencesPanel(this);
  }


}
