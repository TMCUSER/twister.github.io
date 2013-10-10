#!/usr/bin/env python
#
# -*- coding: utf-8 -*-
#
# version: 2.002
#
# File: PacketSnifferPlugin.py ; This file is part of Twister.
#
# Copyright (C) 2012 , Luxoft
#
#
# Authors:
#    Adrian Toader <adtoader@luxoft.com>
#
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


"""
    PacketSniffer plugin manage the sniffed packets from client
    and makes them usable for the interface.
"""


from os import getenv, makedirs
from os.path import exists

from binascii import a2b_base64
from ast import literal_eval

from copy import deepcopy
from time import time

from json import dumps
from scapy.all import wrpcap, Ether

from BasePlugin import BasePlugin




class Plugin(BasePlugin):
    """
    Packet Sniffer plugin.
    """

    def __init__(self, user, data):
        if not data:
            return False

        BasePlugin.__init__(self, user, data)

        # history list length, packets buffer size, query buffer size
        if data.has_key('historyLength'):
            self.data['historyLength'] = (40000,
                int(self.data['historyLength']))[int(self.data['historyLength'])>0]
        else:
            self.data.update([('historyLength', 40000), ])
        if data.has_key('packetsBuffer'):
            self.data['packetsBuffer'] = (400,
                int(self.data['packetsBuffer']))[int(self.data['packetsBuffer'])>0]
        else:
            self.data.update([('packetsBuffer', 400), ])

        # plugin status, packets list, temporary path to save pcap files,
        # packets list index limit, registered sniffers
        self.status = 'paused'
        self.packets = []
        self.pcapPath = getenv('TWISTER_PATH') + '/tmp'
        if not exists(self.pcapPath):
            makedirs(self.pcapPath)
        self.packetsIndexLimit = (self.data['historyLength']
                                    - self.data['packetsBuffer'])
        self.filters = {}
        self.sniffers = {}

        self.commands = {
            'simple': [
                'echo',
                'pause', 'resume',
                'restart', 'reset',
                'savepcap',
                'getfilters',
                'setfilters',
            ],
            'argumented': [
                'query', 'querypkt', 'pushpkt',
                'registersniff', 'restarted',
            ]
        }


    def run(self, args):
        args = {k: v[0] if isinstance(v, list) else v for k,v in args.iteritems()}

        # response structure
        response = {
            'status': {
                'success': True,
                'message': 'None', # error message
            },
            'type': 'reply', # reply type
            'state': self.status, # state of plugin
            'data': 'None', # response data
        }

        if (not args.has_key('command') or args['command'] not in
                self.commands['simple'] + self.commands['argumented']):

            response['status']['success'] = False
            response['status']['message'] = 'unknown command'

        elif (args['command'] in self.commands['argumented']
                and not args.has_key('data')):
            response['status']['success'] = False
            response['status']['message'] = 'no command data specified'

        # echo
        elif args['command'] == 'echo':
            response['type'] = 'echo reply'

            if args.has_key('data'):
                try:
                    response['state'] = self.sniffers[args['data']]
                    response['data'] = {'filters': self.filters}
                except Exception, e:
                    response['status']['success'] = False
                    response['status']['message'] = 'command data not valid: \
                                                        {err}'.format(err=e)
            else:
                response = self.status

        # registersniff
        elif args['command'] == 'registersniff':
            response['type'] = 'register sniff reply'

            try:
                self.sniffers.update([(args['data'], self.status), ])
            except Exception, e:
                response['status']['success'] = False
                response['status']['message'] = 'error: {err}'.format(err=e)

        # get / set filters
        elif args['command'] == 'getfilters':
            response['type'] = 'getfilters reply'

            response = self.filters
        elif args['command'] == 'setfilters':
            response['type'] = 'setfilters reply'

            try:
                if args.has_key('data'):
                    data = args['data'].split()
                    data = {data[i]: data[i+1] for i in range(0, len(data)-1, 2)}

                    self.filters = dict((k,str(v)) for k,v in data.iteritems() if v is not None)
                else:
                    self.filters = {}

                self.packets = []
                response['data'] = {'index': 0}

            except Exception, e:
                response['status']['success'] = False
                response['status']['message'] = 'error: {err}'.format(err=e)

        # pause / resume
        elif args['command'] in ['pause', 'resume']:
            response['type'] = 'pause/resume reply'

            self.status = ('paused', 'running')[args['command']=='resume']

            response['state'] = self.status

            if self.status == 'paused':
                for key in self.sniffers.iterkeys():
                    self.sniffers[key] = 'paused'
            else:
                for key in self.sniffers.iterkeys():
                    self.sniffers[key] = 'running'

        # restart
        elif args['command'] == 'restart':
            response['type'] = 'restart reply'

            for key in self.sniffers.iterkeys():
                    self.sniffers[key] = 'restart'

        # restarted
        elif args['command'] == 'restarted':
            response['type'] = 'restarted reply'

            try:
                self.sniffers[args['data']] = 'running'
            except Exception, e:
                response['status']['success'] = False
                response['status']['message'] = 'command data not valid: \
                                                    {err}'.format(err=e)

        # reset
        elif args['command'] == 'reset':
            response['type'] = 'reset reply'

            self.packets = []

            response['data'] = {'index': 0}

        # # query / querypkt
        # """ query for packets command (you must supply data field
        # in argument dictionary which represents the index you left from
        # and you will recive a new index on reponse which you will use
        # for the next call of the function)
        # query for packet command (you must supply data field
        # in argument dictionary which represents the id of the packet) """
        elif args['command'] in ['query', 'querypkt']:
            try:
                try:
                    packetIndex = int(args['data'])
                    packet = deepcopy(self.packets[packetIndex])
                except Exception, e:
                    packetIndex = None
                    packet = None

                if not packetIndex == 0:
                    packetID = str(args['data'])
                    for _packet in self.packets:
                        if _packet['packet_head']['id'] == packetID:
                            packetIndex = self.packets.index(_packet) + 1
                            packet = deepcopy(_packet)


                if packetIndex is not None:
                    if args['command'] == 'query':
                        response['type'] = 'query reply'

                        queriedPackets = []
                        packetID = None
                        for _packet in self.packets[packetIndex:packetIndex \
                                                    + self.data['packetsBuffer']]:
                            pk = deepcopy(_packet)
                            pk.pop('packet')
                            packetID = pk['packet_head']['id']
                            queriedPackets.append(dumps(pk, encoding='latin'))

                        response['data'] = {
                            'id': (args['data'], packetID)[packetID is not None],
                            'packets': queriedPackets
                        }

                    else:
                        #response['type'] = 'querypkt reply'

                        packet = dumps(packet['packet'], encoding='latin')
                        response = packet    #response['data'] = packet
                else:
                    response['status']['success'] = False
                    if self.packets:
                        response['status']['message'] = 'packet index unknown'
                    else:
                        response['data'] = dict()
                        response['status']['message'] = 'packets list empty'

                del packet
            except Exception, e:
                response['status']['success'] = False
                response['status']['message'] = 'command data not valid: \
                                                    {err}'.format(err=e)

        # # pushpkt
        # """ push packet command (data is an encoded string representing
        # a dictionary whre you can find the sniffed packet) """
        elif args['command'] == 'pushpkt':
            response['type'] = 'pushpkt reply'

            args['data'] = literal_eval(a2b_base64(args['data']))
            for packet in args['data']:
                try:
                    _packet = literal_eval(a2b_base64(packet))
                    self.packets.append(_packet)
                except Exception, e:
                    response['status']['success'] = False
                    response['status']['message'] = 'command data not valid: \
                                                        {err}'.format(err=e)

            if len(self.packets) >= self.packetsIndexLimit:
                del self.packets[:self.data['packetsBuffer']]

        # savepcap
        elif args['command'] == 'savepcap':
            response['type'] = 'savepcap reply'

            try:
                filePath = '{pcap_path}/{user}[{epoch_time}].pcap'.format(
                                    pcap_path=self.pcapPath,
                                    user=self.user,
                                    epoch_time=str(time()).replace('.', '|'))

                wrpcap(filePath, [Ether(p['packet_str']) for p in self.packets])

                response = filePath
            except Exception, e:
                response['status']['success'] = False
                response['status']['message'] = 'error: {err}'.format(err=e)

        else:
            response['status']['success'] = False
            response['status']['message'] = 'except'

        return response




"""

#### plugins.xml config ####

<Plugin>
    <name>PacketSnifferPlugin</name>
    <jarfile>PacketSnifferPlugin.jar</jarfile>
    <pyfile>PacketSnifferPlugin.py</pyfile>
    <status>enabled</status>
    <property>
        <propname>historyLength</propname>
        <propvalue>4000</propvalue>
    </property>
    <property>
        <propname>packetsBuffer</propname>
        <propvalue>400</propvalue>
    </property>
</Plugin>

"""
