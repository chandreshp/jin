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

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Hashtable;
import java.util.Vector;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import free.jin.Jin;
import free.jin.JinConnection;
import free.jin.plugin.Plugin;
import free.util.StringParser;
import free.util.BrowserControl;
import free.workarounds.FixUtils;
import jregex.*;


/**
 * A Component which implements a text console in which the user can see the
 * output of the server and write/send arbitrary commands to the server. This
 * is a component that can be used by various plugins - it's mainly used by
 * free.jin.console.ConsoleManager.
 */

public class Console extends JPanel implements KeyListener, ContainerListener{


  /**
   * The listener list.
   */

  protected final EventListenerList listenerList = new EventListenerList();



  /**
   * The ConsoleTextPane where the output is displayed.
   */

  private final ConsoleTextPane outputComponent;



  /**
   * The JScrollPane wrapping the output component.
   */

  private final JScrollPane outputScrollPane;



  /**
   * The ConsoleTextField which takes the input from the user.
   */
  
  private final ConsoleTextField inputComponent;



  /**
   * The Plugin which uses us, we use its properties to determine all kinds of
   * our properties (text colors etc.). TODO: Allow setting the properties
   * separately from setting the user Plugin.
   */

  protected final Plugin userPlugin;




  /**
   * The regular expressions against which we match the text to find links.
   */

  private Pattern [] linkREs;




  /**
   * The commands executed for the matched links.
   */

  private String [] linkCommands;




  /**
   * The indices of the subexpression to make a link out of.
   */

  private int [] linkSubexpressionIndices;




  /**
   * The regular expression we use for detecting URLs.
   */

  private static final Pattern urlRegex = new Pattern("(((ftp|http(s)?)://)|(www\\.))([^\\s()<>\"])*[^\\s.,()<>\"]");



  /**
   * The regular expression we use for detecting emails.
   */

  private static final Pattern emailRegex = new Pattern("[^\\s()<>\"]+@[^\\s()<>\"]+\\.[^\\s.,()<>\"]+");


  

  /**
   * Maps text types that were actually looked up to the resulting AttributeSets.
   */

  private final Hashtable attributesCache = new Hashtable();




  /**
   * A history of people who have told us anything.
   */

  private final Vector tellers = new Vector();




  /**
   * The amount of times addToOutput was called. See {@see #addToOutput(String, String)}
   * for the hack involved.
   */

  private int numAddToOutputCalls = 0;




  /**
   * Whether the runnable that is supposed to scroll the scrollpane to the
   * bottom already executed. See {@see #addToOutput(String, String)}
   * for the hack involved.
   */

  private boolean didScrollToBottom = true;





  /**
   * Creates a new Console which is used by the given Plugin. The Console uses
   * various properties of the plugin to determine how do display text etc.
   * TODO: Write a documentation of which properties it uses.
   */

  public Console(Plugin userPlugin){
    this.userPlugin = userPlugin;

    this.outputComponent = createOutputComponent();
    this.outputScrollPane = createOutputScrollPane(outputComponent);
    this.inputComponent = createInputComponent();

    setLayout(new BorderLayout());
    add(outputScrollPane, BorderLayout.CENTER);
    add(inputComponent, BorderLayout.SOUTH);

//    outputComponent.addKeyListener(this);
    inputComponent.addKeyListener(this);
    outputComponent.addContainerListener(this);

    init();
  }




  /**
   * Creates the JTextPane to which the server's textual output goes.
   */

  protected ConsoleTextPane createOutputComponent(){
    final ConsoleTextPane textPane = new ConsoleTextPane(this);

    // Seriously hack the caret for our own purposes (desired scrolling and selecting).
    Caret caret = new DefaultCaret(){
      public void focusLost(FocusEvent e) {
        this.setVisible(false);
      }

      protected void adjustVisibility(Rectangle nloc){
        if (!dragging)
          return;

        if (SwingUtilities.isEventDispatchThread()){
          textPane.scrollRectToVisible(nloc);
          if (nloc.y+nloc.height>textPane.getSize().height-nloc.height/2){
            BoundedRangeModel scrollModel = outputScrollPane.getVerticalScrollBar().getModel();
            scrollModel.setValue(scrollModel.getMaximum());
          }
        }
        else{
          super.adjustVisibility(nloc); // Just in case... shouldn't happen.
        } 
      }

      private boolean dragging = false;
      private boolean realDrag = false;

      public void mousePressed(MouseEvent e){
        dragging = true;
        realDrag = false;
        super.mousePressed(e);
      }

      public void mouseReleased(MouseEvent e){
        dragging = false;
        super.mouseReleased(e);
        if (realDrag&&isCopyOnSelect())
          requestDefaultFocus();
      }

      public void mouseDragged(MouseEvent e){
        realDrag = true;
        super.mouseDragged(e);
      }

      public void mouseClicked(MouseEvent e){
        super.mouseClicked(e);
        if (isCopyOnSelect())
          requestDefaultFocus();
      }

      protected void moveCaret(MouseEvent e){
        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = textPane.getUI().viewToModel(textPane, pt, biasRet);
        if (pos >= 0) {
          int maxPos = textPane.getDocument().getEndPosition().getOffset();
          if ((maxPos==pos+1)&&(pos>0)){
            pos--;
            moveDot(pos);
            if (dragging){
              BoundedRangeModel scrollModel = outputScrollPane.getVerticalScrollBar().getModel();
              scrollModel.setValue(scrollModel.getMaximum());
            }
          }
          else
            moveDot(pos);
        }
      }

      protected void positionCaret(MouseEvent e) {
        Point pt = new Point(e.getX(), e.getY());
        Position.Bias[] biasRet = new Position.Bias[1];
        int pos = textPane.getUI().viewToModel(textPane, pt, biasRet);
        if (pos >= 0) {
          int maxPos = textPane.getDocument().getEndPosition().getOffset();
          if ((maxPos==pos+1)&&(pos>0)){
            pos--;
            setDot(pos);
            if (dragging){
              BoundedRangeModel scrollModel = outputScrollPane.getVerticalScrollBar().getModel();
              scrollModel.setValue(scrollModel.getMaximum());
            }
          }
          else
            setDot(pos);
        }
      }


    };

    caret.addChangeListener(new ChangeListener(){
      public void stateChanged(ChangeEvent evt){
        if (isCopyOnSelect())
          textPane.copy(); // CDE/Motif style copy/paste
      }
    });

    textPane.setCaret(caret);

    return textPane;
  }





  /**
   * The JViewport we use as the viewport for the scrollpane of the output
   * component. This class being the viewport makes sure that when a console is
   * resized, the currently displayed text remains such. The anchor is the last
   * currently visible character.
   */

  protected class OutputComponentViewport extends JViewport{

    private int lastVisibleIndex = -1;


    public void reshape(int x, int y, int width, int height){
      JTextComponent view = (JTextComponent)getView();
      Dimension curSize = this.getSize();
      if ((view != null)&&(lastVisibleIndex==-1)&&((curSize.width!=width)||(curSize.height!=height))){
        Dimension viewHoleSize = getExtentSize();
        Point viewLocation = view.getLocation();
        Point bottomRightPoint = new Point(viewHoleSize.width-viewLocation.x, viewHoleSize.height-viewLocation.y-1);
        lastVisibleIndex = view.viewToModel(bottomRightPoint);
      }

      super.reshape(x, y, width, height);
    }


    public void setViewSize(Dimension newSize){
      super.setViewSize(newSize);

      SwingUtilities.invokeLater(new Runnable(){
        public void run(){
          if (lastVisibleIndex != -1){
            try{
              JTextComponent view = (JTextComponent)getView();              
              Rectangle rect = view.modelToView(lastVisibleIndex);

              if (rect!=null){
//                OutputComponentViewport.this.setVisible(false);
                view.scrollRectToVisible(new Rectangle(0,0,1,1));
                view.scrollRectToVisible(rect);
//                view.scrollRectToVisible(rect);
//                OutputComponentViewport.this.setVisible(true);
              }
              lastVisibleIndex = -1;
            } catch (BadLocationException e){}
          }
        }
      });
    }

  }




  /**
   * Creates the JScrollPane in which the output component will be put.
   */

  protected JScrollPane createOutputScrollPane(JTextPane outputComponent){
    JViewport viewport = new OutputComponentViewport();
    viewport.setView(outputComponent);

    JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setViewport(viewport);

    viewport.putClientProperty("EnableWindowBlit", Boolean.TRUE);

    return scrollPane;
  }




  /**
   * Creates the JTextField in which the user can input commands to be sent to
   * the server.
   */

  protected ConsoleTextField createInputComponent(){
    ConsoleTextField textField = new ConsoleTextField(this);
    return textField;
  }




  
  /**
   * Assigns the default focus to input component.
   */

  public boolean requestDefaultFocus(){
    inputComponent.requestFocus();
    return true;
  }




  /**
   * Initializes this console, loading all the properties from the plugin, etc.
   * The Console uses the Plugin's (and the User's properties) to determine its 
   * various properties (text color etc.)
   */

  private void init(){
    attributesCache.clear(); // Clear the cache


    /********************* OUTPUT COMPONENT ***********************/

    // We set it here because of a Swing bug which causes the background to be 
    // drawn with the foreground color if you set the background as an attribute.
    String outputBg = getProperty("background");
    if (outputBg!=null)
      outputComponent.setBackground(StringParser.parseColor(outputBg));

    String outputSelection = getProperty("output-selection");
    if (outputSelection!=null)
      outputComponent.setSelectionColor(StringParser.parseColor(outputSelection));

    String outputSelected = getProperty("output-selected");
    if (outputSelected!=null)
      outputComponent.setSelectedTextColor(StringParser.parseColor(outputSelected));      



    /********************* INPUT COMPONENT *************************/

    String inputBg = getProperty("input-background");
    if (inputBg!=null)
      inputComponent.setBackground(StringParser.parseColor(inputBg));

    String inputFg = getProperty("input-foreground");
    if (inputFg!=null)
      inputComponent.setForeground(StringParser.parseColor(inputFg));

    String inputSelection = getProperty("input-selection");
    if (inputSelection!=null)
      inputComponent.setSelectionColor(StringParser.parseColor(inputSelection));

    String inputSelected = getProperty("input-selected");
    if (inputSelected!=null)
      inputComponent.setSelectedTextColor(StringParser.parseColor(inputSelected));      

    // TODO: How do we change the font so it affects correctly all the L&Fs ?


    int numLinkPatterns = Integer.parseInt(getProperty("output-link.num-patterns","0"));
    linkREs = new Pattern[numLinkPatterns];
    linkCommands = new String[numLinkPatterns];
    linkSubexpressionIndices = new int[numLinkPatterns];
    for (int i=0;i<numLinkPatterns;i++){
      try{
        String linkPattern = getProperty("output-link.pattern-"+i);
        String linkCommand = getProperty("output-link.command-"+i);
        String subexpressionIndexString = getProperty("output-link.index-"+i);
        if ((linkPattern!=null)&&(linkCommand!=null)&&(subexpressionIndexString!=null)){
          linkSubexpressionIndices[i] = Integer.parseInt(subexpressionIndexString);
          Pattern regex = new Pattern(linkPattern);
          linkREs[i] = regex;
          linkCommands[i] = linkCommand;
        }
      } catch (PatternSyntaxException e){
          e.printStackTrace();
        }
        catch (NumberFormatException e){
          e.printStackTrace();
        }
    }
  }




  /**
   * Refreshed the console by re-reading the plugin/user properties and
   * adjusting the assosiated console properties accordingly. This is useful
   * to call after a user changes the preferences.
   */

  public void refreshFromProperties(){
    init();
  }





  /**
   * Returns true if selected text will be copied into the clipboard on selection.
   * Returns false otherwise. This checks the value of the "copyOnSelect"
   * property
   */

  protected boolean isCopyOnSelect(){
    String val = userPlugin.getProperty("copyOnSelect", "true");
    return new Boolean(val).booleanValue();
  }




  /**
   * This method <B>must</B> be called before adding anything to the output
   * component. This method works together with the <code>assureScrolling</code>
   * method.
   *
   * @returns whether the <code>assureScrolling</code> method should scroll the
   * scrollpane of the output component to the bottom. This needs to be passed
   * to the <code>assureScrolling</code> method.
   */

  protected final boolean prepareAdding(){
    // Seriously hack the scrolling to make sure if we're at the bottom, we stay there,
    // and if not, we stay there too :-) If you figure out what (and why) I'm doing, drop me an email,
    // and we'll hire you as a Java programmer.
    numAddToOutputCalls++;
    outputScrollPane.getViewport().putClientProperty("EnableWindowBlit", Boolean.FALSE); // Adding a lot of text is slow with blitting
    BoundedRangeModel verticalScroll = outputScrollPane.getVerticalScrollBar().getModel();

    return (verticalScroll.getMaximum()<=verticalScroll.getValue()+verticalScroll.getExtent()+5);
    // The +5 is to scroll it to the bottom even if it's a couple of pixels away.
    // This can happen if you try to scroll to the bottom programmatically
    // (a bug probably) using scrollRectToVisible(Rectangle).
  }




  /**
   * This method <B>must</B> be called after adding anything to the output
   * component. This method works together with the <code>prepareAdding</code>
   * method. Pass the value returned by <code>prepareAdding</code> as the
   * argument of this method.
   */

  protected final void assureScrolling(boolean scrollToBottom){
    class BottomScroller implements Runnable{
      
      private int curNumCalls;
      private int initNumCalls;
      
      BottomScroller(int curNumCalls){
        this.curNumCalls = curNumCalls;
        initNumCalls = curNumCalls;
      }

      public void run(){
        if (numAddToOutputCalls==curNumCalls){
          BoundedRangeModel verticalScrollModel = outputScrollPane.getVerticalScrollBar().getModel();
          verticalScrollModel.setValue(verticalScrollModel.getMaximum());
          didScrollToBottom = true;
          outputScrollPane.getViewport().putClientProperty("EnableWindowBlit",Boolean.TRUE); // Enable blitting again
//          outputComponent.repaint(); Not sure why this is needed
        }
        else{
          curNumCalls = numAddToOutputCalls;
          SwingUtilities.invokeLater(this);
        }
      }
      
    }

    if (scrollToBottom&&didScrollToBottom){
      didScrollToBottom = false;
      SwingUtilities.invokeLater(new BottomScroller(numAddToOutputCalls));
    }
  }




  /**
   * Adds the given component to the output.
   */

  public void addToOutput(JComponent component){
    boolean shouldScroll = prepareAdding();

    boolean wasEditable = outputComponent.isEditable();
    outputComponent.setEditable(true);
    outputComponent.setCaretPosition(outputComponent.getDocument().getLength());
    StyledDocument document = outputComponent.getStyledDocument();
    outputComponent.insertComponent(component);

    // See http://developer.java.sun.com/developer/bugParade/bugs/4353673.html
    LayoutManager layout = component.getParent().getLayout();
    if (layout instanceof OverlayLayout)
      ((OverlayLayout)layout).invalidateLayout(component.getParent());

    try{
      document.insertString(document.getLength(), "\n", null);
    } catch (BadLocationException e){
        e.printStackTrace();
      } 
    outputComponent.setEditable(wasEditable);

    assureScrolling(shouldScroll);
  }




  /**
   * Adds the given text of the given text type to the output.
   *
   * @param text The text to add, '\n' excluded.
   * @param textType The type of the text, "kibitz" for example.
   */

  public void addToOutput(String text, String textType){
    try{
      boolean shouldScroll = prepareAdding();
      addToOutputImpl(text, textType);
      assureScrolling(shouldScroll);
    } catch (BadLocationException e){
        e.printStackTrace(); // Why the heck is this checked?
      }
  }




  /**
   * Actually does the work of adding the given text to the output component's
   * Document.
   */

  protected void addToOutputImpl(String text, String textType) throws BadLocationException{
    StyledDocument document = outputComponent.getStyledDocument();
    int oldTextLength = document.getLength();
    document.insertString(document.getLength(),text+"\n", attributesForTextType(textType));

    AttributeSet urlAttributes = attributesForTextType("link.url");
    AttributeSet emailAttributes = attributesForTextType("link.email");
    AttributeSet commandAttributes = attributesForTextType("link.command");

    MatchIterator urlMatches = urlRegex.matcher(text).findAll();
    while (urlMatches.hasMore()){
      MatchResult result = urlMatches.nextMatch();
      int matchStart = result.start();
      int matchEnd = result.end();

      Command command = new Command("url "+text.substring(matchStart,matchEnd), Command.SPECIAL_MASK|Command.BLANKED_MASK);
      Link link = new Link(matchStart+oldTextLength, matchEnd+oldTextLength, command);
      document.setCharacterAttributes(matchStart+oldTextLength, matchEnd-matchStart, urlAttributes, false);
      outputComponent.addLink(link);
    }

    MatchIterator emailMatches = emailRegex.matcher(text).findAll();
    while (emailMatches.hasMore()){
      MatchResult result = emailMatches.nextMatch();
      int matchStart = result.start();
      int matchEnd = result.end();

      Command command = new Command("email "+text.substring(matchStart,matchEnd), Command.SPECIAL_MASK|Command.BLANKED_MASK);
      Link link = new Link(matchStart+oldTextLength, matchEnd+oldTextLength, command);
      document.setCharacterAttributes(matchStart+oldTextLength, matchEnd-matchStart, emailAttributes, false);
      outputComponent.addLink(link);
    }

    for (int i=0;i<linkREs.length;i++){
      Pattern linkRE = linkREs[i];

      if (linkRE==null) // Bad pattern was given in properties.
        continue;

      MatchIterator matches = linkRE.matcher(text).findAll();      
      while(matches.hasMore()){
        String linkCommand = linkCommands[i];

        MatchResult result = matches.nextMatch();

        int index = -1;
        while ((index = linkCommand.indexOf("$", index+1))!=-1){
          if ((index<linkCommand.length()-1)&&(Character.isDigit(linkCommand.charAt(index+1)))){
            int subexpressionIndex = Character.digit(linkCommand.charAt(index+1),10);
            linkCommand = linkCommand.substring(0,index)+result.group(subexpressionIndex)+linkCommand.substring(index+2);
          }
        }

        int linkSubexpressionIndex = linkSubexpressionIndices[i];
        int matchStart = result.start(linkSubexpressionIndex);
        int matchEnd = result.end(linkSubexpressionIndex);

        document.setCharacterAttributes(matchStart+oldTextLength, matchEnd-matchStart, commandAttributes, false);

        Link link = new Link(matchStart+oldTextLength, matchEnd+oldTextLength, new Command(linkCommand,0));
        outputComponent.addLink(link);
      }
    }

  }





  /**
   * Returns the size of the output area.
   */

  public Dimension getOutputArea(){
    return outputScrollPane.getViewport().getSize();
  }




  /**
   * Executes a special command. The following commands are recognized by this
   * method:
   * <UL>
   *   <LI> cls - Removes all text from the console.
   *   <LI> "url <url>" - Displays the URL  (the '<' and '>' don't actually appear in the string).
   *   <LI> "email <email address>" - Displays the mailer with the "To" field set to the given email address.
   * </UL>
   */

  protected void executeSpecialCommand(String command){
    command = command.trim();
    if (command.equalsIgnoreCase("cls")){
      clear();
    }
    else if (command.startsWith("url ")){
      String urlString = command.substring("url ".length());
      try{
        BrowserControl.displayURL(urlString);
      } catch (IOException e){
          e.printStackTrace();
        }
    }
    else if (command.startsWith("email ")){
      String emailString = command.substring("email ".length());
      try{
        BrowserControl.displayMailer(emailString);
      } catch (IOException e){
          e.printStackTrace();
        }
    }
    else{
      addToOutput("Unknown special command: \""+command+"\"","system");
    }
  }
  



  
  /**
   * Executes the given command.
   */

  public void issueCommand(Command command){
    String commandString = command.getCommandString();

    if (!command.isBlanked()){
      addToOutput(commandString, "user");
    } 

    if (command.isSpecial())
      executeSpecialCommand(commandString);
    else{
      JinConnection conn = userPlugin.getConnection();
      if (conn.isConnected())
        conn.sendCommand(commandString);
      else
        addToOutput("Unable to issue command - not connected to the server.", "info");
    }
  }




  /**
   * Removes all text from the console.
   */

  public void clear(){
    outputComponent.setText("");
    outputComponent.removeAll();
    outputComponent.removeLinks();
  }




  /**
   * Gets called when a tell by the given player is received. This method saves
   * the name of the teller so it can be later retrieved when F9 is hit.
   */

  public void tellReceived(String teller){
    tellers.removeElement(teller);
    tellers.insertElementAt(teller, 0);
    if (tellers.size() > getTellerRingSize())
      tellers.removeElementAt(tellers.size());
  }




  /**
   * Returns the size of the teller ring, the amount of last players who told us
   * something we traverse.
   */

  public int getTellerRingSize(){
    return Integer.parseInt(getProperty("teller-ring-size", "5"));
  }



  /**
   * Returns the nth (from the end) person who told us something via "tell",
   * "say" or "atell"  which went into this console. The index is 0 based. 
   * Sorry about the name of the method but I didn't think getColocutor()
   * was much better :-)
   */

  public String getTeller(int n){
    return (String)tellers.elementAt(n);
  }




  /**
   * Returns the amount of people who have told us anything so far.
   */

  public int getTellerCount(){
    return tellers.size();
  }




  /**
   * Returns the value of the userPlugin property with the given name. Returns
   * null if no property with the given name exists. This method simply delegates
   * to Plugin.getProperty(String)
   */

  public String getProperty(String propertyName){
    return userPlugin.getProperty(propertyName);
  }




  /**
   * Returns the value of the property with the given name. Returns the given 
   * default value if no property with the given name exists. This method simply
   * delegates to Plugin.getProperty(String, String)
   */

  public String getProperty(String propertyName, String defaultValue){
    return userPlugin.getProperty(propertyName, defaultValue);
  }


  


  /**
   * Looks up and returns a value for the property with the given name. Simply
   * delegates to Plugin.lookupProperty(String).
   */

  protected String lookupStringProperty(String propertyName){
    return userPlugin.lookupProperty(propertyName);
  }




  /**
   * Looks up an integer property with the given name. Follows the same procedure
   * as lookupStringProperty(String) and then parses the string as an integer.
   */

  protected Integer lookupIntegerProperty(String propertyName){
    String propertyValue = lookupStringProperty(propertyName);
    if (propertyValue == null)
      return null;

    return new Integer(propertyValue);
  }




  /**
   * Looks up a float property with the given name. Follows the same procedure
   * as lookupStringProperty(String) and then parses the string as a float.
   */

  protected Float lookupFloatProperty(String propertyName){
    String propertyValue = lookupStringProperty(propertyName);
    if (propertyValue == null)
      return null;

    return new Float(propertyValue);
  }





  /**
   * Looks up a boolean property with the given name. Follows the same procedure
   * as lookupStringProperty(String) and then parses the string as a boolean.
   */

  protected Boolean lookupBooleanProperty(String propertyName){
    String propertyValue = lookupStringProperty(propertyName);
    if (propertyValue == null)
      return null;

    return new Boolean(propertyValue);
  }





  /**
   * Looks up a color property with the given name. Follows the same procedure
   * as lookupStringProperty(String) and then parses the string as a color.
   */

  protected Color lookupColorProperty(String propertyName){
    String propertyValue = lookupStringProperty(propertyName);
    if (propertyValue == null)
      return null;

    return StringParser.parseColor(propertyValue);
  }





  /**
   * Returns the AttributeSet for the given type of output text. Due to a bug
   * in Swing, this method does not address the background color.
   */

  protected AttributeSet attributesForTextType(String textType){
    AttributeSet attributes = (AttributeSet)attributesCache.get(textType);
    if (attributes!=null)
      return attributes;

    SimpleAttributeSet mAttributes = new SimpleAttributeSet();

    String fontFamily = lookupStringProperty("font-family."+textType);
    Integer fontSize = lookupIntegerProperty("font-size."+textType);
    Boolean bold = lookupBooleanProperty("font-bold."+textType);
    Boolean italic = lookupBooleanProperty("font-italic."+textType);
    Boolean underlined = lookupBooleanProperty("font-underlined."+textType);
    Color foreground = lookupColorProperty("foreground."+textType);

    if (fontFamily!=null)
      StyleConstants.setFontFamily(mAttributes,fontFamily);
    if (fontSize!=null)
      StyleConstants.setFontSize(mAttributes,fontSize.intValue());
    if (bold!=null)
      StyleConstants.setBold(mAttributes,bold.booleanValue());
    if (italic!=null)
      StyleConstants.setItalic(mAttributes,italic.booleanValue());
    if (underlined!=null)
      StyleConstants.setUnderline(mAttributes,underlined.booleanValue());
    if (foreground!=null)
      StyleConstants.setForeground(mAttributes,foreground);
    attributesCache.put(textType,mAttributes);

    return mAttributes;
  }





  /**
   * Processes Key pressed events from the components we're registered as listeners for.
   * The default implementation is registered to listen to the input component.
   */

  public void keyPressed(KeyEvent evt){
    if ((evt.getSource()==inputComponent)){
      if (evt.getID()==KeyEvent.KEY_PRESSED){
        long modifiers = evt.getModifiers();
        if ((modifiers & KeyEvent.CTRL_MASK) != 0){
          JScrollBar vscrollbar = outputScrollPane.getVerticalScrollBar();
          Rectangle viewRect = outputScrollPane.getViewport().getViewRect();
          int value = vscrollbar.getValue();
          switch (evt.getKeyCode()){
            case KeyEvent.VK_UP:
              vscrollbar.setValue(value - outputComponent.getScrollableUnitIncrement(viewRect, SwingConstants.VERTICAL, -1));
              break;
            case KeyEvent.VK_DOWN:
              vscrollbar.setValue(value + outputComponent.getScrollableUnitIncrement(viewRect, SwingConstants.VERTICAL, +1));
              break;
            case KeyEvent.VK_PAGE_UP:
              vscrollbar.setValue(value - outputComponent.getScrollableBlockIncrement(viewRect, SwingConstants.VERTICAL, -1));
              break;
            case KeyEvent.VK_PAGE_DOWN:
              vscrollbar.setValue(value + outputComponent.getScrollableBlockIncrement(viewRect, SwingConstants.VERTICAL, -1));
              break;
            case KeyEvent.VK_HOME:
              vscrollbar.setValue(vscrollbar.getMinimum());
              break;
            case KeyEvent.VK_END:
              vscrollbar.setValue(vscrollbar.getMaximum() - vscrollbar.getVisibleAmount());
              break;
          }
        }
      }
    }

  }




  /**
   * Processes Key released events from the components we're registered as listeners for.
   * The default implementation is registered to listen to the output and to the
   * input component.
   */

  public void keyReleased(KeyEvent evt){}




  /**
   * Processes Key typed events from the components we're registered as listeners for.
   * The default implementation is registered to listen to the output and to the
   * input component.
   */

  public void keyTyped(KeyEvent evt){
    if (SwingUtilities.isDescendingFrom(evt.getComponent(), outputComponent)){
      if (evt.getKeyChar() != FixUtils.CHAR_UNDEFINED){

        // We request the focus in invokeLater because we want the key event
        // processing to finish while the correct component still has focus.
        SwingUtilities.invokeLater(new Runnable(){
          public void run(){
            requestDefaultFocus();
          }
        });


        KeyEvent fakeKeyPressedEvent = new KeyEvent(inputComponent, KeyEvent.KEY_PRESSED, evt.getWhen(), evt.getModifiers(), evt.getKeyCode(), evt.getKeyChar());
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(fakeKeyPressedEvent);

        KeyEvent fakeKeyReleasedEvent = new KeyEvent(inputComponent, KeyEvent.KEY_RELEASED, evt.getWhen(), evt.getModifiers(), evt.getKeyCode(), evt.getKeyChar());
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(fakeKeyReleasedEvent);

        KeyEvent fakeKeyTypedEvent = new KeyEvent(inputComponent, KeyEvent.KEY_TYPED, evt.getWhen(), evt.getModifiers(), KeyEvent.VK_UNDEFINED, evt.getKeyChar());
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(fakeKeyTypedEvent);
      }
    }
  }




  /**
   * Listens to components being added to the output component and its descendents
   * and registers as the key and container listener for all of them, because
   * we need to transfer the focus to the input field.
   */
   
  public void componentAdded(ContainerEvent evt){
    Container container = evt.getContainer();
    Component child = evt.getChild();

    if (SwingUtilities.isDescendingFrom(container,outputComponent)) // Check just in case.
      registerAsListenerToHierarchy(child);
  }




  /**
   * Listens to components being removed from the output component and its
   * descendents and unregisters as the key listener.
   */
  
  public void componentRemoved(ContainerEvent evt){
    Container container = evt.getContainer();
    Component child = evt.getChild();

    if (SwingUtilities.isDescendingFrom(container,outputComponent)) // Check just in case.
      unregisterAsListenerToHierarchy(child);
  }




  /**
   * Recursively registers <code>this</code> as the key listener with the given
   * component and of its descendants (recursively) if they are focus
   * traversable. If they are Containers, also registers as their
   * ContainerListener.
   */

  private void registerAsListenerToHierarchy(Component component){
    if (component.isFocusTraversable())
      component.addKeyListener(this);

    if (component instanceof Container){
      Container container = (Container)component;
      container.addContainerListener(this);
      int numChildren = container.getComponentCount();
      for (int i=0; i<numChildren; i++)
        registerAsListenerToHierarchy(container.getComponent(i));        
    }
  }



  
  /**
   * Does the opposite of <code>registerAsListenerToHierarchy(Component)</code>,
   * unregistering <code>this</code> as the key or container listener from the
   * given component and any of its children.
   */

  private void unregisterAsListenerToHierarchy(Component component){
    if (component.isFocusTraversable())
      component.removeKeyListener(this);

    if (component instanceof Container){
      Container container = (Container)component;
      container.removeContainerListener(this);
      int numChildren = container.getComponentCount();
      for (int i=0; i<numChildren; i++)
        unregisterAsListenerToHierarchy(container.getComponent(i));        
    }
  }


}
