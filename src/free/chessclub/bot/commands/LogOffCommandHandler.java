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

package free.chessclub.bot.commands;

import free.chessclub.bot.SimpleCommandHandler;


/**
 * A handler for asking the bot to log off. When issued, the bot will send the
 * command "exit" to the server.
 */

public class LogOffCommandHandler extends SimpleCommandHandler{


  /**
   * Creates a new LogOffCommandHandler with the given list of people who are
   * authorized to ask the bot to log off.
   */

  public LogOffCommandHandler(String [] authorizedPeopleList){
    super("exit", authorizedPeopleList, false);
  }

}
