/*
File: TwisterPluginInterface.java ; This file is part of Twister.
Version: 2.001
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
package com.twister.plugin.twisterinterface;

import java.applet.Applet;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Hashtable;
import org.w3c.dom.Document;
import com.twister.Item;

public interface TwisterPluginInterface{
	void init(ArrayList <Item>suite,ArrayList <Item>suitetest,
			  Hashtable<String, String>variables,
			  Document pluginsConfig,Applet container);
	void terminate();
    Component getContent();
    String getName();
    String getDescription(String localplugindir);
    String getFileName();
    void setInterface(CommonInterface commoninterface);
    void resizePlugin(int width, int height);
}
