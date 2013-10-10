/*
File: MainPanel.java ; This file is part of Twister.
Version: 2.006

Copyright (C) 2012-2013 , Luxoft

Authors: Andrei Costachi <acostachi@luxoft.com>
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import java.io.File; 
import java.io.PrintStream;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.DefaultKeyboardFocusManager;
import javax.swing.InputMap; 
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.net.URL;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import java.util.Iterator;

/*
 * twister main container
 * applet - true if starts from applet, false otherwie
 */
public class MainPanel extends JTabbedPane{
    private static final long serialVersionUID = 1L;
    public Panel1 p1;//suites tab
    public Panel2 p2;//masterxml tab
    public Panel4 p4;//configure tab
    private ClearCase p5;
    private boolean applet;
    
    public Panel2 getP2(){
        return p2;
    }
    
    public ClearCase getP5(){
        return p5;
    }

    public MainPanel(boolean applet){
        InputMap map = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP,
                                                    InputEvent.CTRL_MASK, false );
        map.put( keyStroke, "DoNothing" );
        RunnerRepository.introscreen.setStatus("Started Main initialization");
        RunnerRepository.introscreen.addPercent(0.035);
        RunnerRepository.introscreen.repaint();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.applet = applet;
        p1 = new Panel1("", applet,(int)screenSize.getWidth());
        p2 = new Panel2(applet);    
        p4 = new Panel4(); 
        p5 = new ClearCase(RunnerRepository.host,RunnerRepository.user,RunnerRepository.password);
//         p5 = new Panel5(1500,612);
        setBounds(0, 5, (int)screenSize.getWidth()-50, 672);
        addTab("Project", new ImageIcon(), p1);
        //addTab("Monitoring", p2);
        addTab("Reports", null);
        addTab("Configuration", p4); 
        //addTab("ClearCase", p5);
//         add("Network", new JScrollPane(p5));
//         new Thread(){
//             public void run(){
//                 SwingUtilities.invokeLater(new Runnable(){
//                     public void run(){
//                         try{while(p1.sc.g.getGraphics() == null){ 
//                                 try{Thread.sleep(100);}
//                                 catch(Exception e){
//                                     System.out.println("Thread interrupted at getting Graphics");}}
//                                 if(p1.suitaDetails.isEnabled())askForFile();}
//                          catch(NullPointerException e){e.printStackTrace();}}});}}.start();
        
        /*
         * if it is applet
         * listen for clicks on reports tab
         * and open url when pressed
         */
        if(applet){
            addChangeListener(new ChangeListener(){
                public void stateChanged(ChangeEvent e){
                    if(getSelectedIndex()==1){
                        try{RunnerRepository.window.container.getAppletContext().showDocument(
                                                    new URL("http://"+RunnerRepository.host+":"+
                                                    RunnerRepository.getCentralEnginePort()+
                                                    "/report/index/"+RunnerRepository.getUser()), "_blank");}
                        catch(Exception ex){ex.printStackTrace();}
                        setSelectedIndex(0);}}});}
        RunnerRepository.introscreen.setStatus("Finished Main initialization");
        RunnerRepository.introscreen.addPercent(0.035);
        RunnerRepository.introscreen.repaint();
    }
 
    /*
     * ask user for suite file
     */
//     public void askForFile(){       
//         File usersdirectory = new File(RunnerRepository.getUsersDirectory());
//         String users[] = new String[usersdirectory.list().length + 1];
//         System.arraycopy(usersdirectory.list(), 0, users, 0,
//                             usersdirectory.list().length);
//         users[users.length - 1] = "New File";
//         JComboBox combo = new JComboBox(users);
//         int resp = (Integer)CustomDialog.showDialog(combo,
//                                                     JOptionPane.INFORMATION_MESSAGE,
//                                                     JOptionPane.OK_CANCEL_OPTION,p1,
//                                                     "Project File",null);
//         if(resp==JOptionPane.OK_OPTION){
//             String user = combo.getSelectedItem().toString();
//             if(user.equals("New File")){
//                 user = CustomDialog.showInputDialog(JOptionPane.QUESTION_MESSAGE,
//                                                     JOptionPane.OK_CANCEL_OPTION, p1,
//                                                     "File Name", "Please enter file name").
//                                                     toUpperCase();
//                 if(!user.equals("NULL")){
//                     (new XMLBuilder(RunnerRepository.getSuite())).writeXMLFile((new StringBuilder()).
//                                         append(RunnerRepository.getUsersDirectory()).append(RunnerRepository.
//                                         getBar()).append(user).append(".XML").toString(),false,false);
//                     p1.sc.g.setUser((new StringBuilder()).append(RunnerRepository.getUsersDirectory()).
//                                         append(RunnerRepository.getBar()).append(user).append(".XML").
//                                         toString());
//                     p1.sc.g.printXML(p1.sc.g.getUser(),false,false,false);}}
//             else{
//                 p1.sc.g.setUser((new StringBuilder()).append(RunnerRepository.getUsersDirectory()).
//                                         append(RunnerRepository.getBar()).append(user).toString());
//                 p1.sc.g.parseXML(new File((new StringBuilder()).append(RunnerRepository.getUsersDirectory()).
//                                         append(RunnerRepository.getBar()).append(user).toString()));}}}
        
    public void saveUserXML(){
        if(!p1.sc.g.getUser().equals("")){
            p1.sc.g.printXML(p1.sc.g.getUser(), false,false,false,false,false,"",false,null);}}}
