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

package free.jin.console;

import free.jin.*;
import free.jin.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.beans.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import free.jin.plugin.Plugin;
import free.util.StringParser;
import free.util.StringEncoder;
import free.util.GraphicsUtilities;
import free.util.IOUtilities;
import free.util.audio.AudioClip;
import java.io.InputStream;
import java.io.IOException;



/**
 * A Plugin which implements the consoles functionality. It's responsible for
 * opening, positioning and closing the various consoles used by the user.
 */

public class ConsoleManager extends Plugin implements PlainTextListener, ChatListener, ConnectionListener, GameListListener{

  
  /**
   * The main console.
   */

  protected Console console;



  /**
   * The JInternalFrame in which the console sits.
   */

  protected JInternalFrame consoleFrame;




  /**
   * The current game list table display style.
   */

  protected String gameListDisplayStyle;

  



  /**
   * Starts this plugin.
   */

  public void start(){
    // Some procedural programming :-)
    initState();
    openConsole();
    registerConnListeners();
    JinConnection conn = getConnection();
    console.addToOutput("Trying to connect to "+conn.getHostname()+" on port "+conn.getPort()+"...", "info");
  }




  /**
   * Stops the plugin.
   */

  public void stop(){
    // Again, some procedural clean up.
    saveState();
    unregisterConnListeners();
    closeConsole();
  }





  /**
   * Sets all the variables representing the current settings from the parameters.
   */

  protected void initState(){
    gameListDisplayStyle = getProperty("game-list-display-style","embedded");
  }





  /**
   * Creates and opens the console.
   */

  private void openConsole(){
    console = createConsole();

    consoleFrame = new JInternalFrame("Main Console",true,true,true,true);

    /* See http://developer.java.sun.com/developer/bugParade/bugs/4176136.html on
       why I do this instead of adding an InternalFrameListener like a sane person. */
    consoleFrame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
    consoleFrame.addVetoableChangeListener(new VetoableChangeListener(){

      public void vetoableChange(PropertyChangeEvent pce) throws PropertyVetoException{
        if (pce.getPropertyName().equals(JInternalFrame.IS_CLOSED_PROPERTY)&&
              pce.getOldValue().equals(Boolean.FALSE)&&pce.getNewValue().equals(Boolean.TRUE)){

          int result = JOptionPane.YES_OPTION; 

          if (getConnection().isConnected())
            result = JOptionPane.showConfirmDialog(getPluginContext().getMainFrame(),"Really close this window and log out?","Select an option",JOptionPane.YES_NO_OPTION);
          if (result==JOptionPane.YES_OPTION)
            getPluginContext().getMainFrame().closeConnection(getConnection());
          else
            throw new PropertyVetoException("Canceled closing", pce);
        }
      }

    });

    Container content = consoleFrame.getContentPane();
    content.setLayout(new BorderLayout());
    content.add(console, BorderLayout.CENTER);

    JDesktopPane desktop = getPluginContext().getMainFrame().getDesktop();
    desktop.add(consoleFrame);

    Rectangle desktopBounds = new Rectangle(desktop.getSize());
    String boundsString = getProperty("frame-bounds");
    Rectangle bounds = null;
    if (boundsString!=null)
      bounds = StringParser.parseRectangle(boundsString);

    if (bounds==null)
      consoleFrame.setBounds(0, 0, desktopBounds.width*3/4, desktopBounds.height*3/4);
    else
      consoleFrame.setBounds(bounds);

    boolean isMaximized = Boolean.valueOf(getProperty("maximized","false")).booleanValue();
    if (isMaximized){
      try{
        consoleFrame.setMaximum(true);
      } catch (java.beans.PropertyVetoException e){}
    }

    boolean isIconified = Boolean.valueOf(getProperty("iconified","false")).booleanValue();
    if (isIconified){
      try{
        consoleFrame.setIcon(true);
      } catch (java.beans.PropertyVetoException e){}
    }

    JComponent icon = consoleFrame.getDesktopIcon();
    String iconBoundsString = getProperty("frame-icon-bounds");
    if (iconBoundsString!=null){
      Rectangle iconBounds = StringParser.parseRectangle(iconBoundsString);
      icon.setBounds(iconBounds);
    }

    consoleFrame.setVisible(true);
    try{
      consoleFrame.setSelected(true);
    } catch (java.beans.PropertyVetoException e){} // Ignore.

    console.requestDefaultFocus();
  }




  /**
   * Creates the Console for this ConsoleManager.
   */

  protected Console createConsole(){
    Console console = new Console(this);
    return console;
  }



  /**
   * Creates and returns the JMenu for this plugin.
   */

  public JMenu createPluginMenu(){
    JMenu myMenu = new JMenu(getName());

    if (getConnection() instanceof GameListJinConnection){
      JMenu gameListDisplayStyleMenu = new JMenu("Game lists display style");

      JCheckBoxMenuItem embeddedCB = new JCheckBoxMenuItem("Embedded", gameListDisplayStyle.equalsIgnoreCase("embedded"));
      JCheckBoxMenuItem framedCB = new JCheckBoxMenuItem("Framed", gameListDisplayStyle.equalsIgnoreCase("framed"));
      JCheckBoxMenuItem noneCB = new JCheckBoxMenuItem("None", gameListDisplayStyle.equalsIgnoreCase("none"));
      embeddedCB.setActionCommand("embedded");
      framedCB.setActionCommand("framed");
      noneCB.setActionCommand("none");
      ButtonGroup gameListDisplayStyleCBGroup = new ButtonGroup();
      gameListDisplayStyleCBGroup.add(embeddedCB);
      gameListDisplayStyleCBGroup.add(framedCB);
      gameListDisplayStyleCBGroup.add(noneCB);

      ActionListener gameListDisplayStyleListener = new ActionListener(){
        
        public void actionPerformed(ActionEvent evt){
          String actionCommand = evt.getActionCommand();
          if (actionCommand.equals(gameListDisplayStyle))
            return;
          String oldStyle = gameListDisplayStyle;
          gameListDisplayStyle = actionCommand;

          if (gameListDisplayStyle.equals("none"))
            ((GameListJinConnection)getConnection()).removeGameListListener(ConsoleManager.this);
          else if (oldStyle.equalsIgnoreCase("none"))
            ((GameListJinConnection)getConnection()).addGameListListener(ConsoleManager.this);
        }
      };

      embeddedCB.addActionListener(gameListDisplayStyleListener);
      framedCB.addActionListener(gameListDisplayStyleListener);
      noneCB.addActionListener(gameListDisplayStyleListener);

      gameListDisplayStyleMenu.add(embeddedCB);
      gameListDisplayStyleMenu.add(framedCB);
      gameListDisplayStyleMenu.add(noneCB);

      myMenu.add(gameListDisplayStyleMenu);
    }


    final JCheckBoxMenuItem copyOnSelectCB = new JCheckBoxMenuItem("Copy on Select",new Boolean(getProperty("copyOnSelect","true")).booleanValue());
    copyOnSelectCB.addChangeListener(new ChangeListener(){
      
      public void stateChanged(ChangeEvent evt){
        getUser().setProperty(getID()+".copyOnSelect",copyOnSelectCB.isSelected() ? "true" : "false", true);
      }
  
    });
    myMenu.add(copyOnSelectCB);

    return myMenu;
  }




  /**
   * Closes the console.
   */

  private void closeConsole(){
    consoleFrame.removeAll();
    consoleFrame.dispose();
  }




  /**
   * Registers all the necessary listeners to JinConnection events.
   */

  protected void registerConnListeners(){
    JinConnection conn = getConnection();
    conn.addPlainTextListener(this);
    conn.addChatListener(this);
    conn.addConnectionListener(this);
    if ((conn instanceof GameListJinConnection)&&(!gameListDisplayStyle.equalsIgnoreCase("none")))
      ((GameListJinConnection)conn).addGameListListener(this);
  }




  /**
   * Unregisters all the listeners we've registered with the JinConnection.
   */

  protected void unregisterConnListeners(){
    JinConnection conn = getConnection();
    conn.removePlainTextListener(this);
    conn.removeChatListener(this);
    conn.removeConnectionListener(this);
    if ((conn instanceof GameListJinConnection)&&(!gameListDisplayStyle.equalsIgnoreCase("none")))
      ((GameListJinConnection)conn).removeGameListListener(this);
  } 




  /**
   * Listens to plain text and adds it to the console.
   */

  public void plainTextArrived(PlainTextEvent evt){
    console.addToOutput(evt.getText(), "plain");
  }



  /**
   * Listens to ChatEvents and adds appropriate text to the console.
   */

  public void chatMessageArrived(ChatEvent evt){
    String type = evt.getType();
    Object forum = evt.getForum();
    String sender = evt.getSender();
    String chatMessageType = type+"."+(forum == null ? "" : forum.toString())+"."+sender;

    console.addToOutput(translateChat(evt),chatMessageType);
  }




  /**
   * Translates the given ChatEvent into a string to be added to the console's
   * output. This method is intended to be overridden by server specific classes.
   * The default implementation returns a string useful only for debugging.
   */

  protected String translateChat(ChatEvent evt){
    return evt.toString();
  }




  /**
   * Gets called when the connection to the server is established.
   */

  public void connectionEstablished(ConnectionEvent evt){
    console.addToOutput("Connected","info");  
  }




  /**
   * Gets called when the login procedure is done.
   */

  public void connectionLoggedIn(ConnectionEvent evt){
    consoleFrame.setTitle("Main Console - "+getConnection().getUsername()+" on "+getUser().getServer().getProperty("name.long"));    
    consoleFrame.repaint(); // The title doesn't repaint itself.
  }




  /**
   * Gets called when the connection to the server is lost.
   */

  public void connectionLost(ConnectionEvent evt){
    console.addToOutput("WARNING: Disconnected", "info");
  }




  /**
   * Creates a table to display a game list item for the given GameListEvent.
   */

  protected JTable createGameListTable(GameListEvent evt){
    return new GameListTable(console, evt);
  }

  


  /**
   * Gets called when a game list arrives from the server.
   * Adds a JTable displaying the list to the console.
   */

  public void gameListArrived(GameListEvent evt){
    final JTable table = createGameListTable(evt);
    JTableHeader header = table.getTableHeader();
    Dimension originalPrefSize = header.getPreferredSize();
    // This abomination is needed because Metal L&F has a too small preferred label height on 1.1
    header.setPreferredSize(new Dimension(originalPrefSize.width, Math.max(originalPrefSize.height, 18)));

    JScrollPane scrollPane = new JScrollPane(table);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    // The following block sets the preferred sizes of the columns to the maximum
    // preferred width of the cells in that column.
    TableColumnModel columnModel = table.getColumnModel();
    TableModel model = table.getModel();
    for (int columnIndex=0;columnIndex<columnModel.getColumnCount();columnIndex++){
      TableColumn column = columnModel.getColumn(columnIndex);
      Component headerRendererComponent = column.getHeaderRenderer().getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, columnIndex);
      int maxWidth = headerRendererComponent.getPreferredSize().width;
      for (int rowIndex=0;rowIndex<model.getRowCount();rowIndex++){
        TableCellRenderer cellRenderer = table.getCellRenderer(rowIndex, columnIndex);
        Object value = model.getValueAt(rowIndex, columnIndex);
        Component rendererComponent = cellRenderer.getTableCellRendererComponent(table, value, false, false, rowIndex, columnIndex);
        int cellWidth = rendererComponent.getPreferredSize().width;
        if (cellWidth>maxWidth)
          maxWidth = cellWidth;
      }
      if (maxWidth>150) // This is probably the "note" column, which is very wide but we don't want it to take all the space
        column.setPreferredWidth(50);
      else
        column.setPreferredWidth(maxWidth);
    }

    String title = "  "+evt.getListTitle()+".  Displaying items "+evt.getFirstIndex()+"-"+evt.getLastIndex()+" out of "+evt.getItemCount()+"  ";
    if (gameListDisplayStyle.equals("embedded")){
      scrollPane.setBorder(new TitledBorder(title));
      int maxHeight = (console.getOutputArea().height-40)*2/3;
      if (scrollPane.getPreferredSize().height>maxHeight)
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, maxHeight));
      console.addToOutput(scrollPane);
    }
    else{
      JInternalFrame frame = new JInternalFrame(title, true, true, true, true);
      frame.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
      frame.getContentPane().add(scrollPane);

      JDesktopPane desktop = getPluginContext().getMainFrame().getDesktop();
      Dimension desktopSize = desktop.getSize();
      Dimension prefSize = frame.getPreferredSize();
      int width = Math.min(prefSize.width, desktopSize.width);
      int height = Math.min(prefSize.height, desktopSize.height);
      int x = (desktopSize.width-width)/2;
      int y = (desktopSize.height-height)/2;
      frame.setBounds(x, y, width, height);

      desktop.add(frame);
      frame.setVisible(true);
      try{
        frame.setSelected(true);
      } catch (java.beans.PropertyVetoException e){} // Ignore.
    }
  }



  /**
   * Saves the current state into the user file.
   */

  private void saveState(){
    User user = getUser();
    String prefix = getID()+".";

    boolean isMaximized = consoleFrame.isMaximum();
    user.setProperty(prefix+"maximized", String.valueOf(isMaximized), false);

    boolean isIconified = consoleFrame.isIcon();
    user.setProperty(prefix+"iconified", String.valueOf(isIconified), false);

    // This is the only way to retrieve the "normal" bounds of the frame under
    // JDK1.2 and earlier. JDK1.3 has a getNormalBounds() method.
    if (isMaximized){
      try{
        consoleFrame.setMaximum(false);
      } catch (java.beans.PropertyVetoException ex){}
    }

    Rectangle consoleFrameBounds = consoleFrame.getBounds();
    // If something bad happened, let's not save that state.
    if ((consoleFrameBounds.width>10)&&(consoleFrameBounds.height>10))
      user.setProperty(prefix+"frame-bounds",StringEncoder.encodeRectangle(consoleFrameBounds),false);

    if (!getProperty("game-list-display-style", "embedded").equalsIgnoreCase(gameListDisplayStyle))
      user.setProperty(prefix+"game-list-display-style", gameListDisplayStyle, true);
  }

} 