/*
File: BasePlugin.java ; This file is part of Twister.
Version: 2.003
Copyright (C) 2012 , Luxoft

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
package com.twister.plugin.baseplugin;

import java.applet.Applet;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import javax.swing.JPanel;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.twister.Item;
import com.twister.plugin.twisterinterface.CommonInterface;
import com.twister.plugin.twisterinterface.TwisterPluginInterface;

public class BasePlugin extends JPanel implements TwisterPluginInterface {
	private static final long serialVersionUID = 1L;
	protected ArrayList<Item> suite;// suite list
	protected ArrayList<Item> suitetest;// test suite list generated
	protected Hashtable<String, String> variables;
	protected Document pluginsConfig;
	protected Element rootElement;
	protected ChannelSftp c;
	protected Session session;

	@Override
	public void init(ArrayList<Item> suite, ArrayList<Item> suitetest,
			Hashtable<String, String> variables, Document pluginsConfig,Applet container) {
		this.suite = suite;
		this.suitetest = suitetest;
		this.variables = variables;
		this.pluginsConfig = pluginsConfig;
		initializeSFTP();
		createXMLStructure();
		setEnabledValue(true);
		uploadPluginsFile();
	}

	@Override
	public void terminate() {
		setEnabledValue(false);
		uploadPluginsFile();
		session.disconnect();
		c.disconnect();
		c = null;
		session = null;
		suite = null;
		suitetest = null;
		variables = null;
		pluginsConfig = null;
		rootElement = null;
	}
	
	public void setEnabledValue(boolean value){
		Element item = (Element)rootElement.getElementsByTagName("status").item(0);
		if(value){
			item.getChildNodes().item(0).setNodeValue("enabled");
		} else {
			item.getChildNodes().item(0).setNodeValue("disabled");
		}
	}

	@Override
	public Component getContent() {
		return this;
	}

	@Override
	public String getName() {
		return "Base plugin..";
	}

	@Override
	public String getDescription(String localplugindir) {
		try {
			String descfile = localplugindir
					+ "/"
					+ getFileName().substring(0,
							getFileName().indexOf("."))
					+ "_description.txt";
			BufferedReader br = new BufferedReader(new FileReader(descfile));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
			br.close();
			return sb.toString();
		} catch (Exception e) {
			System.out.println("Could not read description file from plugin ");
			e.printStackTrace();
			return "Could not read description file from plugin ";
		}
	}

	@Override
	public String getFileName() {
		return "FileName.jar";
	}
	
	/*
     * method to copy plugins configuration file
     * to server 
     */
    public boolean uploadPluginsFile(){
        try{
            DOMSource source = new DOMSource(pluginsConfig);
            File file = new File(variables.get("pluginslocalgeneralconf"));
            Result result = new StreamResult(file);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http:xml.apache.org/xslt}indent-amount","4");
            transformer.transform(source, result);
            c.cd(variables.get("remoteuserhome")+"/twister/config/");
            FileInputStream in = new FileInputStream(file);
            c.put(in, file.getName());
            in.close();
            System.out.println("Saved "+file.getName()+" to: "+
					variables.get("remoteuserhome")+"/twister/config/");
            return true;}
        catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    
    public ChannelSftp getSFTP(){
    	return c;
    }
    
    public void initializeSFTP(){
		try{
			JSch jsch = new JSch();
            String user = variables.get("user");
            session = jsch.getSession(user, variables.get("host"), 22);
            session.setPassword(variables.get("password"));
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            Channel channel = session.openChannel("sftp");
            channel.connect();
            c = (ChannelSftp)channel;
            System.out.println("SFTP successfully initialized");
		}
		catch(Exception e){
			System.out.println("SFTP could not be initialized");
			e.printStackTrace();
		}
	}

	/*
	 * method to check and create XML structure for this plugin
	 */
	public void createXMLStructure() {
		Document doc = pluginsConfig;
		NodeList list1 = doc.getElementsByTagName("Plugin");
		Element item;
		Element compare;
		boolean found = false;
		for (int i = 0; i < list1.getLength(); i++) {
			item = (Element) list1.item(i);
			compare = (Element) item.getElementsByTagName("jarfile").item(0);
			if (compare.getChildNodes().item(0).getNodeValue()
					.equals(getFileName())) {
				found = true;
				rootElement = item;
				break;
			}
		}
		if (!found) {
			rootElement = doc.createElement("Plugin");
			doc.getFirstChild().appendChild(rootElement);
			Element em2 = doc.createElement("name");
			em2.appendChild(doc.createTextNode(getName()));
			rootElement.appendChild(em2);
			em2 = doc.createElement("jarfile");
			em2.appendChild(doc.createTextNode(getFileName()));
			rootElement.appendChild(em2);
			em2 = doc.createElement("pyfile");
			String filename = getFileName().split(".jar")[0] + ".py";
			em2.appendChild(doc.createTextNode(filename));
			rootElement.appendChild(em2);
			em2 = doc.createElement("status");
			em2.appendChild(doc.createTextNode("enabled"));
			rootElement.appendChild(em2);
		}
	}

	/*
	 * method to get a Node from general config xml that represents a property
	 * with name=name and coresponds to this plugin. If the method doesent find
	 * the property it creates one
	 */
	public Node getPropValue(String name) {
		NodeList list2 = rootElement.getElementsByTagName("property");
		Element item;
		Element compare;
		Document doc = pluginsConfig;
		for (int j = 0; j < list2.getLength(); j++) {
			item = (Element) list2.item(j);
			compare = (Element) item.getElementsByTagName("propname").item(0);
			if (compare.getChildNodes().item(0).getNodeValue().equals(name)) {
				compare = (Element) item.getElementsByTagName("propvalue")
						.item(0);
				try {
					if (compare.getChildNodes().item(0) == null) {
						compare.appendChild(doc.createTextNode(" "));
						compare.getChildNodes().item(0).setNodeValue("");
					}
					return compare.getChildNodes().item(0);
				} catch (Exception e) {
					compare.appendChild(doc.createTextNode(" "));
					compare.getChildNodes().item(0).setNodeValue("");
					return compare.getChildNodes().item(0);
				}
			}
		}
		Element em2 = doc.createElement("property");

		Element em3 = doc.createElement("propname");
		em3.appendChild(doc.createTextNode(name));
		em2.appendChild(em3);

		Element em4 = doc.createElement("propvalue");
		em4.appendChild(doc.createTextNode(" "));
		em4.getChildNodes().item(0).setNodeValue("");

		em2.appendChild(em4);

		rootElement.appendChild(em2);

		return em4.getChildNodes().item(0);
	}

	@Override
	public void setInterface(CommonInterface commoninterface) {
	}

	@Override
	public void resizePlugin(int width, int height) {
		
	}
}