/*
File: XMLBuilder.java ; This file is part of Twister.
Version: 2.009

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
import com.twister.Item;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;
import java.io.File;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Result;
import java.io.FileInputStream;
import java.util.Iterator;
import javax.swing.JOptionPane;
import com.twister.CustomDialog;
import javax.swing.tree.DefaultMutableTreeNode;

public class XMLBuilder{
    private DocumentBuilderFactory documentBuilderFactory;
    private Document document;
    private TransformerFactory transformerFactory;
    private Transformer transformer;
    private DOMSource source;
    private ArrayList <Item> suite;
    private boolean skip;

    public XMLBuilder(ArrayList <Item> suite){
        try{documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            document = documentBuilder.newDocument();
            transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            source = new DOMSource(document);
            this.suite = suite;}
        catch(ParserConfigurationException e){
            System.out.println("DocumentBuilder cannot be created which satisfies the"+
                                " configuration requested");}
        catch(TransformerConfigurationException e){
            System.out.println("Could not create transformer");}}
        
    public boolean getRunning(Item item){
        if(item.getType()==1){
            if(item.getSubItem(0).getValue().equals("true")){
                return true;}
            else return false;}
        else{
            int subitemsnr = item.getSubItemsNr();
            for(int i=0;i<subitemsnr;i++){
                if(getRunning(item.getSubItem(i)))return true;}
            return false;}}
        
    public void createXML(boolean skip, boolean stoponfail,
                          boolean prestoponfail,
                          boolean temp, String prescript, String postscript,
                          boolean savedb, String delay, String[] globallibs){//skip checks if it is user or test xml
        this.skip = skip;
        Element root = document.createElement("Root");
        document.appendChild(root);
        Element em2 = document.createElement("stoponfail");;
        if(stoponfail){
            em2.appendChild(document.createTextNode("true"));
        } else {
            em2.appendChild(document.createTextNode("false"));
        }
        root.appendChild(em2);
        em2 = document.createElement("PrePostMandatory");;
        if(prestoponfail){
            em2.appendChild(document.createTextNode("true"));
        } else {
            em2.appendChild(document.createTextNode("false"));
        }
        root.appendChild(em2);
        em2 = document.createElement("ScriptPre");
        em2.appendChild(document.createTextNode(prescript));
        root.appendChild(em2);
        em2 = document.createElement("ClearCaseView");
        em2.appendChild(document.createTextNode(RunnerRepository.window.mainpanel.getP5().view));
        root.appendChild(em2);
        em2 = document.createElement("libraries");
        StringBuilder sb = new StringBuilder();
        if(globallibs!=null){
            for(String s:globallibs){
                sb.append(s);
                sb.append(";");
            }
        }
        em2.appendChild(document.createTextNode(sb.toString()));
        root.appendChild(em2);
        em2 = document.createElement("ScriptPost");
        em2.appendChild(document.createTextNode(postscript));
        root.appendChild(em2);
        
        em2 = document.createElement("dbautosave");;
        if(savedb){
            em2.appendChild(document.createTextNode("true"));
        } else {
            em2.appendChild(document.createTextNode("false"));
        }
        root.appendChild(em2);
        em2 = document.createElement("tcdelay");
        em2.appendChild(document.createTextNode(delay));
        root.appendChild(em2);
        int nrsuite = suite.size();
        if(skip && nrsuite>0){
             ArrayList <Item> temporary = new <Item> ArrayList();
             String [] EPS;
             
            DefaultMutableTreeNode parent = RunnerRepository.window.mainpanel.p4.getSut().sut.root;
            int sutsnr = parent.getChildCount();             
             for(int i=0;i<nrsuite;i++){
                 sb.setLength(0);
                 Item current = suite.get(i);
                 for(String s:current.getEpId()){
//                     Iterator iter = parent.getChildren().keySet().iterator();
                    for(int j=0;j<sutsnr;j++){
                        SUT child = (SUT)((DefaultMutableTreeNode)parent.getChildAt(j)).getUserObject();
                        if(child!=null&&child.getName().equals(s)){
                            for(String ep:child.getEPs().split(";")){
                                Item item = current.clone();
                                String []str = {ep,child.getName()};
                                item.setEpId(str);
                                temporary.add(item);
                            }
                        }
                    }
                }
                 
                 
//                  Node parent = RunnerRepository.window.mainpanel.p4.getTB().getParentNode();
//                  for(String s:current.getEpId()){
//                     Iterator iter = parent.getChildren().keySet().iterator();
//                     while(iter.hasNext()){
//                         Node child = parent.getChild(iter.next().toString());
//                         if(child!=null&&child.getName().equals(s)){
//                             for(String ep:child.getEPs().split(";")){
//                                 Item item = current.clone();
//                                 String []str = {ep,child.getName()};
//                                 item.setEpId(str);
//                                 temporary.add(item);
//                             }
//                         }
//                     }
//                 }
             }
             suite = temporary;
             nrsuite = suite.size();
        }
        for(int i=0;i<nrsuite;i++){
            int nrtc = suite.get(i).getSubItemsNr();
            boolean go = false;
            if(!temp && skip){
                for(int j=0;j<nrtc;j++){
                    if(getRunning(suite.get(i))){
                        go=true;
                        break;}}}
            if(!go&&skip&&!temp)continue;
            Element rootElement = document.createElement("TestSuite");
            root.appendChild(rootElement);
            
            if(suite.get(i).getLibs()!=null&&suite.get(i).getLibs().length>0){
                em2 = document.createElement("libraries");
                sb.setLength(0);
                for(String s:suite.get(i).getLibs()){
                    sb.append(s);
                    sb.append(";");
                }
                em2.appendChild(document.createTextNode(sb.toString()));
                rootElement.appendChild(em2);
            }
            em2 = document.createElement("tsName");
            em2.appendChild(document.createTextNode(suite.get(i).getName()));
            rootElement.appendChild(em2);
            em2 = document.createElement("PanicDetect");
            em2.appendChild(document.createTextNode(suite.get(i).isPanicdetect()+""));
            rootElement.appendChild(em2);
            if(suite.get(i).getEpId()!=null&&suite.get(i).getEpId().length>0){
                if(skip){
                    Element EP = document.createElement("EpId");
                    EP.appendChild(document.createTextNode(suite.get(i).getEpId()[0]));
                    rootElement.appendChild(EP);
                    EP = document.createElement("SutName");
                    EP.appendChild(document.createTextNode(suite.get(i).getEpId()[1]));
                    rootElement.appendChild(EP);
                }
                else {
                    StringBuilder b = new StringBuilder();
                    for(String s:suite.get(i).getEpId()){
                        b.append(s+";");
                    }
                    b.deleteCharAt(b.length()-1);
                    Element EP = document.createElement("SutName");
                    EP.appendChild(document.createTextNode(b.toString()));
                    rootElement.appendChild(EP);
                    EP = document.createElement("EpId");
                    Node parent = RunnerRepository.window.mainpanel.p4.getTB().getParentNode();
                    b.setLength(0);
                    
                    
                    DefaultMutableTreeNode noderoot = RunnerRepository.window.mainpanel.p4.getSut().sut.root;
                    int sutsnr = noderoot.getChildCount();
                    
                    for(String s:suite.get(i).getEpId()){
                        
                        
                        for(int j=0;j<sutsnr;j++){
                            SUT child = (SUT)((DefaultMutableTreeNode)noderoot.getChildAt(j)).getUserObject();
                            if(child!=null&&child.getName().equals(s)){
                        
                        
//                         Iterator iter = parent.getChildren().keySet().iterator();
//                         while(iter.hasNext()){
//                             Node child = parent.getChild(iter.next().toString());
//                             if(child!=null&&child.getName().equals(s)){
                                
                                
                                if(child.getEPs()==null || child.getEPs().equals("")){
                                    CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, 
                                                            RunnerRepository.window, "Warning", 
                                                            "Warning, no ep's found for: "+child.getName());
                                }
                                else {
                                    for(String ep:child.getEPs().split(";")){
                                        b.append(ep);
                                        b.append(";");
                                    }
                                    b.deleteCharAt(b.length()-1);
                                }
                            }
                        }
                    }
                    
                    
                    
//                     for(String s:suite.get(i).getEpId()){
//                         Iterator iter = parent.getChildren().keySet().iterator();
//                         while(iter.hasNext()){
//                             Node child = parent.getChild(iter.next().toString());
//                             if(child!=null&&child.getName().equals(s)){
//                                 if(child.getEPs()==null || child.getEPs().equals("")){
//                                     CustomDialog.showInfo(JOptionPane.WARNING_MESSAGE, 
//                                                             RunnerRepository.window, "Warning", 
//                                                             "Warning, no ep's found for: "+child.getName());
//                                 }
//                                 else {
//                                     for(String ep:child.getEPs().split(";")){
//                                         b.append(ep);
//                                         b.append(";");
//                                     }
//                                     b.deleteCharAt(b.length()-1);
//                                 }
//                             }
//                         }
//                     }
                    
                    
                    EP.appendChild(document.createTextNode(b.toString()));
                    rootElement.appendChild(EP);
                }
            }
            for(int j=0;j<suite.get(i).getUserDefNr();j++){
                Element userdef = document.createElement("UserDefined");
                Element pname = document.createElement("propName");
                pname.appendChild(document.createTextNode(suite.get(i).getUserDef(j)[0]));
                userdef.appendChild(pname);
                Element pvalue = document.createElement("propValue");
                pvalue.appendChild(document.createTextNode(suite.get(i).getUserDef(j)[1]));
                userdef.appendChild(pvalue);
                rootElement.appendChild(userdef);}
            for(int j=0;j<nrtc;j++){
                addSubElement(rootElement,suite.get(i).getSubItem(j),skip,temp);            
            }
        }
    }
            
            
//     public void createTempXML(){//skip verifica daca e user xml sau xml final
//         Element root = document.createElement("Root");
//         document.appendChild(root);
//         int nrsuite = suite.size();        
//         for(int i=0;i<nrsuite;i++){
//             int nrtc = suite.get(i).getSubItemsNr();
// //             boolean go = false;
// //             if(skip){
// //                 for(int j=0;j<nrtc;j++){
// //                     if(getRunning(suite.get(i))){
// //                         go=true;
// //                         break;}}}
// //             if(!go&&skip)continue;   
// //             if(stoponfail){
// //                 Element em2 = document.createElement("stoponfail");
// //                 em2.appendChild(document.createTextNode("true"));
// //                 root.appendChild(em2);}
//             Element rootElement = document.createElement("TestSuite");
//             root.appendChild(rootElement);
//             Element em2 = document.createElement("tsName");
//             em2.appendChild(document.createTextNode(suite.get(i).getName()));
//             rootElement.appendChild(em2);
//             if(suite.get(i).getEpId()!=null&&!suite.get(i).getEpId().equals("")){
//                 Element EP = document.createElement("EpId");
//                 EP.appendChild(document.createTextNode(suite.get(i).getEpId()));
//                 rootElement.appendChild(EP);}
//             for(int j=0;j<suite.get(i).getUserDefNr();j++){
//                 Element userdef = document.createElement("UserDefined");
//                 Element pname = document.createElement("propName");
//                 pname.appendChild(document.createTextNode(suite.get(i).getUserDef(j)[0]));
//                 userdef.appendChild(pname);
//                 Element pvalue = document.createElement("propValue");
//                 pvalue.appendChild(document.createTextNode(suite.get(i).getUserDef(j)[1]));
//                 userdef.appendChild(pvalue);
//                 rootElement.appendChild(userdef);}
//             for(int j=0;j<nrtc;j++){                
//                 addTempSubElement(rootElement,suite.get(i).getSubItem(j));
//             }}}
//             
//     public void addTempSubElement(Element rootelement, Item item){
//         if(item.getType()==0){
//             Element prop = document.createElement("Property");
//             rootelement.appendChild(prop);
//             Element em4 = document.createElement("propName");
//             em4.appendChild(document.createTextNode(item.getName()));
//             prop.appendChild(em4);
//             Element em5 = document.createElement("propValue");
//             em5.appendChild(document.createTextNode(item.getValue()));
//             prop.appendChild(em5);}
//         else if(item.getType()==1){
//             Element tc  = document.createElement("TestCase");
//             rootelement.appendChild(tc);
//             Element em3 = document.createElement("tcName");
//             em3.appendChild(document.createTextNode(item.getFileLocation()));
//             tc.appendChild(em3);
//             
// //             Element em6 = document.createElement("tcID");
// //             em6.appendChild(document.createTextNode(id+""));
// //             id++;
// //             tc.appendChild(em6);
//             Element em7 = document.createElement("Title");
//             em7.appendChild(document.createTextNode(""));
//             tc.appendChild(em7);
//             Element em8 = document.createElement("Summary");
//             em8.appendChild(document.createTextNode(""));
//             tc.appendChild(em8);
//             Element em9 = document.createElement("Priority");
//             em9.appendChild(document.createTextNode("Medium"));
//             tc.appendChild(em9);
//             Element em10 = document.createElement("Dependancy");
//             em10.appendChild(document.createTextNode(" "));
//             tc.appendChild(em10);
//             
//             if(item.isPrerequisite()){
//                 Element prop  = document.createElement("Property");
//                 tc.appendChild(prop);
//                 Element em4 = document.createElement("propName");
//                 em4.appendChild(document.createTextNode("setup_file"));
//                 prop.appendChild(em4);
//                 Element em5 = document.createElement("propValue");
//                 em5.appendChild(document.createTextNode("true"));
//                 prop.appendChild(em5);}
//             if(item.isOptional()){
//                 Element prop  = document.createElement("Property");
//                 tc.appendChild(prop);
//                 Element em4 = document.createElement("propName");
//                 em4.appendChild(document.createTextNode("Optional"));
//                 prop.appendChild(em4);
//                 Element em5 = document.createElement("propValue");
//                 em5.appendChild(document.createTextNode("true"));
//                 prop.appendChild(em5);}
//             Element prop  = document.createElement("Property");
//             tc.appendChild(prop);
//             Element em4 = document.createElement("propName");
//             em4.appendChild(document.createTextNode("Runnable"));
//             prop.appendChild(em4);
//             Element em5 = document.createElement("propValue");
//             em5.appendChild(document.createTextNode(item.isRunnable()+""));
//             prop.appendChild(em5);
//             int nrprop = item.getSubItemsNr();
//             int k;
// //             if(skip)k=1;
// //             else 
//             k=0;
//             for(;k<nrprop;k++)addTempSubElement(tc,item.getSubItem(k));}
//         else{int nrtc = item.getSubItemsNr();
// //             boolean go = false;
// //             if(skip){
// //                 for(int j=0;j<nrtc;j++){
// //                     if(getRunning(item.getSubItem(j))){
// //                         go=true;
// //                         break;}}}
// //             if(!go&&skip)return;
//             Element rootElement2 = document.createElement("TestSuite");
//             rootelement.appendChild(rootElement2);
//             Element em2 = document.createElement("tsName");
//             em2.appendChild(document.createTextNode(item.getName()));
//             rootElement2.appendChild(em2);
//             if(item.getEpId()!=null&&!item.getEpId().equals("")){
//                 Element EP = document.createElement("EpId");
//                 EP.appendChild(document.createTextNode(item.getEpId()));
//                 rootElement2.appendChild(EP);}
//             for(int i=0;i<item.getSubItemsNr();i++){
//                 addTempSubElement(rootElement2,item.getSubItem(i));}}}
                
    public void addSubElement(Element rootelement, Item item, boolean skip, boolean temp){
        if(item.getType()==0){
            Element prop = document.createElement("Property");
            rootelement.appendChild(prop);
            Element em4 = document.createElement("propName");
            em4.appendChild(document.createTextNode(item.getName()));
            prop.appendChild(em4);
            Element em5 = document.createElement("propValue");
            em5.appendChild(document.createTextNode(item.getValue()));
            prop.appendChild(em5);}
        else if(item.getType()==1){
            if(!temp && item.getSubItem(0).getValue().equals("false") && skip)return;
            Element tc  = document.createElement("TestCase");
            rootelement.appendChild(tc);
            Element em3 = document.createElement("tcName");
            if(temp){
                em3.appendChild(document.createTextNode(item.getFileLocation()));
            }
            else{
                if(item.isClearcase()){
                    em3.appendChild(document.createTextNode(item.getFileLocation()));
                } else {
                    em3.appendChild(document.createTextNode(RunnerRepository.getTestSuitePath()+
                                        item.getFileLocation()));
                }
                
            }
            
            tc.appendChild(em3);
            if(item.isClearcase()){
                em3 = document.createElement("ClearCase");
                em3.appendChild(document.createTextNode("true"));
                tc.appendChild(em3);
            }
            
            em3 = document.createElement("ConfigFiles");
            StringBuilder sb = new StringBuilder();
            for(String s:item.getConfigurations()){
                sb.append(s);
                sb.append(";");
            }
            if(sb.length()>0)sb.setLength(sb.length()-1);
            em3.appendChild(document.createTextNode(sb.toString()));
            tc.appendChild(em3);
            
            if(temp || skip){
//                 Element em6 = document.createElement("tcID");
//                 em6.appendChild(document.createTextNode(id+""));
//                 id++;
//                 tc.appendChild(em6);
                Element em7 = document.createElement("Title");
                em7.appendChild(document.createTextNode(""));
                tc.appendChild(em7);
                Element em8 = document.createElement("Summary");
                em8.appendChild(document.createTextNode(""));
                tc.appendChild(em8);
                Element em9 = document.createElement("Priority");
                em9.appendChild(document.createTextNode("Medium"));
                tc.appendChild(em9);
                Element em10 = document.createElement("Dependancy");
                em10.appendChild(document.createTextNode(" "));
                tc.appendChild(em10);
            }
            if(item.isPrerequisite()){
                Element prop  = document.createElement("Property");
                tc.appendChild(prop);
                Element em4 = document.createElement("propName");
                em4.appendChild(document.createTextNode("setup_file"));
                prop.appendChild(em4);
                Element em5 = document.createElement("propValue");
                em5.appendChild(document.createTextNode("true"));
                prop.appendChild(em5);}
            if(item.isTeardown()){
                Element prop  = document.createElement("Property");
                tc.appendChild(prop);
                Element em4 = document.createElement("propName");
                em4.appendChild(document.createTextNode("teardown_file"));
                prop.appendChild(em4);
                Element em5 = document.createElement("propValue");
                em5.appendChild(document.createTextNode("true"));
                prop.appendChild(em5);}
            if(item.isOptional()){
                Element prop  = document.createElement("Property");
                tc.appendChild(prop);
                Element em4 = document.createElement("propName");
                em4.appendChild(document.createTextNode("Optional"));
                prop.appendChild(em4);
                Element em5 = document.createElement("propValue");
                em5.appendChild(document.createTextNode("true"));
                prop.appendChild(em5);}
            Element prop  = document.createElement("Property");
            tc.appendChild(prop);
            Element em4 = document.createElement("propName");
            em4.appendChild(document.createTextNode("Runnable"));
            prop.appendChild(em4);
            Element em5 = document.createElement("propValue");
            em5.appendChild(document.createTextNode(item.isRunnable()+""));
            prop.appendChild(em5);
            int nrprop = item.getSubItemsNr();
            int k=0;
            if(!temp && skip)k=1;
            for(;k<nrprop;k++)addSubElement(tc,item.getSubItem(k),skip,temp);}
        else{int nrtc = item.getSubItemsNr();
            boolean go = false;
            if(!temp && skip){
                for(int j=0;j<nrtc;j++){
                    if(getRunning(item.getSubItem(j))){
                        go=true;
                        break;}}}
            if(!go&&skip&&!temp)return;
            Element rootElement2 = document.createElement("TestSuite");
            rootelement.appendChild(rootElement2);
            Element em2 = document.createElement("tsName");
            em2.appendChild(document.createTextNode(item.getName()));
            rootElement2.appendChild(em2);
            if(item.getEpId()!=null&&!item.getEpId().equals("")){
                
                if(skip){
                    
                    
                    Element EP = document.createElement("EpId");
                    
                    EP.appendChild(document.createTextNode(item.getEpId()[0]));
                    rootElement2.appendChild(EP);
                    
                    EP = document.createElement("SutName");
                    EP.appendChild(document.createTextNode(item.getEpId()[1]));
                    rootElement2.appendChild(EP);
                    
                    
                    
                    
                } else {

                    Element EP = document.createElement("SutName");
                    StringBuilder b = new StringBuilder();
                    for(String s:item.getEpId()){
                        b.append(s+";");
                    }
                    b.deleteCharAt(b.length()-1);                   
                    EP.appendChild(document.createTextNode(b.toString()));
                    rootElement2.appendChild(EP);
                    
                    EP = document.createElement("EpId");
                    Node parent = RunnerRepository.window.mainpanel.p4.getTB().getParentNode();
                    b.setLength(0);
                    DefaultMutableTreeNode noderoot = RunnerRepository.window.mainpanel.p4.getSut().sut.root;
                    int sutsnr = noderoot.getChildCount();
                    
                    for(String s:item.getEpId()){
                        
//                         Iterator iter = parent.getChildren().keySet().iterator();
//                         while(iter.hasNext()){
//                             Node child = parent.getChild(iter.next().toString());
                            
                         for(int j=0;j<sutsnr;j++){
                            SUT child = (SUT)((DefaultMutableTreeNode)noderoot.getChildAt(j)).getUserObject();
                            
                            
                            
                            if(child!=null&&child.getName().equals(s)){
                                for(String ep:child.getEPs().split(";")){
                                    b.append(ep);
                                    b.append(";");
//                                     Item item = current.clone();
//                                     String []str = {ep,child.getName()};
//                                     item.setEpId(str);
//                                     temporary.add(item);
                                    
                        //                                 sb.append(ep);
                        //                                 sb.append(";"); 
                                }
                                b.deleteCharAt(b.length()-1);
                            }
                        }
                    }
                    EP.appendChild(document.createTextNode(b.toString()));
                    rootElement2.appendChild(EP);
                    
                    
                }
                
                
                
                
                
            
                //temporary solution for CE
                if(skip){
                    Item parent = suite.get(item.getPos().get(0));            
                    for(int j=0;j<parent.getUserDefNr();j++){
                        Element userdef = document.createElement("UserDefined");
                        Element pname = document.createElement("propName");
                        pname.appendChild(document.createTextNode(parent.getUserDef(j)[0]));
                        userdef.appendChild(pname);
                        Element pvalue = document.createElement("propValue");
                        pvalue.appendChild(document.createTextNode(parent.getUserDef(j)[1]));
                        userdef.appendChild(pvalue);
                        rootElement2.appendChild(userdef);}
                }
                //end solution for CE
            }
            for(int i=0;i<item.getSubItemsNr();i++){
                addSubElement(rootElement2,item.getSubItem(i),skip,temp);
                
            }}}
                    
    public void printXML(){        
        StreamResult result =  new StreamResult(System.out);
        try{transformer.transform(source, result);}
        catch(Exception e){System.out.println("Could not write standard output stream");}}
        
//     public boolean writeTempXMLFile(String filename, boolean local){
//         File file = new File(filename);
//         Result result = new StreamResult(file);
//         try{transformer.transform(source, result);}
//         catch(Exception e){
//             e.printStackTrace();
//             System.out.println("Could not write to file");
//             return false;}
//         if(!local){
//             try{String dir = RunnerRepository.getXMLRemoteDir();
//                 String [] path = dir.split("/");
//                 StringBuffer result2 = new StringBuffer();
//                 if (path.length > 0){
//                     for (int i=0; i<path.length-1; i++){
//                         result2.append(path[i]);
//                         result2.append("/");}}
//                 RunnerRepository.c.cd(result2.toString());
//                 FileInputStream in = new FileInputStream(file);
//                 RunnerRepository.c.put(in, file.getName());
//                 in.close();}
//             catch(Exception e){e.printStackTrace();
//                 System.out.println("Could not get XML file to upload on sever");
//                 return false;}}
//         return true;}
        
        
    public boolean writeXMLFile(String filename, boolean local, boolean temp, boolean lib){
        File file = new File(filename);
        if(temp)file = new File(RunnerRepository.temp +RunnerRepository.getBar()+"Twister"+RunnerRepository.getBar()+ filename);
        Result result = new StreamResult(file);
        try{transformer.transform(source, result);}
        catch(Exception e){
            e.printStackTrace();
            System.out.println("Could not write to file");
            return false;}
        if(!local){
            try{
                if(temp || skip){
                    String dir = RunnerRepository.getXMLRemoteDir();
                    String [] path = dir.split("/");
                    StringBuffer result2 = new StringBuffer();
                    if (path.length > 0){
                        for (int i=0; i<path.length-1; i++){
                            result2.append(path[i]);
                            result2.append("/");}}
                    FileInputStream in = new FileInputStream(file);
                    return RunnerRepository.uploadRemoteFile(result2.toString(), in, file.getName());
                }
                else{
                    if(lib){
                        FileInputStream in = new FileInputStream(file);
                        return RunnerRepository.uploadRemoteFile(RunnerRepository.getPredefinedSuitesPath(), in, file.getName());
                    } else {
                        FileInputStream in = new FileInputStream(file);
                        return RunnerRepository.uploadRemoteFile(RunnerRepository.getRemoteUsersDirectory(), in, file.getName());
                    }
                }}
            catch(Exception e){e.printStackTrace();
                System.out.println("Could not get XML file to upload on sever");
                return false;}}
        return true;}}
