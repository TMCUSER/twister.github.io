/*
File: XMLReader.java ; This file is part of Twister.
Version: 2.011

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
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Font;
import java.util.ArrayList;

public class XMLReader{
    private DocumentBuilderFactory dbf;
    private DocumentBuilder db;
    private Document doc;
    private Node fstNode,secNode,trdNode;
    private Element fstElmnt,fstNmElmnt,secElmnt,
                    secNmElmnt,trdElmnt,trdNmElmnt;
    private NodeList fstNmElmntLst,fstNm,fstNmElmntLst2,
                     secNmElmntLst,secNm,secNmElmntLst2,
                     trdNmElmntLst,trdNm,trdNmElmntLst2,trdNm2;
    private File f;
    private String name,value;
    private int index = 1001;
    
    public XMLReader (File file){
        f = file;
        dbf = DocumentBuilderFactory.newInstance();
        try{db = dbf.newDocumentBuilder();}
        catch(ParserConfigurationException e){
            System.out.println("Could not create a XML parser configuration");}
        try{doc = db.parse(file);}
        catch(SAXException e){
            try{System.out.println("The document"+file.getCanonicalPath()+
                                    " is empty or not valid");}
            catch(Exception ex){e.printStackTrace();}}
        catch(IOException e){
            try{System.out.println("Could not read the document"+
                                    file.getCanonicalPath());}
            catch(Exception ex){e.printStackTrace();}}
        try{doc.getDocumentElement().normalize();}
        catch(Exception e){e.printStackTrace();}}
        
    public void manageSubChilderen(Item item,Node node,ArrayList <Integer> indexes,
                                    Graphics g, boolean test){
        if(!node.getNodeName().equals("Property")){
            Item theone;
            int k = 0;
            if(node.getNodeName().equals("TestSuite")){
                fstNmElmntLst = ((Element)node).getElementsByTagName("tsName");
                fstNmElmnt = (Element)fstNmElmntLst.item(0);
                fstNm = fstNmElmnt.getChildNodes();
                FontMetrics metrics = g.getFontMetrics(new Font("TimesRoman", 1, 13));
                int width = metrics.stringWidth(fstNm.item(0).getNodeValue().toString());
                if(test){
                    theone = new Item(fstNm.item(0).getNodeValue(),2,
                                        -1,10, width+120,25,indexes);}
                else{
                    theone = new Item(fstNm.item(0).getNodeValue(),2,
                                        -1,10, width+50,25,indexes);}
                if(test){
                    String []text={""};
                    try{                          
                        fstNmElmntLst = ((Element)node).getElementsByTagName("EpId");
                        fstNmElmnt = (Element)fstNmElmntLst.item(0);
                        fstNm = fstNmElmnt.getChildNodes();
                        text[0] = fstNm.item(0).getNodeValue()+" : ";
                    } catch(Exception e){ e.printStackTrace();}
                    try{                          
                        fstNmElmntLst = ((Element)node).getElementsByTagName("SutName");
                        fstNmElmnt = (Element)fstNmElmntLst.item(0);
                        fstNm = fstNmElmnt.getChildNodes();
                        text[0] += fstNm.item(0).getNodeValue();
                    } catch(Exception e){ e.printStackTrace();}
                    theone.setEpId(text);
                } else{
                        try{
                            fstNmElmntLst = ((Element)node).getElementsByTagName("SutName");
                            fstNmElmnt = (Element)fstNmElmntLst.item(0);
                            fstNm = fstNmElmnt.getChildNodes();
                        } catch(Exception ex){
                            System.out.println("Could not find EpId/SutName tag");
                            ex.printStackTrace();
                        }
                    theone.setEpId(fstNm.item(0).getNodeValue().split(";"));
                    
                    
                }
                
                //temporary solution for CE
                if(test){
                   try{
                       k=6;
                       NodeList list = ((Element)node).getChildNodes();
                       for(int i=0;i<list.getLength();i++){
                           if(list.item(i).getNodeName().equals("UserDefined")){
                               k+=2;
                            }
                        }
                    } catch(Exception e){
                        e.printStackTrace();
                        k=6;
                    } 
                } else {
                    k=6;    
                }
                //temporary solution for CE
                
            }
            else{
                secNmElmntLst = ((Element)node).getElementsByTagName("tcName");
                if(secNmElmntLst.getLength()==0)return;
                secNmElmnt = (Element)secNmElmntLst.item(0);
                secNm = secNmElmnt.getChildNodes();
                FontMetrics metrics = g.getFontMetrics(new Font("TimesRoman", 0, 13));
                NodeList clearcase = ((Element)node).getElementsByTagName("ClearCase");
                String f = "";
                boolean isclearcase = false;
                if(clearcase.getLength()==0){
                    f = secNm.item(0).getNodeValue().toString().
                            split(RunnerRepository.getTestSuitePath())[1];
                    k=2;
                } else {
                    f = secNm.item(0).getNodeValue().toString();
                    isclearcase = true;
                    k=4;
                }
                
                int width = metrics.stringWidth(f) + 8;
                if(test){f = secNm.item(0).getNodeValue().toString();}   
                theone = new Item(f,1,-1,-1,width+40,20,indexes);
                theone.setClearcase(isclearcase);
                theone.setCEindex(index++);
                
                secNmElmntLst = ((Element)node).getElementsByTagName("ConfigFiles");
                if(secNmElmntLst.getLength()>0){
                    secNmElmnt = (Element)secNmElmntLst.item(0);
                    secNm = secNmElmnt.getChildNodes();
                    String configs[] ={};
                    if(secNm.getLength()>0){
                        configs= secNm.item(0).getNodeValue().toString().split(";");
                    }
                    theone.setConfigurations(configs);
                    k+=2;
                }
                if(test){
                    ArrayList <Integer> indexpos3 = (ArrayList <Integer>)indexes.clone();
                    indexpos3.add(new Integer(0));
                    name = "Status";
                    value = "Pending";
                    metrics = g.getFontMetrics(new Font("TimesRoman", 0, 11));
                    width = metrics.stringWidth(name+":  "+value) + 8;
                    Item property = new Item(name,0,-1,-1,width+20,20,indexpos3);
                    property.setSubItemVisible(true);
                    property.setValue(value);
                    theone.addSubItem(property);}
                else{
                    ArrayList <Integer> indexpos3 = (ArrayList <Integer>)indexes.clone();
                    indexpos3.add(new Integer(0));
                    name = "Running";
                    value = "true";
                    metrics = g.getFontMetrics(new Font("TimesRoman", 0, 11));
                    width = metrics.stringWidth(name+":  "+value) + 8;
                    Item property = new Item(name,0,-1,-1,width+20,20,indexpos3);
                    property.setSubItemVisible(false);
                    property.setValue(value);
                    theone.addSubItem(property);}
//                 k=4;
            }
//            if(!(test&&theone.getType()==1)){//if it is test the props should not be read further
                int subchildren = node.getChildNodes().getLength();
                int index=0;
                for(;k<subchildren-1;k++){
                    ArrayList <Integer> temp = (ArrayList <Integer>)indexes.clone();
                    temp.add(new Integer(index));
                    k++;
                    manageSubChilderen(theone,node.getChildNodes().item(k),temp,g,test);
                    index++;}
//                 }
            item.addSubItem(theone);}
        else{
            trdNmElmntLst = ((Element)node).getElementsByTagName("propName");
            trdNmElmnt = (Element)trdNmElmntLst.item(0);
            trdNm = trdNmElmnt.getChildNodes();
            name = (trdNm.item(0).getNodeValue().toString());
            if(name.equals("Runnable")){
                trdNmElmntLst2 = ((Element)node).getElementsByTagName("propValue");
                Element trdNmElmnt2 = (Element)trdNmElmntLst2.item(0);
                trdNm2 = trdNmElmnt2.getChildNodes();
                value = (trdNm2.item(0).getNodeValue().toString());
                item.setRunnable(Boolean.parseBoolean(value));
                return;}
            else if(name.equals("teardown_file")){
                item.setTeardown(true);
                return;}
            else if(name.equals("setup_file")){
                item.setPrerequisite(true);
                return;}
            else if(name.equals("Optional")){
                item.setOptional(true);
                return;}
            trdNmElmntLst2 = ((Element)node).getElementsByTagName("propValue");
            Element trdNmElmnt2 = (Element)trdNmElmntLst2.item(0);
            trdNm2 = trdNmElmnt2.getChildNodes();
            value = trdNm2.item(0).getNodeValue().toString();
            if(name.equals("Running")){
                item.setCheck(Boolean.parseBoolean(value));
                return;
            }
            FontMetrics metrics = g.getFontMetrics(new Font("TimesRoman", 0, 11));
            int width = metrics.stringWidth(name+":  "+value) + 8;
            indexes.set(indexes.size()-1,
                        new Integer(indexes.get(indexes.size()-1).intValue()-1));
            int index = item.getSubItemsNr();
            indexes.set(indexes.size()-1, index);
            Item property = new Item(name,0,-1,-1,width+30,20,indexes);
            property.setSubItemVisible(false);
            property.setValue(value);
            item.addSubItem(property);
            item.setVisible(false);
        }
    }
    
    /*
     * clear - if the parser populates a custom array it should be false
     * clear is true when opening a new file in Repositor array
     */    
    public void parseXML(Graphics g,boolean test,ArrayList <Item> suite, boolean clear){
        if(!test&&clear){
            RunnerRepository.window.mainpanel.p1.suitaDetails.setGlobalLibs(null);
            RunnerRepository.window.mainpanel.p1.suitaDetails.setSaveDB(false);
            RunnerRepository.window.mainpanel.p1.suitaDetails.setDelay("");
            RunnerRepository.window.mainpanel.p1.suitaDetails.setStopOnFail(false);
            RunnerRepository.window.mainpanel.p1.suitaDetails.setPreStopOnFail(false);
            RunnerRepository.window.mainpanel.p1.suitaDetails.setPostScript("");
            RunnerRepository.window.mainpanel.p1.suitaDetails.setPreScript("");
        }
        NodeList nodeLst = doc.getChildNodes().item(0).getChildNodes();
        int childsnr = doc.getChildNodes().item(0).getChildNodes().getLength();
        if(childsnr==0){
            try{System.out.println(f.getCanonicalPath()+" has no content");}
            catch(Exception e){e.printStackTrace();}}
        int indexsuita = 0;
        for(int m=0;m<childsnr;m++){
            Node fstNode = nodeLst.item(m);
            if(!test&&clear){
                if(fstNode.getNodeName().equals("stoponfail")){
                    if(fstNode.getChildNodes().item(0).getNodeValue().toString().equals("true")){                    
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setStopOnFail(true);
                    }
                    else RunnerRepository.window.mainpanel.p1.suitaDetails.setStopOnFail(false);
                    continue;
                }
                if(fstNode.getNodeName().equals("PrePostMandatory")){
                    if(fstNode.getChildNodes().item(0).getNodeValue().toString().equals("true")){                    
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setPreStopOnFail(true);
                    }
                    else RunnerRepository.window.mainpanel.p1.suitaDetails.setPreStopOnFail(false);
                    continue;
                }
                else if(fstNode.getNodeName().equals("tcdelay")){
                    try{
                        String delay = fstNode.getChildNodes().item(0).getNodeValue().toString();
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setDelay(delay);}
                    catch(Exception e){
                        e.printStackTrace();
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setDelay("");
                    }
                    continue;
                }
                else if(fstNode.getNodeName().equals("dbautosave")){
                    if(fstNode.getChildNodes().item(0).getNodeValue().toString().equals("true")){                    
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setSaveDB(true);
                    }
                    else RunnerRepository.window.mainpanel.p1.suitaDetails.setSaveDB(false);
                    continue;
                }
                else if(fstNode.getNodeName().equals("ScriptPre")){
                    try{
                        String script = fstNode.getChildNodes().item(0).getNodeValue().toString();
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setPreScript(script);}
                    catch(Exception e){
                        e.printStackTrace();
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setPreScript("");
                    }
                    continue;
                }
                else if(fstNode.getNodeName().equals("ClearCaseView")){
                    try{
                        String view = fstNode.getChildNodes().item(0).getNodeValue().toString();
                        RunnerRepository.window.mainpanel.getP5().view = view;}
                    catch(Exception e){
                        RunnerRepository.window.mainpanel.getP5().view = "";
//                         e.printStackTrace();
//                         RunnerRepository.window.mainpanel.p1.suitaDetails.setPreScript("");
                    }
                    continue;
                }
                else if(fstNode.getNodeName().equals("ScriptPost")){
                    try{
                        String script = fstNode.getChildNodes().item(0).getNodeValue().toString();
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setPostScript(script);}
                    catch(Exception e){
                        e.printStackTrace();
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setPostScript("");
                    }
                    continue;
                }
                else if(fstNode.getNodeName().equals("libraries")){
                    try{
                        String [] libraries = fstNode.getChildNodes().item(0).getNodeValue().toString().split(";");
                        RunnerRepository.window.mainpanel.p1.suitaDetails.setGlobalLibs(libraries);}
                    catch(Exception e){
                        e.printStackTrace();
                    }
                    continue;
                }
            }
            if(!fstNode.getNodeName().equals("TestSuite"))continue;            
            ArrayList <Integer> indexpos = new ArrayList <Integer> ();
            indexpos.add(new Integer(indexsuita));            
            fstElmnt = (Element)fstNode;
            fstNmElmntLst = fstElmnt.getElementsByTagName("tsName");
            fstNmElmnt = (Element)fstNmElmntLst.item(0);
            fstNm = fstNmElmnt.getChildNodes();
            FontMetrics metrics = g.getFontMetrics(new Font("TimesRoman", 1, 13));
            int width = metrics.stringWidth(fstNm.item(0).getNodeValue().toString());
            Item suitatemp;
            if(!test)suitatemp= new Item(fstNm.item(0).getNodeValue(),
                                         2,-1,10, width+50,25,indexpos);
            else suitatemp=  new Item(fstNm.item(0).getNodeValue(),
                                      2,-1,10, width+120,25,indexpos);
            int k=6;
                                      
            fstNmElmntLst = fstElmnt.getElementsByTagName("libraries");
            if(fstNmElmntLst.getLength()>0){
                fstNmElmnt = (Element)fstNmElmntLst.item(0);
                fstNm = fstNmElmnt.getChildNodes();
                suitatemp.setLibs(fstNm.item(0).getNodeValue().split(";"));
                k+=2;
            }
            
            try{fstNmElmntLst = fstElmnt.getElementsByTagName("PanicDetect");
                if(fstNmElmntLst.getLength()>0){
                    fstNmElmnt = (Element)fstNmElmntLst.item(0);
                    fstNm = fstNmElmnt.getChildNodes();
                    suitatemp.setPanicdetect(Boolean.parseBoolean(fstNm.item(0).getNodeValue()));
                    k+=2;
                }
            } catch(Exception e){
                e.printStackTrace();
            }
            if(test){
                String []text={""};
                try{                          
                    fstNmElmntLst = fstElmnt.getElementsByTagName("EpId");
                    fstNmElmnt = (Element)fstNmElmntLst.item(0);
                    fstNm = fstNmElmnt.getChildNodes();
                    text[0] = fstNm.item(0).getNodeValue()+" : ";
                } catch(Exception e){ e.printStackTrace();}
                try{                          
                    fstNmElmntLst = fstElmnt.getElementsByTagName("SutName");
                    fstNmElmnt = (Element)fstNmElmntLst.item(0);
                    fstNm = fstNmElmnt.getChildNodes();
                    text[0] += fstNm.item(0).getNodeValue();
                } catch(Exception e){ e.printStackTrace();}
                suitatemp.setEpId(text);
                
            } else{
//                 try{                          
//                     fstNmElmntLst = fstElmnt.getElementsByTagName("EpId");
//                     fstNmElmnt = (Element)fstNmElmntLst.item(0);
//                     fstNm = fstNmElmnt.getChildNodes();
//                 } catch(Exception e){
                    try{fstNmElmntLst = fstElmnt.getElementsByTagName("SutName");
                        fstNmElmnt = (Element)fstNmElmntLst.item(0);
                        fstNm = fstNmElmnt.getChildNodes();
                    } catch (Exception ex){
                        System.out.println("There is an element that has no EpId or SutName!!!");
                        ex.printStackTrace();
                    }
//                 }
                
    //             suitatemp.setEpId(fstNm.item(0).getNodeValue());
                try{suitatemp.setEpId(fstNm.item(0).getNodeValue().split(";"));}
                catch(Exception e){
                    String [] s = {fstNm.item(0).getNodeValue()};
                    suitatemp.setEpId(s);}
            }
            
            
            
            
            
            //temp solution for CE
            int items = fstElmnt.getChildNodes().getLength();
            int userdefinitions = 0;
            for(int i=0;i<items;i++){
                if(fstElmnt.getChildNodes().item(i).getNodeName().equals("UserDefined"))userdefinitions++;
            }
            for(int i=0;i<items;i++){
                if(fstElmnt.getChildNodes().item(i).getNodeName().equals("UserDefined")){
                    Element element = (Element)fstElmnt.getChildNodes().item(i);                
                    NodeList propname = element.getElementsByTagName("propName");
                    Element el1 = (Element)propname.item(0);
                    fstNm = el1.getChildNodes();
                    String prop ;
                    if(fstNm.getLength()>0)prop= fstNm.item(0).getNodeValue();
                    else prop = "";
                    NodeList propvalue = element.getElementsByTagName("propValue");
                    Element el2 = (Element)propvalue.item(0);
                    fstNm = el2.getChildNodes();
                    String val ;
                    if(fstNm.getLength()>0)val = fstNm.item(0).getNodeValue();
                    else val = "";
                    suitatemp.addUserDef(new String[]{prop,val});
                }
            }
            //temp solution for CE
            
            
            
//             fstNmElmntLst = fstElmnt.getElementsByTagName("UserDefined");
//             int userdefinitions = fstNmElmntLst.getLength();            
//             for(int l=0;l<userdefinitions;l++){
//                 Element element = (Element)fstNmElmntLst.item(l);                
//                 NodeList propname = element.getElementsByTagName("propName");
//                 Element el1 = (Element)propname.item(0);
//                 fstNm = el1.getChildNodes();
//                 String prop ;
//                 if(fstNm.getLength()>0)prop= fstNm.item(0).getNodeValue();
//                 else prop = "";
//                 NodeList propvalue = element.getElementsByTagName("propValue");
//                 Element el2 = (Element)propvalue.item(0);
//                 fstNm = el2.getChildNodes();
//                 String val ;
//                 if(fstNm.getLength()>0)val = fstNm.item(0).getNodeValue();
//                 else val = "";
//                 suitatemp.addUserDef(new String[]{prop,val});}
                
                
                
            int subchildren = fstElmnt.getChildNodes().getLength();
            int index=0;
            indexsuita++;
            //if(test)k+=2;
            for( k+=(userdefinitions*2);k<subchildren-1;k++){
                k++;
                ArrayList <Integer> temp =(ArrayList <Integer>)indexpos.clone();
                temp.add(new Integer(index));
                manageSubChilderen(suitatemp,fstElmnt.getChildNodes().item(k),
                                    temp,g,test);
                index++;}
//             if(!test)RunnerRepository.addSuita(suitatemp);
            if(!test)suite.add(suitatemp);
            
            else{
//                 RunnerRepository.addTestSuita(suitatemp);
                suite.add(suitatemp);
                
                String currents = suitatemp.getEpId()[0].split(" : ")[0];
//                 for(String currents:suiteeps){
                    boolean found = false;
                    for(String s:RunnerRepository.getLogs()){
                        if(s.equals(currents+"_"+RunnerRepository.getLogs().get(4))){                        
                            found = true;
                            break;
                        }                    
                    }
                    if(!found){
                            RunnerRepository.getLogs().add(currents+"_"+
                                                     RunnerRepository.getLogs().get(4));
                    }
//                 }
            
            
            
            
//             for(String s:RunnerRepository.getLogs()){
//                 String [] suiteeps = suitatemp.getEpId();
//                 for(String currents:suiteeps){
//                     if(s.equals(suitatemp.getEpId()+"_"+RunnerRepository.getLogs().get(4))){                        
//                         found = true;
//                         break;
//                     }
//                 }
//                 if(found)break;                
//             }
//             if(!found){
//                 RunnerRepository.getLogs().add(suitatemp.getEpId()+"_"+
//                                             RunnerRepository.getLogs().get(4));
//                                         }
                                    
                                    
                                    }}
        if(!test){
            if(RunnerRepository.getSuiteNr()>0){
                while(RunnerRepository.window.mainpanel.p1.sc.g==null){
                    try{Thread.sleep(10);}
                    catch(Exception e){e.printStackTrace();}}
                RunnerRepository.window.mainpanel.p1.sc.g.updateLocations(RunnerRepository.getSuita(0));
                RunnerRepository.window.mainpanel.p1.sc.g.repaint();}}
        else{if(RunnerRepository.getTestSuiteNr()>0){
                while(RunnerRepository.window==null||RunnerRepository.window.mainpanel==null||
                RunnerRepository.window.mainpanel.getP2()==null||RunnerRepository.window.mainpanel.getP2().sc==null||
                RunnerRepository.window.mainpanel.getP2().sc.g==null){
                    try{Thread.sleep(10);}
                    catch(Exception e){e.printStackTrace();}}
                RunnerRepository.window.mainpanel.getP2().sc.g.updateLocations(RunnerRepository.getTestSuita(0));
                RunnerRepository.window.mainpanel.getP2().sc.g.repaint();}}}}
