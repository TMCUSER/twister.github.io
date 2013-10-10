/*
File: RunnerRepository.java ; This file is part of Twister.
Version: 2.0030

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
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.applet.Applet;
import java.util.ArrayList;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.awt.Image;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import java.net.URL;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.Iterator;
import java.util.Map.Entry;
import com.google.gson.JsonPrimitive;
import java.io.Writer;
import java.io.OutputStreamWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.Color;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.util.Vector;
import java.util.Hashtable;
import com.twister.Item;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import java.net.URLClassLoader;
import java.awt.Point;
import com.twister.CustomDialog;
import com.twister.plugin.twisterinterface.TwisterPluginInterface;
import com.twister.plugin.twisterinterface.CommonInterface;
import com.twister.plugin.baseplugin.BasePlugin;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JFrame;

/*
 * static class to hold
 * twister resources
 */
public class RunnerRepository {
    private static ArrayList <Item> suite;//suite list
    private static ArrayList<Item> suitetest ;//test suite list generated
    private static String bar ;//System specific file.separator
    private static ArrayList<String> logs;//logs tracked by twister framwork
    public static String[] columnNames;
    public static Window window;//main window displayed if twister is running local
    //public static ChannelSftp c;//main sftp connection used by Twister
    public static ChannelSftp connection;//main sftp connection used by Twister
    public static Session session;
    public static Hashtable variables ;
    public static Starter starter;
    public static String user,host,password,temp,TWISTERINI, USERHOME, REMOTECONFIGDIRECTORY,
                         PLUGINSDIRECTORY,CENTRALENGINEPORT,
                         REMOTEDATABASECONFIGPATH,
                         REMOTEDATABASECONFIGFILE, REMOTEEMAILCONFIGPATH,
                         REMOTEEMAILCONFIGFILE,CONFIGDIRECTORY, USERSDIRECTORY,
                         XMLDIRECTORY,  
                         TESTSUITEPATH,
                         LOGSPATH ,XMLREMOTEDIR,REMOTEPLUGINSDIR,
                         REMOTELIBRARY,PREDEFINEDSUITES,
                         REMOTEUSERSDIRECTORY, REMOTEEPIDDIR, //REMOTEHARDWARECONFIGDIRECTORY,
                         PLUGINSLOCALGENERALCONF, GLOBALSREMOTEFILE,
                         SECONDARYLOGSPATH,PATHENABLED,TESTCONFIGPATH;
    public static Image passicon,testbedicon,porticon,suitaicon, tcicon, propicon,
                        failicon, passwordicon, playicon, stopicon, pauseicon,logo,
                        background,notexecicon,pendingicon,skipicon,stoppedicon,
                        timeouticon,waiticon,workingicon,moduleicon,deviceicon,upicon,
                        addsuitaicon,removeicon,vlcclient,vlcserver,switche,optional,
                        flootw,rack150,rack151,rack152,switche2,inicon,outicon,baricon;
    public static boolean run ;//signal that Twister is not closing
    public static boolean applet,initialized,sftpoccupied; //keeps track if twister is run from applet or localy;;stfpconnection flag
    public static IntroScreen introscreen;    
    private static ArrayList <String []> databaseUserFields ;
    public static int LABEL = 0;    
    public static int ID = 1;
    public static int SELECTED = 2;
    public static int MANDATORY = 3;
    public static int ELEMENTSNR = 4;
    private static XmlRpcClient client;
    private static JsonObject editors, looks, layout, inifile;//json structure of conf file saved localy;editors saved by user localy
    private static JsonArray plugins;
    private static String[] lookAndFeels;
    public static Container container;
    private static Document pluginsconfig;
    private static String version = "2.034";
    private static String builddate = "04.10.2013";
    public static String logotxt,os,python;
    
    public static void setStarter(Starter starter){
        RunnerRepository.starter = starter;
    }
    
    /*
     * repository initialization method
     * applet - if it is initialized from applet
     * host - server for twister location
     * container - applet or null
     */
    public static void initialize(String applet,String host,Applet container){
        RunnerRepository.initialized = false;
        RunnerRepository.run = true;
        RunnerRepository.container = container;
        suite = new ArrayList <Item> ();
        suitetest = new ArrayList <Item> ();
        logs = new ArrayList<String>();
        variables = new Hashtable(5,0.5f);
        bar = System.getProperty("file.separator");
        databaseUserFields = new ArrayList<String[]>();
        
        /*
         * temp folder creation to hold
         * all the needed twister files localy
         */
        try{
            loadResourcesFromApplet();
            temp = System.getProperty("user.home")+bar+".twister" ;
            File g1 = new File(temp);
            if(g1.mkdir()){
                System.out.println(temp+" successfully created");}
            else System.out.println(temp+" could not be created ");
            g1 = new File(temp+bar+host);
            if(g1.mkdir()){
                System.out.println(temp+bar+host+" successfully created");}
            else System.out.println(temp+bar+host+" could not be created ");
            temp = g1.getCanonicalPath();}
        catch(Exception e){
            System.out.println("Could not retrieve Temp directory for this OS");
            e.printStackTrace();}
        System.out.println("Temp directory where Twister Directory is created: "+temp);
        File file = new File(RunnerRepository.temp+bar+"Twister");
        File twisterhome = new File(System.getProperty("user.home")+bar+".twister");
        /*
         * if file was not deleted on previous
         * Twister exit, delete it now
         */
        if(file.exists()){
            if(Window.deleteTemp(file))
                System.out.println(RunnerRepository.temp+bar+"Twister deleted successful");
            else System.out.println("Could not delete: "+RunnerRepository.temp+bar+"Twister");}
        if(!twisterhome.exists()){
            try{if(twisterhome.mkdir())
                    System.out.println(twisterhome.getCanonicalPath()+" successfully created");
                else System.out.println("Could not create "+twisterhome.getCanonicalPath());}
            catch(Exception e){
                System.out.println("Could not create "+
                    System.getProperty("user.home")+bar+".twister");
                e.printStackTrace();}}
        /*
         * twiste configuration file
         */
        try{File twisterini = new File(twisterhome.getCanonicalPath()+bar+"twister.conf");
            TWISTERINI = twisterhome.getCanonicalPath()+bar+"twister.conf";
            if(!twisterini.exists()||twisterini.length()==0){// if it does not exist or is empty, create one from scratch 
                if(twisterini.exists())twisterini.delete();
                if(new File(twisterhome.getCanonicalPath()+bar+"twister.conf").createNewFile()){
                    generateJSon();}
                else System.out.println("Could not create twister.conf");}
            parseIni(twisterini);
        }//parse configuration file
        catch(Exception e){e.printStackTrace();}
        RunnerRepository.host = host;
        System.out.println("Setting sftp server to :"+host);
        introscreen = new IntroScreen();//display intro screen
        introscreen.setStatus("Started initialization");
        introscreen.setVisible(true);
        RunnerRepository.applet = Boolean.parseBoolean(applet);
        if(RunnerRepository.applet)System.out.println("Twister running from applet");
        else System.out.println("Twister running from Main");
        
        
        try{
            if(userpassword(user,password,host)){
                ssh(user,password,host);
                /*
                 * create directory structure
                 * for twister resources localy
                 */
                System.out.println("Authentication succeeded");
                if(new File(temp+bar+"Twister").mkdir())
                    System.out.println(temp+bar+"Twister"+" folder successfully created");
                else System.out.println("Could not create "+temp+bar+"Twister"+" folder");
                if(new File(temp+bar+"Twister"+bar+"XML").mkdir())
                    System.out.println(temp+bar+"Twister"+bar+
                        "XML folder successfully created");
                else System.out.println("Could not create "+temp+bar+"Twister"+
                    bar+"XML folder");
                if(new File(temp+bar+"Twister"+bar+"Users").mkdir()){
                    USERSDIRECTORY = RunnerRepository.temp+bar+"Twister"+bar+"Users";
                    System.out.println(temp+bar+"Twister"+bar+
                        "Users folder successfully created");}
                else System.out.println("Could not create "+temp+bar+"Twister"+
                    bar+"Users folder");
                if(new File(temp+bar+"Twister"+bar+"config").mkdir()){
                    System.out.println(temp+bar+"Twister"+bar+
                        "config folder successfully created");}
                else System.out.println("Could not create "+temp+bar+
                    "Twister"+bar+"config folder");
                CONFIGDIRECTORY = RunnerRepository.temp+bar+"Twister"+bar+"config";
                File pluginsdirectory = new File(twisterhome.getCanonicalPath()+
                                                 bar+"Plugins");
                if(pluginsdirectory.exists()){
                    PLUGINSDIRECTORY = twisterhome.getCanonicalPath()+bar+"Plugins";
                    System.out.println(twisterhome.getCanonicalPath()+bar+
                                        " Plugins folder found");}
                else if(pluginsdirectory.mkdir()){
                    PLUGINSDIRECTORY = twisterhome.getCanonicalPath()+bar+"Plugins";
                    System.out.println(twisterhome.getCanonicalPath()+
                            bar+" Plugins folder successfully created");}
                else System.out.println("Could not create "+twisterhome.getCanonicalPath()+
                                        bar+"Plugins folder");
                PLUGINSLOCALGENERALCONF = temp+bar+"Twister"+bar+"config"+bar+"plugins.xml";
                introscreen.setStatus("Started to parse the config");
                introscreen.addPercent(0.035);
                introscreen.repaint();
                parseConfig();
                if(!getPluginsFile())createGeneralPluginConf();
                if(!parsePluginsConfig(CONFIGDIRECTORY+"/plugins.xml")){
                    System.out.println("There was a problem in parsing"+
                                       " plugins configuration");}
                                       
                initializeRPC();
                setCEVeriosns();
                introscreen.setStatus("Finished parsing the config");
                introscreen.addPercent(0.035);
                introscreen.repaint();
                Plugins.deletePlugins();
                
                parseDBConfig(RunnerRepository.REMOTEDATABASECONFIGFILE,true);
                if(!isCE()){
                    CustomDialog.showInfo(JOptionPane.ERROR_MESSAGE,RunnerRepository.window,
                                        "Error", "CE is not running, please start CE in "+
                                                    "order for Twister Framework to run properly");
                }
                try{REMOTEPLUGINSDIR = client.execute("getTwisterPath", new Object[]{}).toString()+"/plugins";
                    System.out.println("Remote Twister plugins instalation path: "+REMOTEPLUGINSDIR);
                } catch(Exception e){
                    REMOTEPLUGINSDIR = "/opt/twister/plugins";
                    System.out.println("Remote Twister plugins instalation path: "+REMOTEPLUGINSDIR);
                }
                window = new Window(RunnerRepository.applet,container);
                parseEmailConfig(RunnerRepository.REMOTEEMAILCONFIGFILE,true);
                populatePluginsVariables();
            }
            else{
                /*
                 * if login is not scucces remove temp folder
                 * and exit application
                 */
                if(Window.deleteTemp(file))
                    System.out.println(RunnerRepository.temp+bar+"Twister deleted successful");
                else System.out.println("Could not delete: "+temp+bar+"Twister");
                introscreen.dispose();
                run = false;
                if(!RunnerRepository.applet)System.exit(0);}
            }
        catch(Exception e){e.printStackTrace();}
        initialized  = true;
    }
    
    //test if CE is running
    public static boolean isCE(){
        try{client.execute("echo", new Object[]{"ping"});
            return true;
        } catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    
    public static void setCEVeriosns(){
        try{String []info = client.execute("getSysInfo", new Object[]{}).toString().split("\n");
            RunnerRepository.os = info[0];
            RunnerRepository.python = info[1];
        } catch(Exception e){
            RunnerRepository.os = "";
            RunnerRepository.python = "";
            e.printStackTrace();
        }
    }
    
    //populate the Hshtable transferred to plugins
    //with appropriate variables
    public static void populatePluginsVariables(){
        variables.put("host",host);
        variables.put("user",user);
        variables.put("password",password);  
        variables.put("temp",temp);
        variables.put("inifile",TWISTERINI);
        variables.put("remoteuserhome",USERHOME);  
        variables.put("remotconfigdir",REMOTECONFIGDIRECTORY);  
        variables.put("localplugindir",PLUGINSDIRECTORY);
        variables.put("centralengineport",CENTRALENGINEPORT); 
        variables.put("remotedatabaseparth",REMOTEDATABASECONFIGPATH);
        variables.put("remotedatabasefile",REMOTEDATABASECONFIGFILE);
        variables.put("remoteemailpath",REMOTEEMAILCONFIGPATH);
        variables.put("remoteemailfile",REMOTEEMAILCONFIGFILE);
        variables.put("configdir",CONFIGDIRECTORY);
        variables.put("usersdir",USERSDIRECTORY);
        variables.put("masterxmldir",XMLDIRECTORY);
        variables.put("testsuitepath",TESTSUITEPATH);
        variables.put("predefinedsuites",PREDEFINEDSUITES);
        variables.put("logspath",LOGSPATH);
        variables.put("masterxmlremotedir",XMLREMOTEDIR);
        variables.put("remoteepdir",REMOTEEPIDDIR);
        variables.put("remoteusersdir",REMOTEUSERSDIRECTORY);
        variables.put("remotelibrary",REMOTELIBRARY);
        variables.put("pluginslocalgeneralconf",PLUGINSLOCALGENERALCONF);
        variables.put("remotegeneralpluginsdir",REMOTEPLUGINSDIR);
        variables.put("globalremotefile",GLOBALSREMOTEFILE);
    }
        
    /*
     * method to create general plugin
     * configuration file 
     */
    public static void createGeneralPluginConf(){
        try{
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            DOMSource source = new DOMSource(document);
            Element rootElement = document.createElement("Root");
            document.appendChild(rootElement);
            File file = new File(RunnerRepository.PLUGINSLOCALGENERALCONF);
            Result result = new StreamResult(file);
            transformer.transform(source, result);
            FileInputStream in = new FileInputStream(file);
            uploadRemoteFile(RunnerRepository.USERHOME+"/twister/config/",in,file.getName());
        }
        catch(Exception e){
            System.out.println("There was a problem in generating Plugins general config");
            e.printStackTrace();
        }
    }
    
    public static void loadResourcesFromApplet(){
        try{
            RunnerRepository.tcicon = loadIcon("tc.png");
            RunnerRepository.background = loadIcon("background.png");
            RunnerRepository.pendingicon = loadIcon("pending.png");
            RunnerRepository.deviceicon = loadIcon("device.png");
            RunnerRepository.upicon = loadIcon("up.png");
            RunnerRepository.moduleicon = loadIcon("module.png");
            RunnerRepository.notexecicon = loadIcon("notexec.png");
            RunnerRepository.skipicon = loadIcon("skip.png");
            RunnerRepository.stoppedicon = loadIcon("stopped.png");
            RunnerRepository.timeouticon = loadIcon("timeout.png");
            RunnerRepository.waiticon = loadIcon("waiting.png");
            RunnerRepository.workingicon = loadIcon("working.png");
            RunnerRepository.suitaicon = loadIcon("suita.png");
            RunnerRepository.propicon = loadIcon("prop.png");
            RunnerRepository.vlcclient = loadIcon("vlcclient.png");
            RunnerRepository.failicon = loadIcon("fail.png");
            RunnerRepository.passicon = loadIcon("pass.png");
            RunnerRepository.stopicon = loadIcon("stop.png");
            RunnerRepository.switche = loadIcon("switch.png");
            RunnerRepository.switche2 = loadIcon("switch.jpg");
            RunnerRepository.flootw = loadIcon("twisterfloodlight.png");
            RunnerRepository.rack150 = loadIcon("150.png");
            RunnerRepository.rack151 = loadIcon("151.png");
            RunnerRepository.rack152 = loadIcon("152.png");
            RunnerRepository.vlcserver = loadIcon("vlcserver.png");
            RunnerRepository.playicon = loadIcon("play.png");
            RunnerRepository.addsuitaicon = loadIcon("addsuita.png");
            RunnerRepository.removeicon = loadIcon("deleteicon.png");
            RunnerRepository.pauseicon = loadIcon("pause.png");
            RunnerRepository.porticon = loadIcon("port.png");
            RunnerRepository.testbedicon = loadIcon("testbed.png");
            RunnerRepository.inicon = loadIcon("in.png");
            RunnerRepository.outicon = loadIcon("out.png");
            RunnerRepository.passwordicon = loadIcon("passwordicon.png");
            RunnerRepository.baricon = loadIcon("bar.png");
        }
        catch(Exception e){e.printStackTrace();}
    }
    
    public static void setSize(int width, int height){
        System.out.println("RunnerRepository setSize");
        RunnerRepository.window.mainpanel.setSize(width-20,height-20);
        RunnerRepository.window.mainpanel.p1.splitPane.setSize(width-52,height-120);
        RunnerRepository.window.mainpanel.setSize(width-28,height-40);
        RunnerRepository.window.mainpanel.p4.getScroll().setSize(width-310,height-155);
        RunnerRepository.window.mainpanel.p4.getScroll().setPreferredSize(new Dimension(width-310,height-155));
        RunnerRepository.window.mainpanel.p4.getMain().setSize(width-300,height-130);
        RunnerRepository.window.mainpanel.p4.getTB().setPreferredSize(
            new Dimension(width-300,height-150));
        try{RunnerRepository.window.appletpanel.setSize(width-20,height-20);}
        catch(Exception e){}
        RunnerRepository.window.mainpanel.p4.getPlugins().setPreferredSize(
            new Dimension(width-300,height-150));
        RunnerRepository.window.mainpanel.p4.getPlugins().horizontalsplit.setPreferredSize(
            new Dimension(width-305,height-155));
        RunnerRepository.window.logout.setLocation(width-130, 3);
        RunnerRepository.window.controlpanel.setLocation(width-285, 3);
        if(container!=null){
            container.validate();
            container.repaint();
        }
        
    }
    
    /*
     * the general method to load icons from jar
     */
    public static Image loadIcon(String icon){
        Image image = null;
        try{System.out.println("Getting "+icon+" from applet jar...");
            InputStream in = RunnerRepository.class.getResourceAsStream("Icons"+"/"+icon);
            System.out.println("Saving "+icon+" in memory.....");
            image = new ImageIcon(ImageIO.read(in)).getImage();
            in.close();
            if(image !=null)System.out.println(icon+" succsesfully loaded.");
            else System.out.println(icon+" not loaded.");}
        catch(Exception e){
            System.out.println("There was a problem in loading "+icon+" icon");
            e.printStackTrace();}
        return image;}
        
    /*
     * load resources needed for framework
     * from local pc
     */
    public static void loadResourcesFromLocal()throws Exception{
        InputStream in;
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"background.png"); 
        background = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"vlcclient.png"); 
        vlcclient = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"vlcserver.png"); 
        vlcserver = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"switch.png"); 
        switche = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"twisterfloodlight.png"); 
        flootw = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"150.png"); 
        rack150 = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"151.png"); 
        rack151 = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"152.png"); 
        rack152 = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"switch.jpg"); 
        switche2 = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"in.png"); 
        inicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"out.png"); 
        outicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"bar.png"); 
        baricon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"port.png"); 
        porticon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"deleteicon.png");
        removeicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"addsuita.png"); 
        addsuitaicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"device.png"); 
        deviceicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"up.png"); 
        upicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"module.png"); 
        moduleicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"tc.png"); 
        tcicon = new ImageIcon(ImageIO.read(in)).getImage(); 
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"suita.png"); 
        suitaicon = new ImageIcon(ImageIO.read(in)).getImage(); 
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"prop.png"); 
        propicon = new ImageIcon(ImageIO.read(in)).getImage(); 
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"fail.png"); 
        failicon = new ImageIcon(ImageIO.read(in)).getImage(); 
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"pass.png");
        passicon = new ImageIcon(ImageIO.read(in)).getImage(); 
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"stop.png");
        stopicon = new ImageIcon(ImageIO.read(in)).getImage(); 
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"play.png");
        playicon = new ImageIcon(ImageIO.read(in)).getImage();                 
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"notexec.png");
        notexecicon = new ImageIcon(ImageIO.read(in)).getImage(); 
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"pending.png");
        pendingicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"skip.png");
        skipicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"stopped.png");
        stoppedicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"timeout.png");
        timeouticon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"waiting.png");
        waiticon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"passwordicon.png");
        passwordicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"working.png");
        workingicon = new ImageIcon(ImageIO.read(in)).getImage();                
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"pause.png");
        pauseicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"testbed.png");
        testbedicon = new ImageIcon(ImageIO.read(in)).getImage();
        in = RunnerRepository.class.getResourceAsStream("Icons"+bar+"optional.png");
        optional = new ImageIcon(ImageIO.read(in)).getImage();
        in.close();}
        
    /*
     * generate local config 
     * file from scratch
     */
    public static void generateJSon(){
        JsonObject root = new JsonObject();
        JsonObject array =new JsonObject();
        array.addProperty("Embedded", "embedded");
        array.addProperty("DEFAULT", "Embedded");
        JsonObject array2 =new JsonObject();
        array2.addProperty("NimbusLookAndFeel", "javax.swing.plaf.nimbus.NimbusLookAndFeel");
        array2.addProperty("MetalLookAndFeel", "javax.swing.plaf.metal.MetalLookAndFeel");
        array2.addProperty("MotifLookAndFeel", "com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        array2.addProperty("WindowsLookAndFeel", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        array2.addProperty("JGoodiesWindowsLookAndFeel", "com.jgoodies.looks.windows.WindowsLookAndFeel");
        array2.addProperty("Plastic3DLookAndFeel", "com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
        array2.addProperty("PlasticXPLookAndFeel", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
        array2.addProperty("DEFAULT", "MetalLookAndFeel");
        root.add("plugins", new JsonArray());
        root.add("editors", array);
        root.add("looks", array2);
        root.add("layout", new JsonObject());
        try{FileWriter writer = new FileWriter(TWISTERINI);
            Gson gson = new GsonBuilder().setPrettyPrinting().create(); 
            writer.write(gson.toJson(root));
            writer.close();}
        catch(Exception e){
            System.out.println("Could not write default JSon to twister.conf");
            e.printStackTrace();}
        System.out.println("twister.conf successfully created");}
    
    /*
     * set UI Look based on
     * user selection
     */
    public static void setUILook(final String look){
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                System.out.println("Setting UI: "+look);
                try{UIManager.setLookAndFeel(RunnerRepository.getLooks().get(look).getAsString());
                    if(applet){SwingUtilities.updateComponentTreeUI(container);}
                    else if(window!=null){SwingUtilities.updateComponentTreeUI(window);}}
                catch(Exception e){e.printStackTrace();}}});}
                
    private static void ssh(String user, String passwd, String host){
        try{JSch jsch = new JSch();
            session = jsch.getSession(user, host, 22);
            session.setPassword(passwd);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Channel channel = session.openChannel("shell");
            channel.connect();
            Thread.sleep(1000);
            channel.disconnect();
            session.disconnect();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
        
    /*
     * attempt to connect with sftp to server
     */ 
    public static boolean userpassword(String user,String password, String host){
        boolean passed = false;
            try{
                JSch jsch = new JSch();
                session = jsch.getSession(user, host, 22);
                session.setPassword(password);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();
                Channel channel = session.openChannel("sftp");
                channel.connect();
                connection = (ChannelSftp)channel;
                try{USERHOME = connection.pwd();}
                catch(Exception e){
                    System.out.println("ERROR: Could not retrieve remote user home directory");}
                REMOTECONFIGDIRECTORY = USERHOME+"/twister/config/";
                passed = true;
            }
            catch(JSchException ex){
                if(ex.toString().indexOf("Auth fail")!=-1)
                    System.out.println("wrong user and/or password");
                else{ex.printStackTrace();
                    System.out.println("Could not connect to server");}}
//                 }
        return true;}        
      
    /*
     * method used to reset database config
     */
    public static void resetDBConf(String filename,boolean server){
        databaseUserFields.clear();
        System.out.println("Reparsing "+filename);
        parseDBConfig(filename,server);
        window.mainpanel.p1.suitaDetails.restart(databaseUserFields);}
        
    /*
     * method used to reset Email config
     */
    public static void resetEmailConf(String filename,boolean server){
        parseEmailConfig(filename,server);}
        
    /*
     * method to get database config file
     * name - file name
     * fromserver - if from server(true) else from local temp folder
     */
    public static File getDBConfFile(String name,boolean fromServer){
        File file = new File(temp+bar+"Twister"+bar+"config"+bar+name);
        if(fromServer){
            String content= RunnerRepository.getRemoteFileContent(RunnerRepository.REMOTEDATABASECONFIGPATH+name);

            try{BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(content);
                writer.close();
            } catch(Exception e){
                e.printStackTrace();
            }   
        }
        return file;}
        
        
    /*
     * method to get Email config file
     * name - file name
     * fromserver - if from server(true) else from local temp folder
     */    
    public static File getEmailConfFile(String name,boolean fromServer){
        File file = new File(temp+bar+"Twister"+bar+"config"+bar+name);
        if(fromServer){
            String content= RunnerRepository.getRemoteFileContent(RunnerRepository.REMOTEEMAILCONFIGPATH+name);
            try{BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(content);
                writer.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return file;}
     
    /*
     * parse database config file
     * name - file name
     * fromserver - true - false
     */
    public static DefaultMutableTreeNode parseDBConfig(String name,boolean fromServer){
        File dbConf = getDBConfFile(name,fromServer);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        try{DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(dbConf);
            doc.getDocumentElement().normalize();
            NodeList nodeLst = doc.getElementsByTagName("insert_section");
            Element tablee = (Element)nodeLst.item(0);
            NodeList fields = tablee.getElementsByTagName("field");
            for(int i=0;i<fields.getLength();i++){                
                tablee = (Element)fields.item(i);
                if(tablee.getAttribute("GUIDefined").equals("true")){
                    String field [] = new String[ELEMENTSNR];
                    field[0]=tablee.getAttribute("Label");
                    if(field[0]==null){
                        System.out.println("Warning, no Label element in"+
                                            " field tag in db.xml at filed nr: "+i);
                        field[0]="";}
                    field[1]=tablee.getAttribute("ID");
                    if(field[1]==null){
                        System.out.println("Warning, no ID element in "+
                                            "field tag in db.xml at filed nr: "+i);
                        field[1]="";}
                    field[2]=tablee.getAttribute("Type");
                    if(field[2]==null){
                        System.out.println("Warning, no Type element in"+
                                            " field tag in db.xml at filed nr: "+i);
                        field[2]="";}
                    field[3]=tablee.getAttribute("Mandatory");
                    if(field[3]==null){
                        System.out.println("Warning, no Mandatory element "+
                                            "in field tag in db.xml at filed nr: "+i);
                        field[3]="";}
                    databaseUserFields.add(field);}}}
        catch(Exception e){
            try{System.out.println("Could not parse batabase XML file: "+dbConf.getCanonicalPath());}
            catch(Exception ex){
                System.out.println("There is a problem with "+name+" file");
                ex.printStackTrace();}
            e.printStackTrace();}
        return root;}
        
    /*
     * parse email config file
     */
    public static void parseEmailConfig(String name,boolean fromServer){
        //clear components
        window.mainpanel.p4.getEmails().setIPName("");
        window.mainpanel.p4.getEmails().setPort("");
        window.mainpanel.p4.getEmails().setUser("");
        window.mainpanel.p4.getEmails().setFrom("");
        window.mainpanel.p4.getEmails().setEmails("");
        window.mainpanel.p4.getEmails().setPassword("");
        window.mainpanel.p4.getEmails().setMessage("");
        window.mainpanel.p4.getEmails().setSubject("");
        
        File dbConf = getEmailConfFile(name,fromServer);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try{DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(dbConf);
            doc.getDocumentElement().normalize();                
            window.mainpanel.p4.getEmails().setCheck(Boolean.parseBoolean(getTagContent(doc, "Enabled", "email config.")));
            String smtppath = getTagContent(doc, "SMTPPath", "email config.");
            window.mainpanel.p4.getEmails().setIPName(smtppath.split(":")[0]);
            window.mainpanel.p4.getEmails().setPort(smtppath.split(":")[1]);
            window.mainpanel.p4.getEmails().setUser(getTagContent(doc, "SMTPUser", "email config."));
            window.mainpanel.p4.getEmails().setFrom(getTagContent(doc, "From", "email config."));
            window.mainpanel.p4.getEmails().setEmails(getTagContent(doc, "To", "email config."));
            if(!getTagContent(doc, "SMTPPwd", "email config.").equals("")){
                window.mainpanel.p4.getEmails().setPassword("****");}
            window.mainpanel.p4.getEmails().setMessage(getTagContent(doc, "Message", "email config."));
            window.mainpanel.p4.getEmails().setSubject(getTagContent(doc, "Subject", "email config."));}
        catch(Exception e){e.printStackTrace();}}
        
        
    /*
     * parse main fwmconfig file
     */
    public static void parseConfig(){ 
        try{InputStream in = null;
            byte[] data = new byte[100]; 
            int nRead;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            OutputStream out=null;
            InputStreamReader inputStreamReader = null;
            BufferedReader bufferedReader = null;
            String line = null;
            String name = null;
            
            
            if(RunnerRepository.getRemoteFolderContent(USERHOME+"/twister/config/").length==0){
                System.out.println("Could not get config folder from:"+USERHOME+"/twister/config/");
                CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE,RunnerRepository.window,
                                        "Warning", "Could not get config folder from:"+USERHOME+
                                        "/twister/config/");
                if(Window.deleteTemp(new File(RunnerRepository.temp+bar+"Twister")))
                    System.out.println(RunnerRepository.temp+bar+"Twister deleted successful");
                else System.out.println("Could not delete: "+RunnerRepository.temp+bar+"Twister");
                run = false;
                if(!applet)System.exit(0);
            }
                
                
            String content = RunnerRepository.getRemoteFileContent(USERHOME+"/twister/config/fwmconfig.xml");
            if(content==null){
                CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, RunnerRepository.window,
                                        "Warning","Could not get fwmconfig.xml from "
                                        +USERHOME+"+/twister/config/ creating a blank one.");
                ConfigFiles.saveXML(true,"");
                //in = c.get("fwmconfig.xml");
            }
            
            
            File file = new File(temp+bar+"Twister"+bar+"config"+bar+"fwmconfig.xml");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
            String usersdir="";
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
            introscreen.setStatus("Finished getting fwmconfig");
            introscreen.addPercent(0.035);
            introscreen.repaint();
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            try{DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(RunnerRepository.getFwmConfig());
                doc.getDocumentElement().normalize();    
                LOGSPATH = getTagContent(doc,"LogsPath", "framework config.");
                if(doc.getElementsByTagName("LogFiles").getLength()==0)
                    System.out.println("LogFiles tag not found in fwmconfig");
                else{
                    logs.add(getTagContent(doc,"logRunning", "framework config."));
                    logs.add(getTagContent(doc,"logDebug", "framework config."));
                    logs.add(getTagContent(doc,"logSummary", "framework config."));
                    logs.add(getTagContent(doc,"logTest", "framework config."));
                    logs.add(getTagContent(doc,"logCli", "framework config."));}
//                 HTTPSERVERPORT = getTagContent(doc,"HttpServerPort");
                CENTRALENGINEPORT = getTagContent(doc,"CentralEnginePort", "framework config.");
                //RESOURCEALLOCATORPORT = getTagContent(doc,"ResourceAllocatorPort");
                usersdir = getTagContent(doc,"UsersPath", "framework config.");
                REMOTEUSERSDIRECTORY = usersdir;
                XMLREMOTEDIR = USERHOME+"/twister/config/testsuites.xml";
//                 getTagContent(doc,"MasterXMLTestSuite");
                XMLDIRECTORY = RunnerRepository.temp+bar+"Twister"+bar+"XML"+
                                        bar+XMLREMOTEDIR.split("/")[XMLREMOTEDIR.split("/").length-1];
                REMOTELIBRARY = getTagContent(doc,"LibsPath", "framework config.");
                REMOTEEPIDDIR = getTagContent(doc,"EpNames", "framework config.");
                REMOTEDATABASECONFIGFILE = getTagContent(doc,"DbConfigFile", "framework config.");
                String [] path = REMOTEDATABASECONFIGFILE.split("/");
                StringBuffer result = new StringBuffer();
                if (path.length > 0) {
                    for (int i=0; i<path.length-1; i++){
                        result.append(path[i]);
                        result.append("/");}}
                REMOTEDATABASECONFIGPATH = result.toString();
                REMOTEDATABASECONFIGFILE = path[path.length-1];
                REMOTEEMAILCONFIGFILE = getTagContent(doc,"EmailConfigFile", "framework config.");
                path = REMOTEEMAILCONFIGFILE.split("/");
                result = new StringBuffer();
                if (path.length > 0) {
                    for (int i=0; i<path.length-1; i++){
                        result.append(path[i]);
                        result.append("/");}}
                REMOTEEMAILCONFIGPATH = result.toString();
                REMOTEEMAILCONFIGFILE = path[path.length-1];
                TESTSUITEPATH = getTagContent(doc,"TestCaseSourcePath", "framework config.");
                PREDEFINEDSUITES = getTagContent(doc,"PredefinedSuitesPath", "framework config.");
                SECONDARYLOGSPATH = getTagContent(doc,"ArchiveLogsPath", "framework config.");
                PATHENABLED = getTagContent(doc,"ArchiveLogsPathActive", "framework config.");
                TESTCONFIGPATH = getTagContent(doc,"TestConfigPath", "framework config.");
                GLOBALSREMOTEFILE = getTagContent(doc,"GlobalParams", "framework config.");
            }
            catch(Exception e){e.printStackTrace();}
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
            introscreen.setStatus("Finished initializing variables fwmconfig");
            introscreen.addPercent(0.035);
            introscreen.repaint();
            introscreen.setStatus("Started getting users xml");
            introscreen.addPercent(0.035);
            introscreen.repaint();
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}


            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
            introscreen.addPercent(0.035);
            introscreen.repaint();
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
            
            String dir = RunnerRepository.getXMLRemoteDir();
            String [] path = dir.split("/");
            StringBuffer result = new StringBuffer();
            if (path.length > 0) {
                for (int i=0; i<path.length-2; i++){
                    result.append(path[i]);
                    result.append("/");}}
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
            introscreen.setStatus("Finished writing xml path");
            introscreen.addPercent(0.035);
            introscreen.repaint();
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
            int length = RunnerRepository.getRemoteFolderContent(result.toString()+path[path.length-2]).length;
            if(length>2){
                introscreen.setStatus("Started writing xml file");
                introscreen.addPercent(0.035);
                introscreen.repaint();
                
                file = new File(temp+bar+"Twister"+bar+"XML"+
                            bar+XMLREMOTEDIR.split("/")[XMLREMOTEDIR.split("/").length-1]);
                writer = new BufferedWriter(new FileWriter(file));
                content = RunnerRepository.getRemoteFileContent(XMLREMOTEDIR);
                writer.write(content);
                writer.close();
                introscreen.setStatus("Finished writing xml ");
                introscreen.addPercent(0.035);
                introscreen.repaint();
            }
        }
        catch(Exception e){e.printStackTrace();}}
        
    /*
     * method to get tag content from xml
     * doc - xml document
     * tag - tag name
     */
    public static String getTagContent(Document doc, String tag, String file){
        NodeList nodeLst = doc.getElementsByTagName(tag);
        if(nodeLst.getLength()==0){
            System.out.println("tag "+tag+" not found in "+doc.getDocumentURI());
            CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE,RunnerRepository.window,
                                        "Warning", tag+" tag not found in "+file);
            return "";
        }
        Node fstNode = nodeLst.item(0);
        Element fstElmnt = (Element)fstNode;
        NodeList fstNm = fstElmnt.getChildNodes();
        String temp;
        try{temp = fstNm.item(0).getNodeValue().toString();}
        catch(Exception e){
            System.out.println(tag+" empty");
            CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE,RunnerRepository.window,
                                        "Warning", tag+" tag is empty in "+file);
            temp = "";}
        return temp;}
        
    /*
     * parser for conf twister file
     */
    public static void parseIni(File ini){
        try{FileInputStream in  = new FileInputStream(ini);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);  
            StringBuffer b=new StringBuffer("");
            String line;
            try{while ((line=bufferedReader.readLine())!= null){b.append(line);}
                bufferedReader.close();
                inputStreamReader.close();
                in.close();}
            catch(Exception e){e.printStackTrace();}
            JsonElement jelement = new JsonParser().parse(b.toString());
            inifile = jelement.getAsJsonObject();
            editors = inifile.getAsJsonObject("editors");
            looks = inifile.getAsJsonObject("looks");
            layout = inifile.getAsJsonObject("layout");
            plugins = inifile.getAsJsonArray("plugins");            
            if(layout==null){
                inifile.add("layout", new JsonObject());
                writeJSon();
                layout = inifile.getAsJsonObject("layout");}
            if(plugins==null){
                inifile.add("plugins", new JsonArray());
                writeJSon();
                plugins = inifile.getAsJsonArray("plugins");}}
        catch(Exception e){
            System.out.print("Could not parse ini file: ");
            try{System.out.println(ini.getCanonicalPath());}
            catch(Exception ex){ex.printStackTrace();}
            e.printStackTrace();}}
            
    /*
     * method to add suite to suite list
     */
    public static void addSuita(Item s){
        suite.add(s);}
        
    /*
     * method to get suite from suite list
     * s - suite index in list
     */
    public static Item getSuita(int s){
        return suite.get(s);}
        
    /*
     * method to get suite list size
     */ 
    public static int getSuiteNr(){
        return suite.size();}
            
    /*
     * method to get Database User Fields
     * set from twister
     */ 
    public static ArrayList<String[]> getDatabaseUserFields(){
        return databaseUserFields;}
      
    /*
     * clear all suite from test suite list
     */
    public static void emptyTestRunnerRepository(){
        suitetest.clear();}        
        
    /*
     * clear the list of logs tracked by Twister
     */
    public static void emptyLogs(){
        logs.clear();}    
         
    /*
     * method to get config file from local pc
     */
    public static File getFwmConfig(){
        return new File(temp+bar+"Twister"+bar+"config"+bar+"fwmconfig.xml");}
    /*
     * users directory from temp folder on local pc
     */  
    public static String getUsersDirectory(){
        return USERSDIRECTORY;}
        
    /*
     * Ep directory from server
     */ 
    public static String getRemoteEpIdDir(){
        return REMOTEEPIDDIR;}        
        
    /*
     * Users directory from server
     */ 
    public static String getRemoteUsersDirectory(){
        return REMOTEUSERSDIRECTORY;}
        
    /*
     * CentralEnginePort set by fwmconfig file
     */ 
    public static String getCentralEnginePort(){
        return CENTRALENGINEPORT;}
        
    /*
     * ResourceAllocatorPort set by fwmconfig file
     */ 
//     public static String getResourceAllocatorPort(){
//         return RESOURCEALLOCATORPORT;}
        
    /*
     * test suite xml directory from server
     */ 
    public static String getXMLRemoteDir(){
        return XMLREMOTEDIR;}
    
    /*
     * suite list from repository
     */
    public static ArrayList<Item> getSuite(){
        return suite;}
        
    /*
     * test suite list from repository
     */
    public static ArrayList<Item> getTestSuite(){
        return suitetest;}
    
    /*
     * test suite list size from repository
     */
    public static int getTestSuiteNr(){
        return suitetest.size();}

    /*
     * local config directory from temp
     */
    public static String getConfigDirectory(){
        return CONFIGDIRECTORY;}
        
     
    /*
     * local config directory from temp
     */
    public static String getTestConfigPath(){
        return TESTCONFIGPATH;}
        
    /*
     * add suite to test suite list
     */
    public static void addTestSuita(Item suita){
        suitetest.add(suita);}
        
    /*
     * method to get suite from test suite list 
     * i - suite index in test suite list
     */
    public static Item getTestSuita(int i){
        return suitetest.get(i);}
    
    /*
     * test suite path on server
     */
    public static String getTestSuitePath(){
        return TESTSUITEPATH;}
        
    /*
     * test suite path on server
     */
    public static String getPredefinedSuitesPath(){
        return PREDEFINEDSUITES;}

    /*
     * empty suites list in RunnerRepository
     */
    public static void emptySuites(){
        suite.clear();}
        
    /*
     * test suite xml local directory
     */
    public static String getTestXMLDirectory(){
        return XMLDIRECTORY;}
        
     /*
      * declare posible looksAndFeel
      */
    private static void populateLookAndFeels(){
        JsonObject looks = RunnerRepository.getLooks();
        if(looks!=null){
            int length = looks.entrySet().size();
            Iterator iter = looks.entrySet().iterator();
            Entry entry;
            String [] vecresult;
            if(looks.get("DEFAULT")!=null)lookAndFeels = new String[length-1];
            else lookAndFeels = new String[length];
            int index = 0;
            for(int i=0;i<length;i++){                        
                entry = (Entry)iter.next();
                if(entry.getKey().toString().equals("DEFAULT"))continue;
                lookAndFeels[index] = (String)entry.getKey();
                index++;}}
        else{System.out.println("Error: No LooksAndFeels set, using default look");}}
        
     /*
      *populate lookandfeel cobo
      *with looks and feels that are
      *available
      */
    private static int populateCombo(JComboBox combo,String[]list){
        int index = -1;
        String name;
        for(int i=0;i<list.length;i++){
            try{Class.forName(getLooks().get(list[i]).getAsString());                
                combo.addItem(list[i]);
                if(RunnerRepository.getDefaultLook().equals(list[i])){
                    index = i;}}
            catch(Exception e){continue;}}
        return index;}
        
    /*
     * panel displayed on
     * twister startup for user and password
     * input
     */    
    public static JPanel getPasswordPanel(JTextField jTextField1,
                JPasswordField jTextField2,final JComboBox combo){
        final JCheckBox check = new JCheckBox("Default");
        check.setSelected(true);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JPanel jPanel1 = new JPanel();
        JLabel jLabel3 = new JLabel();
        JPanel jPanel2 = new JPanel();
        JLabel jLabel4 = new JLabel();
        JPanel jPanel5 = new JPanel();
        jPanel5.add(combo);
        jPanel5.add(check);
        jPanel1.setLayout(new java.awt.BorderLayout());
        jLabel3.setText("User: ");
        jPanel1.add(jLabel3, BorderLayout.CENTER);
        p.add(jPanel1);
        p.add(jTextField1);
        jPanel2.setLayout(new BorderLayout());
        jLabel4.setText("Password: ");
        jPanel2.add(jLabel4, BorderLayout.CENTER);
        p.add(jPanel2);
        p.add(jTextField2);
        p.add(jPanel5);
        combo.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent evt){
                if(evt.getStateChange() == ItemEvent.SELECTED){
                    if(RunnerRepository.getDefaultLook().equals(evt.getItem().toString()))
                        check.setSelected(true);
                    else check.setSelected(false);}}});
        check.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev){
                if(getLooks()!=null){
                    if(check.isSelected())
                        RunnerRepository.setDefaultLook(combo.getSelectedItem().toString());
                    else RunnerRepository.setDefaultLook("MetalLookAndFeel");}}});
        return p;}
        
    /*
     * Twister icons
     */    
    public static Image getSuitaIcon(){
        return suitaicon;}
    
    public static Image getFailIcon(){
        return failicon;}
        
    public static Image getPendingIcon(){
        return pendingicon;}
        
    public static Image getWorkingIcon(){
        return workingicon;}
        
    public static Image getNotExecIcon(){
        return notexecicon;}
        
    public static Image getTimeoutIcon(){
        return timeouticon;}
        
    public static Image getSkippedIcon(){
        return skipicon;}
        
    public static Image getWaitingIcon(){
        return waiticon;}
        
    public static Image getStopIcon(){
        return stopicon;}
        
    public static Image getTestBedIcon(){
        return testbedicon;}
        
     public static Image getStoppedIcon(){
        return stoppedicon;}
        
    public static Image getPassIcon(){
        return passicon;}
        
    public static Image getTCIcon(){
        return tcicon;}
        
    public static Image getPlayIcon(){
        return playicon;}
        
    public static String getBar(){
        return bar;}
        
    public static Image getPropertyIcon(){
        return propicon;}
        
    public static Image getPasswordIcon(){
        return passwordicon;}
        
    /*
     * looks saved in conf file
     */
    private static JsonObject getLooks(){
        return looks;}
        
    /*
     * plugins saved in conf file
     */
    public static JsonArray getPlugins(){
        return plugins;}
        
    /*
     * default look name
     * saved in json list
     * 
     */ 
    public static String getDefaultLook(){
        return getLooks().get("DEFAULT").getAsJsonPrimitive().getAsString();}
        
    /*
     * write default look
     * in json list and in local conf     * 
     */
    public static void setDefaultLook(String look){
        addLook(new String[]{"DEFAULT",look});
        writeJSon();}
        
    /*
     * add user defined look to list
     * of looks
     */
    public static void addLook(String [] look){
        getLooks().add(look[0],new JsonPrimitive(look[1]));
        writeJSon();}
        
        
    /*
     * method to remove plugin
     * from inifile based on filename of plugin
     */
    public static void removePlugin(String filename){
        JsonArray array = new JsonArray();
        int size = getPlugins().size();
        for(int i=0;i<size;i++){
            if(getPlugins().get(i).getAsString().equals(filename))continue;
            array.add(getPlugins().get(i));}
        plugins = array;
        inifile.add("plugins", array);
        writeJSon();}
        
    /*
     * editors saved in conf file
     */
    public static JsonObject getEditors(){
        return editors;}
        
        
    /*
     * layout saved in conf file
     */
    public static JsonObject getLayouts(){
        return layout;}
      
    /*
     * delete editor from editors list
     * and save file
     */        
    public static void removeEditor(String editor){
        editors.remove(editor);
        writeJSon();}
        
    /*
     * add user defined editor to list
     * of editors
     */
    public static void addEditor(String [] editor){
        getEditors().add(editor[0],new JsonPrimitive(editor[1]));
        writeJSon();}
        
    /*
     * add plugin to list
     * of plugins
     */
    public static void addPlugin(String pluginfilename){
        if(getPlugins().isJsonArray()){
            getPlugins().getAsJsonArray().add(new JsonPrimitive(pluginfilename));}
        else{JsonPrimitive primitive = new JsonPrimitive(pluginfilename);
            JsonArray array = new JsonArray();
            array.add(primitive);
            inifile.add("plugins",array);}
        writeJSon();}
    /*
     * default editor name
     * saved in json list
     * 
     */ 
    public static String getDefaultEditor(){
        return getEditors().get("DEFAULT").getAsJsonPrimitive().getAsString();}
        
    /*
     * write default editor
     * in json list and in local conf     * 
     */
    public static void setDefaultEditor(String editor){
        addEditor(new String[]{"DEFAULT",editor});
        writeJSon();}
        
    /*
     * write local conf 
     * with saved json
     */
    public static void writeJSon(){        
        try{Writer writer = new OutputStreamWriter(new FileOutputStream(TWISTERINI));
            Gson gson = new GsonBuilder().setPrettyPrinting().create(); 
            gson.toJson(inifile, writer);
            writer.close();}
        catch(Exception e){
            System.out.println("Could not write to local config file");
            e.printStackTrace();}}
    
    /*
     * logs tracked by twister framwork
     */
    public static ArrayList<String> getLogs(){
        return logs;}
        
    /*
     * RPC connection
     */
    public static XmlRpcClient getRPCClient(){
        return client;}

    /*
     * user used on twister server
     */
    public static String getUser(){
        return user;}
    
    /*
     * variables as hashtable used
     * by plugins
     */
    public static Hashtable getVariables(){
        return variables;}
    
    /*
     * method to get pluginsconfig Document
     */  
    public static Document getPluginsConfig(){
        return pluginsconfig;
    }
    
    public static boolean removeRemoteFile(String file){
        while(sftpoccupied){
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
        }
        sftpoccupied = true;
        try{connection.rm(file);
            sftpoccupied = false;
            return true;
        } catch (Exception e){
            e.printStackTrace();
            sftpoccupied = false;
            System.out.println("Could not delete:"+file+" file on server");
            return false;
        }
    }
    
    public static String [] getRemoteFolderContent(String folder){
        while(sftpoccupied){
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
        }
        sftpoccupied = true;
        int size;
        Vector v=null;
        try{v = connection.ls(folder);
            size = v.size();}
        catch(Exception e){
            System.out.println("No files found in: "+folder+" directory");
            size=0;}
        ArrayList<String> files = new ArrayList<String>();
        String name=null;
        for(int i=0;i<size;i++){
            try{name = ((LsEntry)v.get(i)).getFilename();
                if(name.split("\\.").length==0)continue;
                files.add(name.toString());
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        sftpoccupied = false;
        String [] resp = new String[files.size()];
        files.toArray(resp);
        return resp;
    }
    
    public static String getRemoteFileContent(String file){
        while(sftpoccupied){
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
        }
        sftpoccupied = true;
        String line = null;
        try{InputStream in = RunnerRepository.connection.get(file);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder b=new StringBuilder();
            while ((line=bufferedReader.readLine())!= null){
                    b.append(line);
                    b.append("\n");
            }
            bufferedReader.close();
            inputStreamReader.close();
            in.close();
            sftpoccupied = false;
            return b.toString();
        } catch (Exception e){
            e.printStackTrace();
            sftpoccupied = false;
            return null;
        }
    }
    
    public static boolean uploadRemoteFile(String location,FileInputStream input,String filename){
        if(location==null || location.equals("")){
            CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE,RunnerRepository.window,
                                    "Warning", "No location provided to upload the file");
            return false;
        }
        while(sftpoccupied){
            try{Thread.sleep(100);}
            catch(Exception e){e.printStackTrace();}
        }
        sftpoccupied = true;
//         try{RunnerRepository.connection.cd(location);}
//         catch(Exception e){
//             CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE,RunnerRepository.window,
//                                         "Warning", "Could not upload remote to :"+location+" location");
//             e.printStackTrace();
//             sftpoccupied = false;
//             return false;
//         }
        try{
            System.out.println(location+"/"+filename);
            RunnerRepository.connection.put(input,location+"/"+filename);
            
            input.close();
            sftpoccupied = false;
            return true;
        } catch (Exception e){
            CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE,RunnerRepository.window,
                                        "Warning", "Could not upload :"+filename+" file");
            e.printStackTrace();
            try{input.close();}
            catch(Exception ex){ex.printStackTrace();}
            sftpoccupied = false;
            return false;
        }
    }
        
    /* 
     * method to load plugins config from file to
     * pluginsconfig Node
     */
    public static boolean parsePluginsConfig(String filename){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try{DocumentBuilder db = dbf.newDocumentBuilder();
            File f = new File(filename);
            if(!f.exists())return false;
            Document doc = db.parse(f);
            doc.getDocumentElement().normalize();  
            pluginsconfig = doc;
            return true;
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    
    /*
     * save the layouts of the frameworks
     * for reuse on opening
     */
    public static void saveMainLayout(){
        JsonObject lay = getLayouts();
        lay.add("mainsize", new JsonPrimitive(window.getSize().getWidth()+" "+window.getSize().getHeight()));
        lay.add("mainlocation", new JsonPrimitive(window.getLocation().getX()+" "+window.getLocation().getY()));
        lay.add("mainvsplitlocation", new JsonPrimitive(window.mainpanel.p1.splitPane.getDividerLocation()));
        lay.add("mainh1splitlocation", new JsonPrimitive(window.mainpanel.p1.splitPane3.getDividerLocation()));
        lay.add("mainh2splitlocation", new JsonPrimitive(window.mainpanel.p1.splitPane2.getDividerLocation()));
        writeJSon();
    }
        
        
    /*
     * save the layouts of UT window
     * for reuse on opening
     */
    public static void saveUTLayout(Dimension size, Point p,int divloc){
        JsonObject lay = getLayouts();
        lay.add("UTsize", new JsonPrimitive(size.getWidth()+" "+size.getHeight()));
        lay.add("UTlocation", new JsonPrimitive(p.getX()+" "+p.getY()));
        lay.add("UTh1splitlocation", new JsonPrimitive(divloc));
        writeJSon();
    }
    
    
    /*
     * open project file from server
     */
    public static void openProjectFile(){
        int size;
        Vector v=null;
        try{v = connection.ls(REMOTEUSERSDIRECTORY);
            size = v.size();}
        catch(Exception e){
            System.out.println("No suites xml found in: "+REMOTEUSERSDIRECTORY+" directory");
            size=0;}
        ArrayList<String> files = new ArrayList<String>();
        String name=null;
        
        for(int i=0;i<size;i++){
            try{name = ((LsEntry)v.get(i)).getFilename();
                if(name.split("\\.").length==0)continue; 
                if(name.toLowerCase().indexOf(".xml")==-1)continue;
                if(name.equals("last_edited.xml"))continue;
                files.add(name.toString());
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        
        
        String users[] = new String[files.size()];
        
        if(PermissionValidator.canCreateProject()){
            users = new String[files.size()+1];
            for(int i=0;i<files.size();i++){
                users[i] = files.get(i);
            }
            users[users.length - 1] = "New File";
        } else {
            for(int i=0;i<files.size();i++){
                users[i] = files.get(i);
            }
        }
        JComboBox combo = new JComboBox(users);
        
        int resp = (Integer)CustomDialog.showDialog(combo,
                            JOptionPane.INFORMATION_MESSAGE,
                            JOptionPane.OK_CANCEL_OPTION,window,
                            "Project File",null);
        
        if(resp==JOptionPane.OK_OPTION){
            String user = combo.getSelectedItem().toString();
            if(user.equals("New File")){
                user = CustomDialog.showInputDialog(JOptionPane.QUESTION_MESSAGE,
                                                    JOptionPane.OK_CANCEL_OPTION, window,
                                                    "File Name", "Please enter file name");
                if(!user.equals("NULL")){
                    RunnerRepository.emptySuites();
                    RunnerRepository.window.mainpanel.p1.sc.g.getSelectedCollection().clear();
                    (new XMLBuilder(RunnerRepository.getSuite())).writeXMLFile((new StringBuilder()).
                                        append(RunnerRepository.getUsersDirectory()).append(RunnerRepository.
                                        getBar()).append(user).append(".XML").toString(),false,false,false);
                    window.mainpanel.p1.sc.g.setUser((new StringBuilder()).append(RunnerRepository.getUsersDirectory()).
                                        append(RunnerRepository.getBar()).append(user).append(".XML").
                                        toString());
                    window.mainpanel.p1.sc.g.printXML( window.mainpanel.p1.sc.g.getUser(),false,false,false,false,false,"",false,null);
                    RunnerRepository.window.mainpanel.p1.suitaDetails.setPreScript("");
                    RunnerRepository.window.mainpanel.p1.suitaDetails.setPostScript("");
                    RunnerRepository.window.mainpanel.p1.suitaDetails.setGlobalLibs(null);
                    RunnerRepository.window.mainpanel.p1.suitaDetails.setDelay("");
                    RunnerRepository.window.mainpanel.p1.suitaDetails.setStopOnFail(false);
                    RunnerRepository.window.mainpanel.p1.suitaDetails.setSaveDB(false);
                }}
            else{
                try{
                    String content = RunnerRepository.getRemoteFileContent(REMOTEUSERSDIRECTORY+"/"+user);                    
                    File file = new File(temp+bar+"Twister"+bar+"Users"+bar+user);
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(content);
                    writer.close();                   
                    
                    RunnerRepository.emptySuites();
                    window.mainpanel.p1.sc.g.setUser((new StringBuilder()).append(RunnerRepository.getUsersDirectory()).
                                            append(RunnerRepository.getBar()).append(user).toString());
                    window.mainpanel.p1.sc.g.parseXML(new File((new StringBuilder()).append(RunnerRepository.getUsersDirectory()).
                                            append(RunnerRepository.getBar()).append(user).toString()));}
                catch(Exception e){
                    e.printStackTrace();
            }}
            if(RunnerRepository.getSuiteNr() > 0){
                RunnerRepository.window.mainpanel.p1.sc.g.updateLocations(RunnerRepository.getSuita(0));}
                RunnerRepository.window.mainpanel.p1.sc.g.repaint();
            }
            RunnerRepository.window.mainpanel.p1.sc.g.selectedcollection.clear();
    }
    
    public static String getVersion(){
        return version;
    }
    
    public static String getBuildDate(){
        return builddate;
    }
    
    /*
     * XmlRpc main connection used by Twister framework
     */
    public static void initializeRPC(){
        try{XmlRpcClientConfigImpl configuration = new XmlRpcClientConfigImpl();
            configuration.setBasicPassword(password);
            configuration.setBasicUserName(user);
            configuration.setServerURL(new URL("http://"+user+":"+password+"@"+RunnerRepository.host+
                                        ":"+RunnerRepository.getCentralEnginePort()+"/"));
            client = new XmlRpcClient();
            client.setConfig(configuration);
            System.out.println("Client initialized: "+client);}
        catch(Exception e){System.out.println("Could not conect to "+
                            RunnerRepository.host+" :"+RunnerRepository.getCentralEnginePort()+
                            "for RPC client initialization");}
    }
        
    /*
     * method to copy plugins configuration file
     * localy 
     */
    public static boolean getPluginsFile(){
        try{InputStream in = null;
            byte[] data = new byte[100]; 
            int nRead;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            OutputStream out=null;
            InputStreamReader inputStreamReader = null;
            BufferedReader bufferedReader = null;  
            BufferedWriter writer=null;
            File file;
            String line = null;
            String name = null;
            System.out.println("Starting getting plugins.xml from "+USERHOME+"/twister/config/");
            
            if(RunnerRepository.getRemoteFolderContent(USERHOME+"/twister/config/").length==0){
                System.out.println("Could not get :"+USERHOME+"/twister/config/");
                CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE,RunnerRepository.window,
                                        "Warning", "Could not get config folder from: "+USERHOME+
                                        "/twister/config/");
                return false;
            }
            
            String content = RunnerRepository.getRemoteFileContent(USERHOME+"/twister/config/plugins.xml");
            if(content==null){
                CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, RunnerRepository.window,
                                        "Warning","Could not get plugins.xml from "+
                                        USERHOME+"/twister/config/");
                return false;
            }

            file = new File(PLUGINSLOCALGENERALCONF);
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();

            introscreen.setStatus("Finished getting plugins");
            introscreen.repaint();
            return true;
        }
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
}    
