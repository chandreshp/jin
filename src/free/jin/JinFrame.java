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

package free.jin;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import free.jin.plugin.Plugin;
import free.jin.plugin.PluginContext;
import free.jin.plugin.UnsupportedContextException;
import free.util.ArrayEnumeration;
import java.io.*;
import java.util.*;


/**
 * Jin's main frame.
 */

public class JinFrame extends JFrame{


  
  /**
   * Our current JinConnection, or <code>null</code> if no open connections
   * exist.
   */

  private JinConnection connection = null;




  /**
   * The User object representing the user using the current connection, or
   * <code>null</code> if no open connections exist.
   */

  private User user = null;




  /**
   * An array containing all the Plugins running with the current connection,
   * or <code>null<code> if no open connections exist.
   */

  private Plugin [] plugins = null;




  /**
   * Creates a new JinFrame.
   */

  public JinFrame(){
    setJMenuBar(createJMenuBar());

//    getToolkit().getSystemEventQueue().push(new EventQueue(){
//
//      protected void dispatchEvent(AWTEvent evt){
//        super.dispatchEvent(evt);
//
//        if (evt instanceof KeyEvent){
//          System.out.println("KeyEvent: "+evt);
//          System.out.println("Focus owner: "+getFocusOwner());
//          System.out.println();
//        }
//      }
//
//    });

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
  }




  /**
   * Creates the RootPane pane for Jin's main frame.
   */

  protected JRootPane createRootPane(){
    return new JinRootPane(this);
  }



  

  /**
   * Creates the initial JMenuBar used by Jin's main frame.
   */

  protected JMenuBar createJMenuBar(){
    return new JinFrameMenuBar(this);
  }





  /**
   * Returns the current connection, or <code>null</code> if no connections are
   * currently open.
   */

  public JinConnection getConnection(){
    return connection;
  }





  /**
   * Returns Jin's desktop, where all the JInternalFrames reside.
   */

  public JDesktopPane getDesktop(){
    return ((JinRootPane)getRootPane()).getDesktop();
  }




  /**
   * Returns Jin's status bar.
   */

  public JPanel getStatusbar(){
    return ((JinRootPane)getRootPane()).getStatusbar();
  }




  /**
   * Returns the menubar of this JinFrame.
   */

  public JinFrameMenuBar getJinFrameMenuBar(){
    return (JinFrameMenuBar)super.getJMenuBar();
  }






  /**
   * Shows some UI allowing the user to create a new connection.
   */

  public void showConnectionCreationUI(){
    Server chosenServer;

    Server [] servers = Jin.getServers();
    if (servers.length > 1){
      Object result = JOptionPane.showInputDialog(this,"Choose a server to connect to",
        "Choose server", JOptionPane.QUESTION_MESSAGE, null, servers, null);

      if (result == null)
        return;

      chosenServer = (Server)result;
    }
    else if (servers.length == 0){
      JOptionPane.showMessageDialog(this, "No defined servers, will exit.");
      System.exit(0);
      return; // Silly compiler
    }
    else
      chosenServer = servers[0];

    showLoginDialog(chosenServer, null);
  }





  /**
   * Shows the LoginDialog for the specified <code>Server</code> and
   * <code>User</code>. The <code>User</code> object may be null.
   */

  public void showLoginDialog(Server server, User user){
    LoginDialog loginDialog = user == null ? server.createLoginDialog() : server.createLoginDialog(user);
    loginDialog.show(this);
    if (loginDialog.isCanceled())
      return;

    JinConnection conn = loginDialog.createConnection();
    startConnection(conn, loginDialog.getUser());
  }





  /**
   * Creates and starts all the necessary plugins that should be created for the
   * given user, then connects the given JinConnection to the server. The connecting
   * part is done asynchronously because JinConnection.connect() blocks.
   */

  private void startConnection(JinConnection conn, User user){
    this.connection = conn;
    this.user = user;

    // Phase 1 - initialize the plugins
    String pluginsString = user.getProperty("plugins", "");
    Hashtable pluginsToProperties = new Hashtable();
    Hashtable pluginNamesToPlugins = new Hashtable();
    StringTokenizer pluginsTokenizer = new StringTokenizer(pluginsString, "; ");
    if (pluginsTokenizer.countTokens() == 0){ // No plugins
      System.err.println("There are no plugins assosiated with "+user+" - will not be connecting");
      return;
    }
    plugins = new Plugin[pluginsTokenizer.countTokens()];
    int pluginCount = 0;
    while (pluginsTokenizer.hasMoreTokens()){
      String pluginName = pluginsTokenizer.nextToken();
      try{
        InputStream in = getClass().getResourceAsStream("resources/plugins/"+pluginName+".properties");
        Properties pluginProperties = new Properties();
        pluginProperties.load(in);
        in.close();

        Class pluginClass = Class.forName(pluginProperties.getProperty("class"));
        Plugin plugin = (Plugin)pluginClass.newInstance();
        plugins[pluginCount++] = plugin;
        pluginsToProperties.put(plugin, pluginProperties);
        pluginNamesToPlugins.put(pluginName, plugin);
      } catch (Exception e){                                         
          synchronized(System.err){
            System.err.println("Unable to load plugin "+pluginName);
            e.printStackTrace();
          }
        }
    }


    // Phase 2 - set the plugins' context
    for (int i = 0; i < plugins.length; i++){
      Plugin plugin = plugins[i];
      PluginContext context = new PluginContext(user, conn, this, (Properties)pluginsToProperties.get(plugin), pluginNamesToPlugins);
      try{
        plugin.setContext(context);
      } catch (UnsupportedContextException e){
          System.out.println("The plugin \""+plugin+"\" doesn't support the given server or connection implementation ("+e.getMessage()+")");
          plugins[i] = null;
        }
        catch (RuntimeException e){ // Make sure that one bad plugin doesn't spoil for the rest.
          String message = "The plugin \""+plugin+"\" failed to start.";
          String errorMessage = e.getMessage();
          String [] errorMessages = errorMessage == null ? new String[]{message} : new String[]{message,
                                   "Error message: "+errorMessage};
          JOptionPane.showMessageDialog(this, errorMessages, "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
          plugins[i] = null;
        }
    }


    int startedPluginsCount = 0;

    // Phase 3 - start the plugins
    for (int i = 0; i < plugins.length; i++){
      Plugin plugin = plugins[i];
      if (plugin != null){
        try{
          plugin.start();
          startedPluginsCount++;
        } catch (RuntimeException e){ // Make sure that one bad plugin doesn't spoil for the rest.
            String message = "The plugin \""+plugin+"\" failed to start.";
            String errorMessage = e.getMessage();
            String [] errorMessages = errorMessage == null ? new String[]{message} : new String[]{message,
                                     "Error message: "+errorMessage};
            JOptionPane.showMessageDialog(this, errorMessages, "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            plugins[i] = null;
          }
      }
    }



    // Delete the plugins that failed to initialize
    Plugin [] startedPlugins = new Plugin[startedPluginsCount];
    for (int i = 0; i < plugins.length; i++){
      if (plugins[i] != null){
        startedPlugins[startedPlugins.length - startedPluginsCount] = plugins[i];
        startedPluginsCount--;
      }
    }
    plugins = startedPlugins;



    // Phase 4 - Tell JinFrameMenuBar to add the required menus on-connect.
    JinFrameMenuBar menubar = getJinFrameMenuBar();
    menubar.connecting(conn, user, new ArrayEnumeration(plugins));


    // Phase 5 - connect.
    new Thread(){

      public void run(){
        try{
          JinConnection connection = getConnection();
          if (connection != null){
            if (!connection.isConnected())
              getConnection().connect();
          }
        } catch (IOException e){
            synchronized(System.err){
              System.err.println("Unable to connect to server");
              e.printStackTrace();
            }
          }
      }

    }.start();
  }





  /**
   * Closes the current Connection by stopping all its plugins and shutting down
   * the JinConnection.
   */

  public void closeConnection(){
    for (int i = 0; i < plugins.length; i++){
      try{
        Plugin plugin = plugins[i];
        if (plugin!=null){
          System.out.println("Stopping "+plugin.getName());
          plugin.stop();  
        }
      } catch (Throwable e){ // Make sure plugins don't bother other plugins.
          synchronized(System.err){
            System.err.println("An exception occurred while stopping a plugin:");
            e.printStackTrace();
          }
        }
    }

    if (!user.isGuest()){
      System.out.println("Saving user settings");
      String userPath = Jin.saveUser(user);

      if (userPath != null){
        System.out.println("Saving last user information");
        Jin.setProperty("last.user.path", userPath);
      }
    }

    System.out.println("Closing connection");
    try{
      if (connection.isConnected()){
        connection.exit();
        connection.disconnect();
      }
    } catch (IOException e){
        synchronized(System.err){
          System.err.println("Failed to disconnect connection: "+connection);
          e.printStackTrace();
        }
      } 
    
    System.out.println("Modifying menubar");
    JinFrameMenuBar menubar = getJinFrameMenuBar();
    menubar.disconnected(connection, user);

    this.connection = null;
    this.user = null;
    this.plugins = null;

    // Bugfix
    menubar.repaint();
  }





  /**
   * This method stops all the plugins and closes all the connections. It's
   * called just before Jin exits.
   */

  public void exiting(){
    if (getConnection() != null)
      closeConnection();
  } 






  /**
   * Shows the login dialog to the user on WINDOW_OPENED event.
   */

  protected void processWindowEvent(WindowEvent evt){
    super.processWindowEvent(evt);
    if (evt.getID() == WindowEvent.WINDOW_OPENED){
      String lastUserPath = Jin.getProperty("last.user.path");
      if (lastUserPath != null){
        User user = Jin.loadUser(lastUserPath);
        if (user != null)
          showLoginDialog(user.getServer(), user);
        return;
      }
      showConnectionCreationUI();
    }
  }



}
