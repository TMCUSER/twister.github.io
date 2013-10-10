/*
File: ScrollGraficTest.java ; This file is part of Twister.
Version: 2.002

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
import java.awt.ScrollPane;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.awt.Dimension;

public class ScrollGraficTest extends JPanel{ 
    private static final long serialVersionUID = 1L;
    JScrollPane pane;
    GraficTest g;
    
    public ScrollGraficTest(int x, int y,boolean applet){
        g = new GraficTest(0,0,applet);
        pane = new JScrollPane(g);
//         pane.setMinimumSize(new Dimension(10,10));
//         pane.setPreferredSize(new Dimension(100,100));
//         pane.setMaximumSize(new Dimension(1000,1000));
        pane.getVerticalScrollBar().setUnitIncrement(16);
//         add(pane);
    }}
