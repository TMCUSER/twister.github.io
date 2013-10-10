/*
File: ConfigFiles.java ; This file is part of Twister.
Version: 2.012

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
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import org.apache.commons.vfs.FileObject;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import org.w3c.dom.Element;
import java.io.File;
import java.io.FileInputStream;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Comment;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.awt.Component;
import java.awt.HeadlessException;
import javax.swing.JDialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Container;
import java.awt.Color;
import javax.swing.plaf.ComponentUI;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import java.awt.Font;
import javax.swing.border.TitledBorder;
import javax.swing.JTextArea;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.border.BevelBorder;
import java.awt.BorderLayout;
import javax.swing.SwingUtilities;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import java.util.Arrays;
import com.twister.MySftpBrowser;
import com.twister.CustomDialog;
import java.io.BufferedWriter;
import java.io.FileWriter;

public class ConfigFiles extends JPanel{
    public static JTextField ttcpath,tMasterXML,tUsers,tepid,
                             tSuites,
                             tlog,trunning,
                             tdebug,tsummary,tinfo,tcli,tdbfile,
                             temailfile,tceport,
                             libpath,tsecondarylog,
                             testconfigpath,
                             tglobalsfile;
    JPanel paths;
    private static JCheckBox logsenabled  = new JCheckBox("Enabled");
    //public JPanel emailpanel;                    
    
    public ConfigFiles(Dimension screensize){  
//         initializeFileBrowser();
        paths = new JPanel();
        paths.setBackground(Color.WHITE);
        //paths.setBorder(BorderFactory.createTitledBorder("Paths"));
        paths.setLayout(null);
        paths.setPreferredSize(new Dimension(930,1144));
        paths.setSize(new Dimension(930,1144));
        paths.setMinimumSize(new Dimension(930,1144));
        paths.setMaximumSize(new Dimension(930,1144));
        //paths.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        setLayout(null);
        ttcpath = new JTextField();
        addPanel("TestCase Source Path",
                "Master directory with the test cases that can"+
                " be run by the framework",
                ttcpath,RunnerRepository.TESTSUITEPATH,10,true,null);
        tMasterXML = new JTextField();
        tUsers = new JTextField();
        
        addPanel("Projects Path",
                "Location of projects XML files",tUsers,
                RunnerRepository.REMOTEUSERSDIRECTORY,83,true,null);
                
        tSuites = new JTextField();
        addPanel("Predefined Suites Path",
                "Location of predefined suites",tSuites,
                RunnerRepository.PREDEFINEDSUITES,156,true,null);
                
        testconfigpath = new JTextField();
        addPanel("Test Configuration Path",
                "Test Configuration path",testconfigpath,
                RunnerRepository.TESTCONFIGPATH,303,true,null);
                
        tepid = new JTextField();
        addPanel("EP name File","Location of the file that contains"+
        " the Ep name list",
                tepid,RunnerRepository.REMOTEEPIDDIR,595,true,null);
        tlog = new JTextField();
        addPanel("Logs Path","Location of the directory that stores the most recent log files."+
                             " The files are re-used each Run.",
                tlog,RunnerRepository.LOGSPATH,667,true,null);
        tsecondarylog = new JTextField(); 
        
        JPanel p = addPanel("Secondary Logs Path","Location of the directory that archives copies of the most recent log files, with"+
                                                  " original file names appended with <.epoch time>",
                tsecondarylog,RunnerRepository.SECONDARYLOGSPATH,930,true,null);
        logsenabled.setSelected(Boolean.parseBoolean(RunnerRepository.PATHENABLED));
        logsenabled.setBackground(Color.WHITE);
        p.add(logsenabled);
                
        JPanel p7 = new JPanel();
        p7.setBackground(Color.WHITE);
        TitledBorder border7 = BorderFactory.createTitledBorder("Log Files");
        border7.setTitleFont(new Font("Arial",Font.PLAIN,14));
        border7.setBorder(BorderFactory.createLineBorder(new Color(150,150,150), 1));
        p7.setBorder(border7);
        p7.setLayout(new BoxLayout(p7, BoxLayout.Y_AXIS));    
        p7.setBounds(80,740,800,190);
        paths.add(p7);
        JTextArea log2 = new JTextArea("All the log files that will be monitored");
        log2.setWrapStyleWord(true);
        log2.setLineWrap(true);
        log2.setEditable(false);        
        log2.setCursor(null);  
        log2.setOpaque(false);  
        log2.setFocusable(false);   
        log2.setBorder(null);
        log2.setFont(new Font("Arial",Font.PLAIN,12));
        log2.setBackground(getBackground());
        log2.setMaximumSize(new Dimension(170,25));
        log2.setPreferredSize(new Dimension(170,25));   
        JPanel p71 = new JPanel();
        p71.setBackground(Color.WHITE);
        p71.setLayout(new GridLayout());
        p71.setMaximumSize(new Dimension(700,13));
        p71.setPreferredSize(new Dimension(700,13));
        p71.add(log2);
        JPanel p72 = new JPanel();
        p72.setBackground(Color.WHITE);
        p72.setLayout(new BoxLayout(p72, BoxLayout.Y_AXIS));
        trunning = new JTextField();
        p72.add(addField(trunning,"Running: ",0));
        tdebug = new JTextField();
        p72.add(addField(tdebug,"Debug: ",1));
        tsummary = new JTextField();
        p72.add(addField(tsummary,"Summary: ",2));
        tinfo = new JTextField();
        p72.add(addField(tinfo,"Info: ",3));
        tcli = new JTextField();
        p72.add(addField(tcli,"Cli: ",4));        
        p7.add(p71);
        p7.add(p72);
        libpath = new JTextField();
        
        addPanel("Library path",
                "Secondary user library path",libpath,
                RunnerRepository.REMOTELIBRARY,229,true,null);
   
        JPanel p8 = new JPanel();
        p8.setBackground(Color.WHITE);
        TitledBorder border8 = BorderFactory.createTitledBorder("File");
        border8.setTitleFont(new Font("Arial",Font.PLAIN,14));
        border8.setBorder(BorderFactory.createLineBorder(new Color(150,150,150), 1));
        p8.setBorder(border8);
        p8.setLayout(null);    
        p8.setBounds(80,1076,800,50);
        if(PermissionValidator.canChangeFWM()){
            paths.add(p8);
        }
        JButton save = new JButton("Save");
        save.setToolTipText("Save and automatically load config");
        save.setBounds(490,20,70,20);
        save.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev){
                saveXML(false,"fwmconfig");
                loadConfig("fwmconfig.xml");
            }});
        p8.add(save);
//         if(!PermissionValidator.canChangeFWM()){
//             save.setEnabled(false);
//         }
        JButton saveas = new JButton("Save as");
        saveas.setBounds(570,20,90,20);
        saveas.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev){
                String filename = CustomDialog.showInputDialog(JOptionPane.QUESTION_MESSAGE,
                                             JOptionPane.OK_CANCEL_OPTION
                                             ,ConfigFiles.this,
                                             "File Name", "Please enter file name");
                if(!filename.equals("NULL")){
                    saveXML(false,filename);
                }
            }});
        p8.add(saveas);
        
        final JButton loadXML = new JButton("Load Config");
        loadXML.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev){  
                try{
                    String [] configs =RunnerRepository.getRemoteFolderContent(RunnerRepository.USERHOME+"/twister/config/");
                    JComboBox combo = new JComboBox(configs);
                    int resp = (Integer)CustomDialog.showDialog(combo,JOptionPane.INFORMATION_MESSAGE,
                                                                JOptionPane.OK_CANCEL_OPTION,
                                                                ConfigFiles.this,"Config",null);
                    final String config ;                                                                
                    if(resp==JOptionPane.OK_OPTION) config = combo.getSelectedItem().toString();
                    else config = null;
                    if(config!=null){ 
                        new Thread(){
                            public void run(){
                                setEnabledTabs(false);
                                JFrame progress = new JFrame();
                                progress.setAlwaysOnTop(true);
                                progress.setLocation((int)loadXML.getLocationOnScreen().getX(),
                                                     (int)loadXML.getLocationOnScreen().getY());
                                progress.setUndecorated(true);
                                JProgressBar bar = new JProgressBar();
                                bar.setIndeterminate(true);
                                progress.add(bar);
                                progress.pack();
                                progress.setVisible(true);
                                loadConfig(config);
                                progress.dispose();
                                setEnabledTabs(true);}}.start();}}
                catch(Exception e){e.printStackTrace();}}});
        loadXML.setBounds(670,20,120,20);
        p8.add(loadXML);
//         if(!PermissionValidator.canChangeFWM()){
//             loadXML.setEnabled(false);
//         }
        
                    
                    
                    
                    
        tdbfile = new JTextField();
        addPanel("Database XML path","File location for database configuration",    
                tdbfile,RunnerRepository.REMOTEDATABASECONFIGPATH+RunnerRepository.REMOTEDATABASECONFIGFILE,
                375,true,null);
        temailfile = new JTextField();
//         emailpanel = (JPanel)
        addPanel("Email XML path","File location for email configuration",temailfile,
                RunnerRepository.REMOTEEMAILCONFIGPATH+RunnerRepository.REMOTEEMAILCONFIGFILE,448,true,null).getParent();
        //paths.remove(emailpanel);
        
//         emailpanel.setBounds(360,440,350,100);
//         RunnerRepository.window.mainpanel.p4.getEmails().add(emailpanel);
        
               
        tglobalsfile = new JTextField();
        addPanel("Globals XML file","File location for globals parameters",tglobalsfile,
                RunnerRepository.GLOBALSREMOTEFILE,521,true,null);         
                
        tceport = new JTextField();
        addPanel("Central Engine Port","Central Engine port",
                tceport,RunnerRepository.getCentralEnginePort(),1003,false,null);                
//         traPort = new JTextField();
//         addPanel("Resource Allocator Port","Resource Allocator Port",
//                 traPort,RunnerRepository.getResourceAllocatorPort(),808,false,null);                
//         thttpPort = new JTextField();
//         addPanel("HTTP Server Port","HTTP Server Port",thttpPort,
//                 RunnerRepository.getHTTPServerPort(),740,false,null);
        
        //paths.add(loadXML);
        
        
        if(!PermissionValidator.canChangeFWM()){
            ttcpath.setEnabled(false);
            tMasterXML.setEnabled(false);
            tUsers.setEnabled(false);
            tepid.setEnabled(false);
            tSuites.setEnabled(false);
            tlog.setEnabled(false);
            trunning.setEnabled(false);
            tdebug.setEnabled(false);
            tsummary.setEnabled(false);
            tinfo.setEnabled(false);
            tcli.setEnabled(false);
            tdbfile.setEnabled(false);
            temailfile.setEnabled(false);
            tceport.setEnabled(false);
            libpath.setEnabled(false);
            tsecondarylog.setEnabled(false);
            testconfigpath.setEnabled(false);
            tglobalsfile.setEnabled(false);
            logsenabled.setEnabled(false);
        }
        
    }
        
    public void setEnabledTabs(boolean enable){
        int nr = RunnerRepository.window.mainpanel.getTabCount();
        for(int i=0;i<nr;i++){
            if(i!=3)RunnerRepository.window.mainpanel.setEnabledAt(i, enable);}}
        
    public void loadConfig(String config){
        try{                    
            String content = RunnerRepository.getRemoteFileContent(RunnerRepository.USERHOME+"/twister/config/"+config);
            File theone = new File(RunnerRepository.temp+RunnerRepository.getBar()+
                                    "Twister"+RunnerRepository.getBar()+"config"+
                                    RunnerRepository.getBar()+config);
            try{BufferedWriter writer = new BufferedWriter(new FileWriter(theone));
                writer.write(content);
                writer.close();
            } catch(Exception e){
                e.printStackTrace();
            }                      
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc=null;
            try{doc = db.parse(theone);}
            catch(Exception e){
                System.out.println(theone.getCanonicalPath()+
                                   "is corrup or not a valid XML");}
            if(doc!=null){
                doc.getDocumentElement().normalize();                
                NodeList nodeLst = doc.getElementsByTagName("FileType");
                if(nodeLst.getLength()>0){
                    Node fstNode = nodeLst.item(0);
                    Element fstElmnt = (Element)fstNode;
                    NodeList fstNm = fstElmnt.getChildNodes();
                    if(fstNm.item(0).getNodeValue().toString().toLowerCase().equals("config")){
                        FileInputStream in = new FileInputStream(theone);
                        RunnerRepository.uploadRemoteFile(RunnerRepository.USERHOME+"/twister/config/", in, "fwmconfig.xml");
                        RunnerRepository.emptyTestRunnerRepository();
                        RunnerRepository.emptyLogs();
                        File dir = new File(RunnerRepository.getUsersDirectory());
                        String[] children = dir.list();
                        for (int i=0; i<children.length; i++){new File(dir, children[i]).delete();}
                        RunnerRepository.parseConfig();
                        RunnerRepository.window.mainpanel.getP2().init(RunnerRepository.applet);
                        RunnerRepository.window.mainpanel.p1.ep.refreshStructure();
                        RunnerRepository.window.mainpanel.p1.lp.refreshStructure();
                        RunnerRepository.window.mainpanel.p4.getDBConfig().refresh();
                        RunnerRepository.window.mainpanel.p4.getGlobals().refresh();
                        RunnerRepository.resetDBConf(RunnerRepository.REMOTEDATABASECONFIGFILE,true);
                        RunnerRepository.resetEmailConf(RunnerRepository.REMOTEEMAILCONFIGFILE,true);
                        RunnerRepository.initializeRPC();
                        RunnerRepository.populatePluginsVariables();
                        tdbfile.setText(RunnerRepository.REMOTEDATABASECONFIGFILE);
                        ttcpath.setText(RunnerRepository.TESTSUITEPATH);
                        libpath.setText(RunnerRepository.REMOTELIBRARY);
                        tMasterXML.setText(RunnerRepository.XMLREMOTEDIR);
                        tUsers.setText(RunnerRepository.REMOTEUSERSDIRECTORY);
                        tepid.setText(RunnerRepository.REMOTEEPIDDIR);
                        tlog.setText(RunnerRepository.LOGSPATH);
                        tsecondarylog.setText(RunnerRepository.SECONDARYLOGSPATH);
                        logsenabled.setSelected(Boolean.parseBoolean(RunnerRepository.PATHENABLED));
                        if(RunnerRepository.getLogs().size()>0)trunning.setText(RunnerRepository.getLogs().get(0));
                        trunning.setText(RunnerRepository.getLogs().get(0));
                        tdebug.setText(RunnerRepository.getLogs().get(1));
                        tsummary.setText(RunnerRepository.getLogs().get(2));
                        tinfo.setText(RunnerRepository.getLogs().get(3));
                        tcli.setText(RunnerRepository.getLogs().get(4));
                        tdbfile.setText(RunnerRepository.REMOTEDATABASECONFIGPATH+
                                        RunnerRepository.REMOTEDATABASECONFIGFILE);
                        temailfile.setText(RunnerRepository.REMOTEEMAILCONFIGPATH+
                                            RunnerRepository.REMOTEEMAILCONFIGFILE);
                        tglobalsfile.setText(RunnerRepository.GLOBALSREMOTEFILE);
                        tSuites.setText(RunnerRepository.PREDEFINEDSUITES);
                        tceport.setText(RunnerRepository.getCentralEnginePort());
                        RunnerRepository.emptySuites();
                        RunnerRepository.window.mainpanel.p4.getTestConfig().tree.refreshStructure();
                        RunnerRepository.window.mainpanel.p4.getTestConfig().cfgedit.reinitialize();
                        RunnerRepository.window.mainpanel.p1.edit(false);
                        RunnerRepository.window.mainpanel.p1.sc.g.setUser(null);
                        RunnerRepository.openProjectFile();
                    }
                    else{
                        CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, 
                                              ConfigFiles.this, "WARNING", 
                                              "This is not a config file");}}}
            else{
                CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, 
                                      ConfigFiles.this, "WARNING", 
                                      "Could not find Config tab");}}
        catch(Exception e){e.printStackTrace();}}
        
    public JPanel addField(JTextField textfield,String text,int nr){
        textfield.setMaximumSize(new Dimension(340,25));
        textfield.setPreferredSize(new Dimension(340,25)); 
        if(RunnerRepository.getLogs().size()>0)textfield.setText(RunnerRepository.getLogs().get(nr));
        JLabel l1 = new JLabel(text);
        l1.setMaximumSize(new Dimension(80,20));
        l1.setPreferredSize(new Dimension(80,20));
        JPanel p721 = new JPanel();
        p721.setBackground(Color.WHITE);
        p721.add(l1);
        p721.add(textfield);
        p721.setMaximumSize(new Dimension(800,28));
        p721.setPreferredSize(new Dimension(800,28)); 
        return p721;}
        
    public JPanel addPanel(String title, String description,
                         final JTextField textfield,
                         String fieldtext, int Y, boolean withbutton,
                         ActionListener actionlistener){
        JPanel p1 = new JPanel();
        p1.setBackground(Color.WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(new Font("Arial",Font.PLAIN,14));
        border.setBorder(BorderFactory.createLineBorder(new Color(150,150,150), 1));
        p1.setBorder(border);         
        p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));    
        p1.setBounds(80,Y,800,75);
        paths.add(p1);
        JTextArea tcpath = new JTextArea(description);
        tcpath.setWrapStyleWord(true);
        tcpath.setLineWrap(true);
        tcpath.setEditable(false);        
        tcpath.setCursor(null);  
        tcpath.setOpaque(false);  
        tcpath.setFocusable(false);         
        tcpath.setFont(new Font("Arial",Font.PLAIN,12));
        tcpath.setBackground(getBackground());
        tcpath.setMaximumSize(new Dimension(170,22));        
        tcpath.setPreferredSize(new Dimension(170,22));
        tcpath.setBorder(null);
        JPanel p11 = new JPanel();
        p11.setBackground(Color.WHITE);
        p11.setLayout(new GridLayout());
        p11.add(tcpath);
        p11.setMaximumSize(new Dimension(700,18));
        p11.setPreferredSize(new Dimension(700,18));
        textfield.setMaximumSize(new Dimension(340,27));
        textfield.setPreferredSize(new Dimension(340,27));
        textfield.setText(fieldtext);
        JButton b = null;
        if(withbutton){
            b = new JButton("...");
            if(!PermissionValidator.canChangeFWM()){
                b.setEnabled(false);
            }
            b.setMaximumSize(new Dimension(50,20));
            b.setPreferredSize(new Dimension(50,20));
            if(actionlistener==null){
                b.addActionListener(new AbstractAction(){
                    public void actionPerformed(ActionEvent ev){ 
                        Container c;
                        
                        if(RunnerRepository.container!=null)c = RunnerRepository.container.getParent();
                        else c = RunnerRepository.window;
                        try{
                            new MySftpBrowser(RunnerRepository.host,RunnerRepository.user,RunnerRepository.password,textfield,c,false);
                        }catch(Exception e){
                            System.out.println("There was a problem in opening sftp browser!");
                            e.printStackTrace();
                        }
                    }});}
            else{b.addActionListener(actionlistener);
                b.setText("Save");
                b.setMaximumSize(new Dimension(70,20));
                b.setPreferredSize(new Dimension(70,20));}}
        JPanel p12 = new JPanel();
        p12.setBackground(Color.WHITE);
        p12.add(textfield);
        if(withbutton)p12.add(b);
        p12.setMaximumSize(new Dimension(700,32));
        p12.setPreferredSize(new Dimension(700,32));
        p1.add(p11);
        p1.add(p12);
        return p12;
    }
        
        
    public static void saveXML(boolean blank, String filename){
        boolean saved = true;
        try{DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(document);                    
            Comment simpleComment = document.createComment("\n Master config file for TSC.\n \n Logs"+
                                                            " Path: Where CE and PE write their getLogs()."+
                                                            " Reports Path: Where all reports are saved.\n "+
                                                            "Test Suite Config: All info about the current "+
                                                            "Test Suite (Test Plan).\n");
            document.appendChild(simpleComment);
            Element root = document.createElement("Root");
            document.appendChild(root);
            Element rootElement = document.createElement("FileType");
            root.appendChild(rootElement);
            rootElement.appendChild(document.createTextNode("config"));
            try{addTag("CentralEnginePort",tceport.getText(),root,blank,document);}
            catch(Exception e){addTag("CentralEnginePort","",root,blank,document);}
//             try{addTag("ResourceAllocatorPort",traPort.getText(),root,blank,document);}
//             catch(Exception e){addTag("ResourceAllocatorPort","",root,blank,document);}
//             try{addTag("HttpServerPort",thttpPort.getText(),root,blank,document);}
//             catch(Exception e){addTag("HttpServerPort","",root,blank,document);}
            try{addTag("TestCaseSourcePath",ttcpath.getText(),root,blank,document);}
            catch(Exception e){addTag("TestCaseSourcePath","",root,blank,document);}
            try{addTag("LibsPath",libpath.getText(),root,blank,document);}
            catch(Exception e){addTag("LibsPath","",root,blank,document);}
            try{addTag("UsersPath",tUsers.getText(),root,blank,document);}
            catch(Exception e){addTag("UsersPath","",root,blank,document);}
            try{addTag("PredefinedSuitesPath",tSuites.getText(),root,blank,document);}
            catch(Exception e){addTag("PredefinedSuitesPath","",root,blank,document);}
            
            
            try{addTag("ArchiveLogsPath",tsecondarylog.getText(),root,blank,document);}
            catch(Exception e){addTag("ArchiveLogsPath","",root,blank,document);}
            try{addTag("ArchiveLogsPathActive",logsenabled.isSelected()+"",root,blank,document);}
            catch(Exception e){addTag("ArchiveLogsPath","",root,blank,document);}
            
            try{addTag("LogsPath",tlog.getText(),root,blank,document);}
            catch(Exception e){addTag("LogsPath","",root,blank,document);}
            rootElement = document.createElement("LogFiles");
            root.appendChild(rootElement);
            try{addTag("logRunning",trunning.getText(),rootElement,blank,document);}
            catch(Exception e){addTag("logRunning","",rootElement,blank,document);}
            try{addTag("logDebug",tdebug.getText(),rootElement,blank,document);}
            catch(Exception e){addTag("logDebug","",rootElement,blank,document);}
            try{addTag("logSummary",tsummary.getText(),rootElement,blank,document);}
            catch(Exception e){addTag("logSummary","",rootElement,blank,document);}
            try{addTag("logTest",tinfo.getText(),rootElement,blank,document);}
            catch(Exception e){addTag("logTest","",rootElement,blank,document);}
            try{addTag("logCli",tcli.getText(),rootElement,blank,document);}
            catch(Exception e){addTag("logCli","",rootElement,blank,document);}
            try{addTag("DbConfigFile",tdbfile.getText(),root,blank,document);}
            catch(Exception e){addTag("DbConfigFile","",root,blank,document);}
            try{addTag("EpNames",tepid.getText(),root,blank,document);}
            catch(Exception e){addTag("EpNames","",root,blank,document);}
            try{addTag("EmailConfigFile",temailfile.getText(),root,blank,document);}
            catch(Exception e){addTag("EmailConfigFile","",root,blank,document);}
            try{addTag("GlobalParams",tglobalsfile.getText(),root,blank,document);}
            catch(Exception e){addTag("GlobalParams","",root,blank,document);}
            try{addTag("TestConfigPath",testconfigpath.getText(),root,blank,document);}
            catch(Exception e){addTag("TestConfigPath","",root,blank,document);}
            String temp;
            if(blank) temp ="fwmconfig";
            else temp = filename;
            File file = new File(RunnerRepository.temp+RunnerRepository.getBar()+
                                "Twister"+RunnerRepository.getBar()+temp+".xml");
            Result result = new StreamResult(file);
            transformer.transform(source, result);
            System.out.println("Saving to: "+RunnerRepository.USERHOME+"/twister/config/");
            FileInputStream in = new FileInputStream(file);
            RunnerRepository.uploadRemoteFile(RunnerRepository.USERHOME+"/twister/config/", in, file.getName());
        }
        catch(ParserConfigurationException e){
            System.out.println("DocumentBuilder cannot be created which"+
                                " satisfies the configuration requested");
            saved = false;}
        catch(TransformerConfigurationException e){
            System.out.println("Could not create transformer");
            saved = false;}
        catch(Exception e){
            e.printStackTrace();
            saved = false;}
        if(saved){
            CustomDialog.showInfo(JOptionPane.INFORMATION_MESSAGE,
                                  RunnerRepository.window,
                                  "Successful", "File successfully saved");}
        else{
            CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE,
                                  RunnerRepository.window.mainpanel.p4.getConfig(),
                                  "Warning", "File could not be saved ");}}
        
                                  
    public static void addTag(String tagname, String tagcontent ,
                              Element root,boolean blank,Document document){
        Element rootElement = document.createElement(tagname);
        root.appendChild(rootElement);
        String temp;
        if(blank) temp ="";
        else temp = tagcontent;            
        rootElement.appendChild(document.createTextNode(temp));
    }
}