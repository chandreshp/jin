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

package free.jin.event;

import free.jin.JinConnection;
import free.jin.SeekJinConnection;
import free.jin.GameListJinConnection;
import free.jin.FriendsJinConnection;
import free.jin.event.SeekJinListenerManager;
import free.jin.event.GameListJinListenerManager;
import free.util.EventListenerList;


/**
 * A basic implementation of JinListenerManager which uses an EventListenerList
 * to hold the listeners. It also provides conventient event firing methods.
 */

public class BasicJinListenerManager implements JinListenerManager, SeekJinListenerManager,
    GameListJinListenerManager, FriendsJinListenerManager{



  /**
   * The JinConnection source of the events.
   */

  protected final JinConnection source;




  /**
   * The EventListenerList where we keep all of our listeners.
   */

  protected final EventListenerList listenerList = new EventListenerList();





  /**
   * Creates a new BasicJinListenerManager with the given source JinConnection.
   */

  public BasicJinListenerManager(JinConnection source){
    this.source = source;
  }




  /**
   * Adds the given ConnectionListener to receive notifications when the connection
   * to the server is established/lost.
   */

  public void addConnectionListener(ConnectionListener listener){
    listenerList.add(ConnectionListener.class, listener);
  }



  /**
   * Removes the given ConnectionListener from the list of listeners receiving
   * notifications when the connection to the server is established/lost.
   */

  public void removeConnectionListener(ConnectionListener listener){
    listenerList.remove(ConnectionListener.class, listener);
  }



  /**
   * Fires the given ConnectionEvent to all interested listeners.
   */

  public void fireConnectionEvent(ConnectionEvent evt){
    int evtID = evt.getID();
    Object [] listeners = listenerList.getListenerList();
    for (int i = 0; i < listeners.length; i += 2){
      if (listeners[i] == ConnectionListener.class){
        ConnectionListener listener = (ConnectionListener)listeners[i+1];
        try{
          switch (evtID){
            case ConnectionEvent.ESTABLISHED:
              listener.connectionEstablished(evt);
              break;
            case ConnectionEvent.LOGGED_IN:
              listener.connectionLoggedIn(evt);
              break;
            case ConnectionEvent.LOST:
              listener.connectionLost(evt);
              break;
            default:
              throw new IllegalArgumentException("Unknown ConnectionEvent id: "+evtID);
          }
        } catch (RuntimeException e){
            e.printStackTrace();
          }
      }
    }
  }



  /**
   * Adds the given PlainTextListener to receive notification when otherwise
   * unidentified text arrives from the server.
   */

  public void addPlainTextListener(PlainTextListener listener){
    listenerList.add(PlainTextListener.class, listener);
  }



  /**
   * Removes the given PlainTextListener from the list of PlainTextListeners
   * receiving notification when otherwise unidentified text arrives from 
   * the server.
   */

  public void removePlainTextListener(PlainTextListener listener){
    listenerList.remove(PlainTextListener.class, listener);
  }




  /**
   * Fires the given PlainTextEvent to all interested listeners.
   */

  public void firePlainTextEvent(PlainTextEvent evt){
    Object [] listeners = listenerList.getListenerList();
    for (int i = 0; i < listeners.length; i += 2){
      if (listeners[i] == PlainTextListener.class){
        PlainTextListener listener = (PlainTextListener)listeners[i+1];
        try{
          listener.plainTextReceived(evt);
        } catch (RuntimeException e){
            e.printStackTrace();
          }
      }
    }
  }





  /**
   * Adds the given ChatListener to receive notification when chat related
   * messages arrive from the server.
   */

  public void addChatListener(ChatListener listener){
    listenerList.add(ChatListener.class, listener);
  }



  /**
   * Removes the given ChatListener from the list of ChatListeners receiving
   * notification when chat related messages arrive from the server.
   */

  public void removeChatListener(ChatListener listener){
    listenerList.remove(ChatListener.class, listener);
  }




  /**
   * Dispatches the given ChatEvent to all interested listeners.
   */

  public void fireChatEvent(ChatEvent evt){
    Object [] listeners = listenerList.getListenerList();
    for (int i = 0; i < listeners.length; i += 2){
      if (listeners[i] == ChatListener.class){
        ChatListener listener = (ChatListener)listeners[i+1];
        try{
          listener.chatMessageReceived(evt);
        } catch (RuntimeException e){
            e.printStackTrace();
          }
      }
    }
  }

 
  
  
  
  /**
   * Adds the given GameListener to the list of listeners receiving notifications
   * of GameEvents.
   */

  public void addGameListener(GameListener listener){
    listenerList.add(GameListener.class, listener);
  }



  /**
   * Removes the given GameListener from the list of listeners receiving notifications
   * of GameEvents.
   */

  public void removeGameListener(GameListener listener){
    listenerList.remove(GameListener.class, listener);
  }



  /**
   * Dispatches the given GameEvent to all interested listeners.
   */

  public void fireGameEvent(GameEvent evt){
    Object [] listeners = listenerList.getListenerList();
    for (int i = 0; i < listeners.length; i += 2){
      if (listeners[i] == GameListener.class){
        GameListener listener = (GameListener)listeners[i+1];
        try{
          if (evt instanceof GameStartEvent)
            listener.gameStarted((GameStartEvent)evt);
          else if (evt instanceof GameEndEvent)
            listener.gameEnded((GameEndEvent)evt);
          else if (evt instanceof MoveMadeEvent)
            listener.moveMade((MoveMadeEvent)evt);
          else if (evt instanceof PositionChangedEvent)
            listener.positionChanged((PositionChangedEvent)evt);  
          else if (evt instanceof TakebackEvent)
            listener.takebackOccurred((TakebackEvent)evt);
          else if (evt instanceof IllegalMoveEvent)
            listener.illegalMoveAttempted((IllegalMoveEvent)evt);
          else if (evt instanceof ClockAdjustmentEvent)
            listener.clockAdjusted((ClockAdjustmentEvent)evt);
          else if (evt instanceof BoardFlipEvent)
            listener.boardFlipped((BoardFlipEvent)evt);
          else
            throw new IllegalArgumentException("Unknown GameEvent type: "+evt.getClass());
        } catch (RuntimeException e){
            e.printStackTrace();
          }
      }
    }
  }





  /**
   * Adds the given SeekListener to the list of listeners receiving notification
   * of SeekEvents.
   *
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of SeekJinConnection.
   */

  public void addSeekListener(SeekListener listener){
    if (!(source instanceof SeekJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of SeekJinConnection");

    listenerList.add(SeekListener.class, listener);
  }




  /**
   * Removes the given SeekListener from the list of listeners receiving 
   * notification of SeekEvents.
   * 
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of SeekJinConnection.
   */

  public void removeSeekListener(SeekListener listener){
    if (!(source instanceof SeekJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of SeekJinConnection");

    listenerList.remove(SeekListener.class, listener);
  }




  /**
   * Fires the given SeekEvent to all interested SeekListeners.
   * 
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of SeekJinConnection.
   */

  public void fireSeekEvent(SeekEvent evt){
    if (!(source instanceof SeekJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of SeekJinConnection");

    Object [] listeners = listenerList.getListenerList();
    for (int i = 0; i < listeners.length; i += 2){
      if (listeners[i] == SeekListener.class){
        SeekListener listener = (SeekListener)listeners[i+1];
        try{
          switch(evt.getID()){
            case SeekEvent.SEEK_ADDED:
              listener.seekAdded(evt);
              break;
            case SeekEvent.SEEK_REMOVED:
              listener.seekRemoved(evt);
              break;
            default:
              throw new IllegalArgumentException("Unknown SeekEvent ID: "+evt.getID());
          }
        } catch (RuntimeException e){
            e.printStackTrace();
          }
      }
    }
  }




  /**
   * Adds the given GameListListener to receive notifications of GameListEvents.
   * 
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of GameListJinConnection.
   */

  public void addGameListListener(GameListListener listener){
    if (!(source instanceof GameListJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of GameListJinConnection");

    listenerList.add(GameListListener.class, listener);
  }




  /**
   * Removes the given GameListListener from the list of listeners receiving
   * notifications of GameListEvents.
   * 
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of GameListJinConnection.
   */

  public void removeGameListListener(GameListListener listener){
    if (!(source instanceof GameListJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of GameListJinConnection");

    listenerList.remove(GameListListener.class, listener);
  }




  /**
   * Fires the given GameListEvent to all interested GameListListeners.
   * 
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of GameListJinConnection.
   */

  public void fireGameListEvent(GameListEvent evt){
    if (!(source instanceof GameListJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of GameListJinConnection");

    Object [] listeners = listenerList.getListenerList();
    for (int i = 0; i < listeners.length; i += 2){
      if (listeners[i] == GameListListener.class){
        GameListListener listener = (GameListListener)listeners[i+1];
        try{
          listener.gameListArrived(evt);
        } catch (RuntimeException e){
            e.printStackTrace();
          }
      }
    }
  }





  /**
   * Adds the given FriendsListener to the list of listeners receiving
   * notifications about friends.
   *
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of FriendsListJinConnection.
   */

  public void addFriendsListener(FriendsListener listener){
    if (!(source instanceof FriendsJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of FriendsJinConnection");

    listenerList.add(FriendsListener.class, listener);
  }





  /**
   * Removes the given FriendsListener from the list of listeners receiving
   * notifications about friends.
   *
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of FriendsListJinConnection.
   */

  public void removeFriendsListener(FriendsListener listener){
    if (!(source instanceof FriendsJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of FriendsJinConnection");

    listenerList.remove(FriendsListener.class, listener);
  }




  /**
   * Dispatches the given FriendsEvent to all interested listeners.
   *
   * @throws free.util.UnsupportedOperationException if the source JinConnection
   * is not an instance of FriendsListJinConnection.
   */

  public void fireFriendsEvent(FriendsEvent evt){
    if (!(source instanceof FriendsJinConnection))
      throw new free.util.UnsupportedOperationException("The source JinConnection is not an instance of FriendsJinConnection");

    Object [] listenerList = this.listenerList.getListenerList();
    for (int i = 0; i < listenerList.length; i += 2){
      if (listenerList[i] == FriendsListener.class){
        FriendsListener listener = (FriendsListener)listenerList[i+1];
        try{
          switch (evt.getID()){
            case FriendsEvent.FRIEND_CONNECTED:
              listener.friendConnected(evt);
              break;
            case FriendsEvent.FRIEND_DISCONNECTED:
              listener.friendDisconnected(evt);
              break;
            case FriendsEvent.FRIEND_ADDED:
              listener.friendAdded(evt);
              break;
            case FriendsEvent.FRIEND_REMOVED:
              listener.friendRemoved(evt);
              break;
            case FriendsEvent.FRIEND_ONLINE:
              listener.friendOnline(evt);
              break;
          }
        } catch (RuntimeException e){
            e.printStackTrace();
          }
      }
    }
  }


}