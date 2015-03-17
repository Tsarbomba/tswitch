package guiModule;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import controlModule.TSwitchController;

public class TSwitchGUI extends JFrame {

    /**
     */
    private static final long serialVersionUID = 1L;
    private static final int CHAT_MAX_CHARACTERS = 10000;
    private final TSwitchController control;
    private final TSwitchGUI frame = this;
    private final static String HTML_HEAD = "<html>";
    private final static String HTML_FOOT = "</font></b></html>";
    private final HashMap<String, ImageIcon> iconCache = new HashMap<String, ImageIcon>();
    private final JMenuBar menuBar = new JMenuBar();
    private final JLabel viewersLabel, gameLabel, statusLabel, logoImg;
    private final JTextField inputField;
    private final JComboBox<Object> comboBox;
    private final JTextPane chatArea;
    private final JTextArea logArea;
    private final JButton sendInputBtn, btnOpen;
    private final JScrollPane chatScroller, logScroller;
    private final JScrollBar vChatBar;
    private final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    private final StyledDocument chatAreaDoc;
    private final SimpleAttributeSet keyWordSender = new SimpleAttributeSet();
    private final SimpleAttributeSet keyWordMessage = new SimpleAttributeSet();
    private final DateFormat dateFormat = new SimpleDateFormat(
            "[yy-MM-dd HH:mm:ss]");
    private final Date date = new Date();
    private ActionListener comboboxListener;
    private Matcher emotesRegXpMatcher;
    private HashMap<String, SimpleAttributeSet> emoteIconStyles = new HashMap<String, SimpleAttributeSet>();

    /**
     * Create the frame.
     * 
     * @throws Exception
     */
    public TSwitchGUI(final TSwitchController control) throws Exception {
        this.control = control;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 386, 380);
        setResizable(false);
        setContentPane(tabPane);
        setTitle("TSwitch GUI");

        final JPanel infoTab = new JPanel(new MigLayout("insets 5"));
        final JPanel chatTab = new JPanel(new MigLayout("insets 5",
                "[grow,fill]"));

        final JPanel logTab = new JPanel(new MigLayout("insets 5",
                "[grow,fill]"));

        tabPane.addTab("Stream", infoTab);
        tabPane.addTab("Chat", chatTab);
        final int chatTabIndex = 1;
        tabPane.addTab("Log", logTab);

        iconCache.put("default", new ImageIcon(new BufferedImage(150, 150,
                BufferedImage.TYPE_INT_ARGB)));

        gameLabel = new JLabel();
        statusLabel = new JLabel();
        viewersLabel = new JLabel();

        logoImg = new JLabel(iconCache.get("default"));
        logoImg.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        btnOpen = new JButton("Watch");
        comboBox = new JComboBox<>();
        inputField = new JTextField(20);
        sendInputBtn = new JButton("Send");
        chatArea = new JTextPane();
        chatArea.setEditorKit(new WrapEditorKit());
        chatArea.setEditable(false);
        chatArea.setText("");
        chatScroller = new JScrollPane(chatArea);
        vChatBar = chatScroller.getVerticalScrollBar();

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setWrapStyleWord(true);
        logArea.setLineWrap(true);
        logArea.setFont(logArea.getFont().deriveFont(11f));
        logScroller = new JScrollPane(logArea);

        statusLabel.setVerticalAlignment(SwingConstants.TOP);
        gameLabel.setVerticalAlignment(SwingConstants.TOP);
        viewersLabel.setVerticalAlignment(SwingConstants.TOP);

        // set the init values of the labels.
        setLabelData("N/A", "N/A", "N/A", null);

        comboBox.setRenderer(new IconListCellRenderer());

        chatAreaDoc = chatArea.getStyledDocument();
        chatScroller
                .setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        chatScroller
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // chatScroller.getVerticalScrollBar().setUnitIncrement(16);
        ((DefaultCaret) chatArea.getCaret())
                .setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        // makes sure scrollbar autoscrolls.
        // new SmartScroller(chatScroller);

        // add all the components to the frame.
        infoTab.add(logoImg, "pushx, wrap,center,w 150!,h 150!");
        infoTab.add(statusLabel, "wrap,grow");
        infoTab.add(gameLabel, "wrap,grow");
        infoTab.add(viewersLabel, "wrap,grow");
        infoTab.add(comboBox, "w 150!,span,center,split 2");
        infoTab.add(btnOpen);

        chatTab.add(chatScroller, "grow,wrap,pushy");
        chatTab.add(inputField, "growx,split 2");
        chatTab.add(sendInputBtn);

        logTab.add(logScroller, "grow,push");
        buildMenu();

        tabPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent changeEvent) {
                // reset the scroll to the bottom of chat if we switched to the
                // chatTab.
                if (tabPane.getSelectedIndex() == chatTabIndex) {
                    vChatBar.setValue(vChatBar.getMaximum());
                }
            }
        });

        sendInputBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                processInputText();
            }

        });

        inputField.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    processInputText();
                }
            }

        });

    }

    private void buildMenu() {
        // setup menus
        final JMenu confMenu = new JMenu("Configure");
        final JMenu helpMenu = new JMenu("Help");
        menuBar.add(confMenu);
        menuBar.add(helpMenu);
        final JMenuItem aboutMenuItem = new JMenuItem("About");
        helpMenu.add(aboutMenuItem);
        final JMenu menuItemAddChannel = new JMenu("Add a Channel");
        final JMenuItem menuItemAddChannelManually = new JMenuItem(
                "Specify (Manual)");
        final JMenuItem menuItemAddChannelByFollow = new JMenuItem(
                "Followed (Auto)");
        final JMenuItem menuItemRemChannel = new JMenuItem("Remove a Channel");
        final JMenuItem menuItemChangeCred = new JMenuItem("Change Credentials");
        menuItemAddChannel.add(menuItemAddChannelManually);
        menuItemAddChannel.add(menuItemAddChannelByFollow);
        confMenu.add(menuItemAddChannel);
        confMenu.add(menuItemRemChannel);
        confMenu.add(menuItemChangeCred);
        final JMenu qualitySubMenu = new JMenu("Stream Quality");
        confMenu.add(qualitySubMenu);

        final ButtonGroup group = new ButtonGroup();
        final JRadioButtonMenuItem rbMenuItem1 = new JRadioButtonMenuItem(
                "Source");
        rbMenuItem1.setActionCommand("source");

        final JRadioButtonMenuItem rbMenuItem2 = new JRadioButtonMenuItem(
                "High");
        rbMenuItem2.setActionCommand("high");

        final JRadioButtonMenuItem rbMenuItem3 = new JRadioButtonMenuItem(
                "Medium");
        rbMenuItem3.setActionCommand("medium");

        final JRadioButtonMenuItem rbMenuItem4 = new JRadioButtonMenuItem("Low");
        rbMenuItem4.setActionCommand("low");

        final JRadioButtonMenuItem rbMenuItem5 = new JRadioButtonMenuItem(
                "Mobile");
        rbMenuItem5.setActionCommand("mobile");

        group.add(rbMenuItem1);
        group.add(rbMenuItem2);
        group.add(rbMenuItem3);
        group.add(rbMenuItem4);
        group.add(rbMenuItem5);
        qualitySubMenu.add(rbMenuItem1);
        qualitySubMenu.add(rbMenuItem2);
        qualitySubMenu.add(rbMenuItem3);
        qualitySubMenu.add(rbMenuItem4);
        qualitySubMenu.add(rbMenuItem5);
        rbMenuItem1.setSelected(true);
        setJMenuBar(menuBar);

        menuItemAddChannelManually.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final String s = JOptionPane
                        .showInputDialog(
                                frame,
                                "Specify the channel name below:\n"
                                        + "(Multiple channels can be added by using a comma.\n"
                                        + " Ex. channel1,channel2,channel3..)",
                                "Add Channel", JOptionPane.PLAIN_MESSAGE);

                // If a string was returned we add the channel.
                if (s != null && !s.trim().isEmpty()) {
                    control.addChannel(s);
                }
            }

        });
        menuItemAddChannelByFollow.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final int reply = JOptionPane.showConfirmDialog(null,
                        "Are you sure that you wish to add the channels followed by "
                                + control.getUsername() + "?", null,
                        JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION) {
                    control.addFollowedChannels();
                }
            }

        });
        menuItemRemChannel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final Object[] removeables = new Object[comboBox.getModel()
                        .getSize()];
                final ComboBoxModel<Object> cBoxModel = comboBox.getModel();
                for (int i = 0; i < comboBox.getModel().getSize(); i++) {
                    removeables[i] = ((Object[]) cBoxModel.getElementAt(i))[0];
                }
                final String s = (String) JOptionPane.showInputDialog(frame,
                        "Choose the channel to be removed below:\n",
                        "Remove Channel", JOptionPane.QUESTION_MESSAGE, null,
                        removeables, removeables[0]);

                // If a string was returned user gave a channel to remove.
                if (s != null) {
                    control.removeChannel(s);
                }
            }

        });
        menuItemChangeCred.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final String[] creds = queryLoginCredentials(false, false);
                if (creds[0] != null) {
                    control.setCredentials(creds[0], creds[1]);
                }
            }

        });

        aboutMenuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                final JLabel aboutTxt = new JLabel(
                        "<html><body>TwitchSwitch BETA<br>A Livestreamer"
                                + " and Twitch.tv Frontend<br>Created by "
                                + "Emanuel Y. Lindgren<br>(twitchswitch.contact@gmail.com)<br></body></html>");
                aboutTxt.setHorizontalAlignment(SwingConstants.CENTER);
                aboutTxt.setVerticalAlignment(SwingConstants.CENTER);

                JOptionPane.showMessageDialog(frame, aboutTxt, "About",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        });

        final ActionListener radiobtnQualityaL = new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent arg0) {
                control.setPreferredStreamQuality(arg0.getActionCommand());
            }
        };

        rbMenuItem1.addActionListener(radiobtnQualityaL);
        rbMenuItem2.addActionListener(radiobtnQualityaL);
        rbMenuItem3.addActionListener(radiobtnQualityaL);
        rbMenuItem4.addActionListener(radiobtnQualityaL);
        rbMenuItem5.addActionListener(radiobtnQualityaL);
    }

    public void chatMessage(final String sender, final String message) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {

                if (sender.equalsIgnoreCase(control.getUsername())) {
                    // this is our own message.
                    StyleConstants.setForeground(keyWordSender,
                            ChatColors.DARK_BLUE);
                    StyleConstants.setForeground(keyWordMessage,
                            ChatColors.BLACK);

                } else if (sender.equalsIgnoreCase("jtv")) {
                    // this is a twitch server message.
                    StyleConstants.setForeground(keyWordSender,
                            ChatColors.DARK_GREEN);
                    StyleConstants.setForeground(keyWordMessage,
                            ChatColors.DARK_GREEN);

                } else {
                    // this is a regular user message.
                    StyleConstants.setForeground(keyWordSender, ChatColors.RED);
                    StyleConstants.setForeground(keyWordMessage,
                            ChatColors.BLACK);
                }

                StyleConstants.setBold(keyWordSender, true);

                try {
                    final String senderString = sender + ": ";
                    final String messageString = message + "\n";

                    // remove oldest text if chat buffer gets too large.
                    clampChatBuffer(
                            senderString.length() + messageString.length(),
                            chatAreaDoc.getLength());

                    // Determine if vertical scrollbar is at bottom(For
                    // autoscroll).
                    final boolean atEnd = vChatBar.getMaximum() == vChatBar
                            .getValue() + vChatBar.getVisibleAmount();
                    // insert messages
                    chatAreaDoc.insertString(chatAreaDoc.getLength(),
                            senderString, keyWordSender);

                    // check for and insert any detected emotes.
                    if (emotesRegXpMatcher != null) {
                        emotesRegXpMatcher.reset(messageString);
                        int parsedEIndex = 0;
                        while (emotesRegXpMatcher.find()) {

                            final int emoteSIndex = emotesRegXpMatcher.start();
                            final int emoteEIndex = emotesRegXpMatcher.end();
                            // insert any text before the emote.
                            chatAreaDoc.insertString(chatAreaDoc.getLength(),
                                    messageString.substring(parsedEIndex,
                                            emoteSIndex), keyWordMessage);
                            // insert emote icon

                            final String emoteText = messageString.substring(
                                    emoteSIndex, emoteEIndex);
                            chatAreaDoc.insertString(chatAreaDoc.getLength(),
                                    "#", emoteIconStyles.get(emoteText));
                            parsedEIndex = emoteEIndex;

                        }
                        // inserts any text left after the last emote OR all the
                        // text if no emotes were found at all.
                        if (parsedEIndex < messageString.length()) {
                            chatAreaDoc.insertString(chatAreaDoc.getLength(),
                                    messageString.substring(parsedEIndex),
                                    keyWordMessage);
                        }

                    } else {
                        // emotes disabled. So just regular text.
                        chatAreaDoc.insertString(chatAreaDoc.getLength(),
                                messageString, keyWordMessage);
                    }

                    // Autoscroll to new bottom if scroll position was at bottom
                    // before insert OR if the tab is not selected. Needs to be
                    // queued to ensure maximum has been updated since insert.

                    if (atEnd) {
                        EventQueue.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                EventQueue.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        vChatBar.setValue(vChatBar.getMaximum());
                                    }
                                });
                            }
                        });
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void clampChatBuffer(final int incomingDataSize,
            final int currentDataSize) {
        /*
         * To limit the chat buffer we remove old messages. By removing per
         * paragraph we can remove complete messages instead of per character
         * which would leave messages partially removed. We remove paragraphs
         * until their combined character count is equal or greater then the
         * message to be inserted contains.
         */
        final int overSize = currentDataSize + incomingDataSize
                - CHAT_MAX_CHARACTERS;

        if (overSize > 0) {
            int trimOffset = 0;
            while (overSize - trimOffset > 0) {
                trimOffset = chatAreaDoc.getParagraphElement(trimOffset + 1)
                        .getEndOffset();
            }
            try {
                chatAreaDoc.remove(0, trimOffset);
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    public void logMessage(final String sender, final String msg) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                date.setTime(System.currentTimeMillis());
                logArea.append(dateFormat.format(date) + " " + sender + ": "
                        + msg + "\n");
            }
        });
    }

    private void processInputText() {
        final String sendText = inputField.getText();
        if (sendText != null && !sendText.trim().isEmpty()) {
            control.sendMessage(sendText);
            inputField.setText("");
            chatMessage(control.getUsername(), sendText);
        }
    }

    public void addStreamSelectionListener(final ActionListener al) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                comboboxListener = al;
                comboBox.addActionListener(al);
            }
        });

    }

    public void addStreamOpenListener(final ActionListener al) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                btnOpen.addActionListener(al);
            }
        });
    }

    public void setLabelData(final String status, final String game,
            final String viewers, final String logourl) {

        statusLabel.setText(HTML_HEAD
                + "<b><font color=green>TITLE:</font><br><font size=\"2\">"
                + status + HTML_FOOT);
        gameLabel.setText(HTML_HEAD
                + "<b><font color=green>PLAYING:</font><br><font size=\"2\">"
                + game + HTML_FOOT);
        viewersLabel.setText(HTML_HEAD
                + "<b><font color=green>VIEWERS:</font><br><font size=\"2\">"
                + viewers + HTML_FOOT);

        URL url = null;
        BufferedImage image = null;
        if (logourl == null || logourl.isEmpty()) {
            logoImg.setIcon(iconCache.get("default"));
        } else {
            ImageIcon icon = iconCache.get(logourl);
            if (icon != null) {
                logoImg.setIcon(icon);
            } else {
                try {
                    url = new URL(logourl);
                    image = ImageIO.read(url);
                    icon = new ImageIcon(image.getScaledInstance(150, 150,
                            Image.SCALE_SMOOTH));
                    logoImg.setIcon(icon);
                    iconCache.put(logourl, icon);
                } catch (final Exception e) {
                    logoImg.setIcon(iconCache.get("default"));
                }
            }
        }
    }

    public void refreshStreamStatus(final LinkedList<Object> streamsData) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                int selIndex = comboBox.getSelectedIndex();
                final DefaultComboBoxModel<Object> newModel = new DefaultComboBoxModel<Object>();
                for (final Object streamdata : streamsData) {
                    newModel.addElement(streamdata);
                }

                // if no prev. selection done, select first element.
                if (selIndex == -1 && !streamsData.isEmpty()) {
                    selIndex = 0;
                    comboBox.setModel(newModel);
                    comboBox.setSelectedIndex(selIndex);
                } else {
                    // workaround to prevent action events to trigger listeners
                    // when we reselect previous selection.
                    comboBox.removeActionListener(comboboxListener);
                    // set new model with data and reselect prev. index.
                    comboBox.setModel(newModel);
                    // We must verify that selIndex is valid for the new model.
                    // If not pick a valid one.
                    if (selIndex >= newModel.getSize()) {
                        selIndex = newModel.getSize() - 1;
                    }
                    comboBox.setSelectedIndex(selIndex);

                    // add listener back.
                    comboBox.addActionListener(comboboxListener);
                }
            }
        });
    }

    public String getSelectedStreamName() {
        final Object[] item = (Object[]) comboBox.getSelectedItem();
        return item[0].toString();

    }

    public void setChatChannelName(String name) {
        if (name.length() > 10) {
            name = name.substring(0, 10) + "..";
        }
        tabPane.setTitleAt(1, "Chat - " + name);
        tabPane.setTitleAt(0, "Stream - " + name);
    }

    public void clearChat() {
        chatArea.setText("");
    }

    public String[] queryLoginCredentials(final boolean showChannelInfo,
            final boolean required) {
        String username = null;
        String oAuth = null;
        while (username == null || username.trim().isEmpty()) {
            username = JOptionPane.showInputDialog(frame,
                    "Specify your Twitch.tv account username:\n", "Username",
                    JOptionPane.PLAIN_MESSAGE);
            if (!required) {
                break;
            }
        }
        if (!required && (username == null || username.trim().isEmpty())) {
            // cancelled the optional username prompt so we can just return.
            return new String[] { null, null };
        }
        while (oAuth == null || oAuth.trim().isEmpty()) {
            oAuth = JOptionPane
                    .showInputDialog(
                            frame,
                            "Specify your Twitch.tv OAuth:\nOAuth is generated at: http://twitchapps.com/tmi/ ",
                            "oAuth", JOptionPane.PLAIN_MESSAGE);
            if (!required) {
                break;
            }
        }
        if (showChannelInfo) {
            JOptionPane.showMessageDialog(frame,
                    "To add channels use the \"Configure\" menu option.",
                    "Adding channels", JOptionPane.INFORMATION_MESSAGE);
        }
        return new String[] { username, oAuth };

    }

    public void setChatEmoteIcons(
            final ConcurrentHashMap<String, String> emoteSet) {
        final StringBuffer regexPatternString = new StringBuffer();
        final HashMap<String, SimpleAttributeSet> emotes = new HashMap<String, SimpleAttributeSet>();
        for (final Entry<String, String> emoteData : emoteSet.entrySet()) {
            regexPatternString.append("\\b" + emoteData.getKey() + "\\b|");
            // the emote names are unique so if we already an attrib for the
            // specific emote we can just copy it from the previous emote set.
            if (emoteIconStyles.containsKey(emoteData.getKey())) {
                emotes.put(emoteData.getKey(),
                        emoteIconStyles.get(emoteData.getKey()));
            } else {
                final SimpleAttributeSet emoteAttrib = new SimpleAttributeSet();
                StyleConstants.setIcon(emoteAttrib,
                        createImageIcon(emoteData.getValue()));
                emotes.put(emoteData.getKey(), emoteAttrib);
            }
        }
        // remove last trailing | from pattern.
        regexPatternString.deleteCharAt(regexPatternString.length() - 1);

        final Pattern emotesPattern = Pattern.compile(regexPatternString
                .toString());
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                // update the emoteMatcher and global emoteicon set. We force
                // this to be done on the EDT to be sure we're not swapping
                // matcher mid-loop.
                emoteIconStyles = emotes;
                emotesRegXpMatcher = emotesPattern.matcher("");
            }
        });

    }

    private ImageIcon createImageIcon(final String sURL) {

        try {
            final URL url = new URL(sURL);
            final BufferedImage img = ImageIO.read(url);
            return new ImageIcon(img);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
