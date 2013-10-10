/*
File: DBConfig.java ; This file is part of Twister.
Version: 2.004

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
import java.awt.Dimension;
import javax.swing.border.BevelBorder;
import javax.swing.BorderFactory;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.FileInputStream;
import java.io.File;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import javax.swing.JPasswordField;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.swing.JOptionPane;
import com.twister.CustomDialog;
import java.io.BufferedWriter;
import java.io.FileWriter;
import javax.xml.bind.DatatypeConverter;

public class DBConfig extends JPanel{
    Document doc;
    File theone;
    private JTextField tdatabase,tserver,tuser;
    JPasswordField tpassword;
    DatabaseInterface databaseinterface;

    public DBConfig(){
        databaseinterface = new DatabaseInterface();
        add(databaseinterface);
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        setBackground(Color.WHITE);
        JLabel file = new JLabel("File: ");
        file.setBounds(15,10,50,20);
        final JTextField tfile = new JTextField();
        tfile.setBounds(100,10,170,25);
        JButton browse = new JButton("Browse");
        browse.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev){
                JFileChooser chooser = new JFileChooser(); 
                chooser.setFileFilter(new XMLFilter());
                chooser.setCurrentDirectory(new java.io.File("."));
                chooser.setDialogTitle("Select XML File"); 
                if (chooser.showOpenDialog(RunnerRepository.window) == JFileChooser.APPROVE_OPTION) {                     
                    File f = chooser.getSelectedFile();
                    try{tfile.setText(f.getCanonicalPath());}
                    catch(Exception e){e.printStackTrace();}}}});
        browse.setBounds(275,13,90,20);
        JButton upload = new JButton("Upload");
        upload.setBounds(375,13,90,20);
        upload.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev){
                boolean saved = true;
                try{File f = new File(tfile.getText());
                    FileInputStream stream = new FileInputStream(f);
                    RunnerRepository.uploadRemoteFile(RunnerRepository.REMOTEDATABASECONFIGPATH, stream, f.getName());
                    
                    Files.copy(f.toPath(), new File(RunnerRepository.getConfigDirectory()+
                    RunnerRepository.getBar()+f.getName()).toPath(), REPLACE_EXISTING);
                    RunnerRepository.resetDBConf(f.getName(),false);}
                catch(Exception e){
                    saved = false;
                    e.printStackTrace();}
                if(saved){
                    CustomDialog.showInfo(JOptionPane.INFORMATION_MESSAGE, 
                                            DBConfig.this, "Successful", 
                                            "File successfully uploaded");}
                else{
                    CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, 
                                            DBConfig.this, "Warning", 
                                            "File could not uploaded");}}});
        JLabel database = new JLabel("Database: ");
        database.setBounds(15,55,90,20);
        tdatabase = new JTextField();
        tdatabase.setBounds(100,55,170,25);
        JLabel server = new JLabel("Server: ");
        server.setBounds(15,80,90,20);
        tserver = new JTextField();
        tserver.setBounds(100,80,170,25);
        JLabel user = new JLabel("User: ");
        user.setBounds(15,105,50,20);
        tuser = new JTextField();
        tuser.setBounds(100,105,170,25);
        JLabel password = new JLabel("Password: ");
        password.setBounds(15,130,90,20);
        tpassword = new JPasswordField();
        tpassword.setBounds(100,130,170,25);
        refresh();
        JButton save = new JButton("Save");
        save.setBounds(200,155,70,20);
        save.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ev){
                if(doc!=null){
                    if(tpassword.getPassword().length == 0){
                        CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, 
                                                DBConfig.this, "Warning", 
                                                "Warning, password not set");}
                    boolean saved = true;
                    try{theone = new File(RunnerRepository.temp+RunnerRepository.getBar()+"Twister"+
                        RunnerRepository.getBar()+"config"+RunnerRepository.getBar()+new File(
                        RunnerRepository.REMOTEDATABASECONFIGFILE).getName());
                        try{NodeList nodeLst = doc.getElementsByTagName("server");
                            if(nodeLst.item(0).getChildNodes().getLength()>0)nodeLst.
                                item(0).getChildNodes().item(0).setNodeValue(tserver.getText());
                            else nodeLst.item(0).appendChild(doc.createTextNode(
                                tserver.getText()));
                            nodeLst = doc.getElementsByTagName("database");
                            if(nodeLst.item(0).getChildNodes().getLength()>0)nodeLst.
                                item(0).getChildNodes().item(0).setNodeValue(tdatabase.
                                getText());
                            else nodeLst.item(0).appendChild(doc.createTextNode(tdatabase.
                                getText()));
                            nodeLst = doc.getElementsByTagName("user");
                            if(nodeLst.item(0).getChildNodes().getLength()>0)nodeLst.
                                item(0).getChildNodes().item(0).setNodeValue(tuser.getText());
                            else nodeLst.item(0).appendChild(doc.createTextNode(tuser.
                                getText()));
                            if(tpassword.getPassword().length != 0 && !(new String(
                                tpassword.getPassword()).equals("****"))){
                                    nodeLst = doc.getElementsByTagName("password");
                                    
                                    String p = "";
                                    byte mydata[]=new String(tpassword.getPassword()).getBytes();
                                    try{p = DatatypeConverter.printBase64Binary(mydata);}
                                    catch(Exception e){e.printStackTrace();}
                                    
                                    if(nodeLst.item(0).getChildNodes().getLength()>0)nodeLst.
                                        item(0).getChildNodes().item(0).setNodeValue(p);
                                    else nodeLst.item(0).appendChild(doc.createTextNode(p));}}
                        catch(Exception e){
                            saved = false;
                            System.out.println(doc.getDocumentURI()+
                            " may not be properly formatted");}
                        Result result = new StreamResult(theone);
                        try{DOMSource source = new DOMSource(doc);
                            TransformerFactory transformerFactory = TransformerFactory.
                            newInstance();
                            Transformer transformer = transformerFactory.newTransformer();
                            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");                        
                            transformer.transform(source, result);
                            FileInputStream input = new FileInputStream(theone);
                            RunnerRepository.uploadRemoteFile(RunnerRepository.REMOTEDATABASECONFIGPATH, input, theone.getName());
                        }
                        catch(Exception e){
                            saved = false;
                            e.printStackTrace();
                            System.out.println("Could not save in file : "+RunnerRepository.
                            temp+RunnerRepository.getBar()+"Twister"+RunnerRepository.getBar()+"Config"+
                            RunnerRepository.getBar()+RunnerRepository.REMOTEDATABASECONFIGFILE+" and send to "+
                            RunnerRepository.REMOTEDATABASECONFIGPATH);}}
                    catch(Exception e){
                        saved = false;
                        e.printStackTrace();}
                    if(saved){
                        CustomDialog.showInfo(JOptionPane.INFORMATION_MESSAGE, 
                                                DBConfig.this, "Successful", 
                                                "File successfully saved");}
                    else{
                        CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, 
                                                DBConfig.this, "Warning", 
                                                "File could not be saved ");}}}});
    }
        
    private void refresh(){
        try{
            System.out.println("refreshing database");
            tserver.setText("");
            tdatabase.setText("");
            tpassword.setText("");
            tuser.setText("");
            theone = new File(RunnerRepository.temp+RunnerRepository.getBar()+"Twister"+RunnerRepository.
                            getBar()+"config"+RunnerRepository.getBar()+
                            new File(RunnerRepository.REMOTEDATABASECONFIGFILE).getName());
            String content = RunnerRepository.getRemoteFileContent(RunnerRepository.REMOTEDATABASECONFIGPATH+RunnerRepository.REMOTEDATABASECONFIGFILE);
            BufferedWriter writer = new BufferedWriter(new FileWriter(theone));
            writer.write(content);
            writer.close();
            try{DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();                                        
                doc = db.parse(theone);
                doc.getDocumentElement().normalize();
                NodeList nodeLst = doc.getElementsByTagName("server");
                tserver.setText(nodeLst.item(0).getChildNodes().item(0).getNodeValue());
                nodeLst = doc.getElementsByTagName("database");
                tdatabase.setText(nodeLst.item(0).getChildNodes().item(0).getNodeValue());
                nodeLst = doc.getElementsByTagName("password");
                tpassword.setText(nodeLst.item(0).getChildNodes().item(0).getNodeValue());
                if(!tpassword.getPassword().equals(""))tpassword.setText("****");
                nodeLst = doc.getElementsByTagName("user");
                tuser.setText(nodeLst.item(0).getChildNodes().item(0).getNodeValue());}
            catch(Exception e){
                System.out.println(RunnerRepository.temp+RunnerRepository.getBar()+
                "Twister"+RunnerRepository.getBar()+"Config"+RunnerRepository.getBar()+new File(RunnerRepository.
                REMOTEDATABASECONFIGFILE).getName()+" is corrupted or incomplete");
                e.printStackTrace();}}
        catch(Exception e){
            //CustomDialog.showInfo(JOptionPane.INFORMATION_MESSAGE, DBConfig.this, "info", e.getMessage());
            e.printStackTrace();
            System.out.println("Could not refresh dbconfig structure");}}}
