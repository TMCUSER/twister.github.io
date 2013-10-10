#!/usr/bin/env python

# version: 2.004
#
# -*- coding: utf-8 -*-
#
# File: PacketSniffer.py ; This file is part of Twister.
#
# Copyright (C) 2012-2013 , Luxoft
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


from binascii import b2a_base64

from xmlrpclib import ServerProxy
from uuid import uuid4
from time import sleep
from thread import start_new_thread, allocate_lock
from scapy.all import Automaton, ATMT, TCP, bind_layers, Packet, NoPayload, Raw


from PacketSnifferClasses import OpenFlow, CentralEngineObject
try:
	from openflow.of_13.parse import of_message_parse
except Exception as e:
	of_message_parse = None
	print('WARNING: openflow lib not found')


from sys import maxsize
from socket import gethostname, gethostbyname, socket, AF_INET, SOCK_DGRAM, inet_ntoa, create_connection
from fcntl import ioctl
from struct import unpack, pack
from array import array




packetsLock = allocate_lock()

sniffedPackets = list()
endAll = False
RESTART = False
IFACE = None
ifaceError = False


def all_interfaces():
	is_64bits = maxsize > 2**32
	struct_size = 40 if is_64bits else 32
	sock = socket(AF_INET, SOCK_DGRAM)
	max_possible = 8 # initial value
	while True:
		bytes = max_possible * struct_size
		names = array('B', '\0' * bytes)
		outbytes = unpack('iL', ioctl(
			sock.fileno(),
			0x8912,  # SIOCGIFCONF
			pack('iL', bytes, names.buffer_info()[0])
		))[0]
		if outbytes == bytes:
			max_possible *= 2
		else:
			break
	namestr = names.tostring()
	return [(namestr[i:i+16].split('\0', 1)[0],
			 inet_ntoa(namestr[i+20:i+24]))
			for i in range(0, outbytes, struct_size)]


class PacketSniffer():
	def __init__(self, user, epConfig, OFPort=None, _iface=None, _uid=None):
		self.snifferData = dict(
			user = user,
			epConfig = epConfig,
			OFPort = OFPort,
			_uid = _uid,
		)
		IFACE = _iface

		self.sniffer = Sniffer(self.snifferData['user'],
										self.snifferData['epConfig'],
										self.snifferData['OFPort'],
										IFACE,
										self.snifferData['_uid'])

		self.parser = ParseData(self.sniffer)


	def run(self):
		self.sniffer.runbg()
		while not self.sniffer._started:
			sleep(2)
		start_new_thread(self.parser.run, ())

		global endAll
		global RESTART

		while not endAll:
			if RESTART:
				endAll = True
				self.sniffer.stop()
				del(self.sniffer)

				global sniffedPackets
				sniffedPackets = []

				self.sniffer = Sniffer(self.snifferData['user'],
										self.snifferData['epConfig'],
										self.snifferData['OFPort'],
										IFACE,
										self.snifferData['_uid'])
				self.sniffer.runbg()

				sleep(2)
				del(self.parser)

				endAll = False
				self.parser = ParseData(self.sniffer)
				start_new_thread(self.parser.run, ())

				RESTART = False
			sleep(2)


class Sniffer(Automaton):
	"""
	Packet Sniffer Scapy Automaton
	"""

	def parse_args(self, user, epConfig, OFPort=None, _iface=None, _uid=None):
		Automaton.parse_args(self)

		self.has_iface = None
		if _iface:
			ifaces = all_interfaces()
			for __iface in ifaces:
				if _iface in __iface:
					self.has_iface = True
					self.socket_kargs = {'iface': _iface, }
			if not self.has_iface:
				self.has_iface = False

				print 'PT debug: set iface error: no such device'

		self.PAUSED = True
		self.OFPort = (OFPort, 6633)[OFPort is None]

		# openflow packet model connect
		#bind_layers(TCP, OpenFlow, sport=self.OFPort)
		#bind_layers(TCP, OpenFlow, dport=self.OFPort)

		# packet filters
		self.filters = None

		# user
		self.username = user
		self.userip = gethostbyname(gethostname())
		self.userhost = gethostname()

		self.epConfig = epConfig
		_epConfig = list(set([(ep['CE_IP'], ep['CE_PORT']) for ep in self.epConfig]))
		self.ceObjects = [
			CentralEngineObject(
				ServerProxy('http://{_us}:EP@{ip}:{port}/'.format(_us=self.username, ip=ep[0], port=ep[1])),
			)
			for ep in _epConfig
		]

		self.ceTraffic = [(ep[0], ep[1]) for ep in _epConfig]

		self.uid = (uuid4(), _uid)[_uid is not None]

		self.reinitRetries = 0
		self.reinitMaxRetries = 4
		self._started = False

	def master_filter(self, packet):
		try:
			# default filter: exclude CE traffic
			sourcePort = str(packet.payload.payload.fields['sport'])
			destinationPort = str(packet.payload.payload.fields['dport'])
			sourceIp = packet.payload.fields['src']
			destinationIp = packet.payload.fields['dst']
			if ((sourceIp, sourcePort) in self.ceTraffic
				or (destinationIp, destinationPort) in self.ceTraffic):
				return False
		except Exception, e:
			sourcePort = 'None'
			destinationPort = 'None'

		# only OFP packets
		#if not (sourcePort == '6633' or destinationPort == '6633'):
		#	return False

		return True


	# BEGIN
	@ATMT.state(initial=1)
	def BEGIN(self):
		print '|||| state: BEGIN ||||'

		# Try to ping status from CE!
		for ce in self.ceObjects:
			try:
				_proxy = str(ce.proxy).split()[2][:-2].split(':')[1:]
				_proxy[0] = _proxy[0].split('@')[1]
				create_connection((_proxy[0], _proxy[1]), 2)
				#ce.proxy.echo('ping')
				self.reinitRetries = 0
			except Exception, e:
				self.ceObjects.pop(self.ceObjects.index(ce))

				print 'PT warning: Central Engine is down .... [{0}]'.format(e)

		if not self.ceObjects:
			if self.reinitRetries < self.reinitMaxRetries:
				print 'PT debug: no central engines; \
						will retry [{r}] ..'.format(r=self.reinitRetries)
				self.reinitRetries += 1

				_epConfig = list(set([(ep['CE_IP'], ep['CE_PORT']) for ep in self.epConfig]))
				self.ceObjects = [
					CentralEngineObject(
						ServerProxy('http://{_us}:EP@{ip}:{port}/'.format(_us=self.username, ip=ep[0],
															port=ep[1])),
					)
					for ep in _epConfig
				]
				sleep(2)

				raise self.BEGIN()
			else:
				raise self.END()

		args = {
			'command': 'registersniff',
			'data': str(self.uid)
		}

		try:
			for ce in self.ceObjects:
				# create user if ep is not running
				ce.proxy.getExecStatusAll(self.username)

				pluginData = ce.proxy.runPlugin(self.username,
												'PacketSnifferPlugin', args)

				if pluginData['status']['success']:
					print 'registered to central engine %s..' % ce.proxy
				else:
					print 'running unregistered to central engine %s ..' % ce.proxy
					print pluginData['status']['message']
		except Exception, e:
			print 'PT debug: [BEGIN] {err}'.format(err=e)

		self._started = True

		raise self.WAITING()


	# WAITING
	@ATMT.state()
	def WAITING(self):
		pass


	# RECEIVING
	@ATMT.receive_condition(WAITING)
	def receive_data(self, pkt):
		if self.has_iface is not None and not self.has_iface:
			raise self.WAITING()

		raise (self.RECEIVING(pkt), self.WAITING())[self.PAUSED]


	# RECEIVED
	@ATMT.state()
	def RECEIVING(self, packet):
		global sniffedPackets
		try:
			with packetsLock:
				if packet not in sniffedPackets:
					sniffedPackets.append(packet)
		except Exception, e:
			print 'PT debug: [RECEIVING] {err}'.format(err=e)

		raise self.WAITING()


	# END
	@ATMT.state(final=1)
	def END(self):
		global endAll

		endAll = True

		print '|||| state: END ||||'

	"""
	# EVENTS
	@ATMT.ioevent(BEGIN, name='commands')
	def transition(self, fd):
		print 'in trans'
		commands = ['stop', 'restart', 'pause', 'resume']

		command = fd.recv()

		if command in commands:
			print 'PT debug: got command {cmd}'.format(cmd=command)
			if command == 'stop':

				raise self.END()

			elif command == 'restart':

				self.restart()

			elif command in ['pause', 'resume']:
				self.PAUSED = (True, False)[self.PAUSED]

				raise self.WAITING()
	"""


class ParseData():
	""" Parse Data from / to CE """

	def __init__(self, sniffer=None):
		self.sniffer = sniffer
		self.packet = None
		self.packetHead = None
		self.packetsToParse = list()
		self.packetsToSend = list()
		self.ceSendLock = allocate_lock()

	def run(self):
		if not self.sniffer:
			return

		print 'PT debug: data parser ready ..'

		global endAll
		global sniffedPackets
		while not endAll:
			if self.sniffer:
				self.ce_status_update()
				try:
					with packetsLock:
						self.packetsToParse = list(sniffedPackets)
						del sniffedPackets[:]

					with self.ceSendLock:
						for packet in self.packetsToParse:
							self.packet = packet
							self.packetHead = self.packet_parse()

							if self.filter():
								self.packetsToSend.append({'packetHead': self.packetHead,
															'packet': self.packet})
						del self.packetsToParse[:]

						if self.packetsToSend:
							self.send()
						del self.packetsToSend[:]
				except Exception, e:
					#print 'no packets in list'
					self.packet = None
					self.packetHead = None

			sleep(0.8)
		print 'PT debug: data parser thread ended ..'

	def ce_status_update(self):
		restart = False

		# Try to ping status from CE!
		for ce in self.sniffer.ceObjects:
			try:
				response = ce.proxy.echo('ping')
				#self.sniffer.reinitRetries = 0

				args = {
					'command': 'None',
					'data': str(self.sniffer.uid)
				}
				args['command'] = 'echo'
				pluginData = ce.proxy.runPlugin(self.sniffer.username,
													'PacketSnifferPlugin', args)

				ce.pluginStatus = pluginData['state']

				self.sniffer.filters = pluginData['data']['filters']
				if self.sniffer.filters.has_key('-i'):
					global RESTART
					global IFACE

					if not IFACE == self.sniffer.filters['-i']:
						IFACE = self.sniffer.filters['-i']
						RESTART = True

				if ce.pluginStatus == 'restart':
					args['command'] = 'restarted'
					ce.proxy.runPlugin(self.sniffer.username,
												'PacketSnifferPlugin', args)

					print 'PT debug: sniffer restart ..\n'

					restart = True

				else:
					ce.PAUSED = not ce.pluginStatus == 'running'

				_paused = self.sniffer.PAUSED

				self.sniffer.PAUSED = (sum([ce.PAUSED for ce in self.sniffer.ceObjects])
								== len(self.sniffer.ceObjects))

				if not _paused == self.sniffer.PAUSED:
					print 'PT debug: sniffer paused status chaged: \
												{s}'.format(s=self.sniffer.PAUSED)

			except Exception, e:
				print 'PT warning: Central Engine is down .... [{0}]'.format(e)

				RESTART = True #self.sniffer.io.commands.send('restart')

				sleep(2)

		if restart: RESTART = True #self.sniffer.io.commands.send('restart')

	def filter(self):
		if not self.sniffer.filters: return True

		filterStatus = True

		if self.sniffer.filters.has_key('-proto'):
			pkt = self.packet
			protocols = []
			while not isinstance(pkt, NoPayload):
				protocols.append(pkt.name)
				pkt = pkt.payload

			filterStatus = self.sniffer.filters['-proto'] in protocols

		if self.sniffer.filters.has_key('-mac_src'):
			filterStatus = (self.sniffer.filters['-mac_src'] ==
											self.packetHead['source']['mac'])

		if self.sniffer.filters.has_key('-mac_dst'):
			filterStatus = (self.sniffer.filters['-mac_dst'] ==
											self.packetHead['destination']['mac'])

		if self.sniffer.filters.has_key('-port_src'):
			filterStatus = (self.sniffer.filters['-port_src'] ==
									str(self.packetHead['source']['port']))

		if self.sniffer.filters.has_key('-port_dst'):
			filterStatus = (self.sniffer.filters['-port_dst'] ==
									str(self.packetHead['destination']['port']))

		if self.sniffer.filters.has_key('-ip_src'):
			filterStatus = (self.sniffer.filters['-ip_src'] ==
											self.packetHead['source']['ip'])

		if self.sniffer.filters.has_key('-ip_dst'):
			filterStatus = (self.sniffer.filters['-ip_dst'] ==
											self.packetHead['destination']['ip'])

		return filterStatus

	def packet_parse(self):
		source = {}
		destination = {}
		try:
			source['mac'] = self.packet.fields['src']
			destination['mac'] = self.packet.fields['dst']

			try:
				source['ip'] = self.packet.payload.fields['src']
				destination['ip'] = self.packet.payload.fields['dst']
			except Exception, e:
				source['ip'] = 'None'
				destination['ip'] = 'None'

				#print 'PT debug: packet head exception (ip): {ex}'.format(ex=e)

			try:
				source['port'] = self.packet.payload.payload.fields['sport']
				destination['port'] = self.packet.payload.payload.fields['dport']
			except Exception, e:
				source['port'] = 'None'
				destination['port'] = 'None'

				#print 'PT debug: packet head exception (port): {ex}'.format(ex=e)
		except Exception, e:
			source['mac'] = 'None'
			destination['mac'] = 'None'

			#print 'PT debug: packet head exception (mac): {ex}'.format(ex=e)

		data = {
			'protocol': self.packet.payload.payload.name,
			'source': source,
			'destination': destination,
		}

		return data

	def packet_to_dict(self, packet):
		""" Recursive function to parse packet and return dict """

		if isinstance(packet, Packet):
			_packet = packet.fields
			if not isinstance(packet.payload, NoPayload):
				_packet['payload'] = packet.payload

			return {packet.name: self.packet_to_dict(_packet)}

		elif isinstance(packet, dict):
			for k,v in packet.iteritems():
				packet[k] = self.packet_to_dict(v)

			return packet

		elif isinstance(packet, list):
			for v in packet:
				packet[packet.index(v)] = self.packet_to_dict(v)

		else:

			return packet

	def send(self):
		packetListToSend = list()
		for _pkt in self.packetsToSend:
			self.packet = _pkt['packet']
			self.packetHead = _pkt['packetHead']

			packet_str = str(self.packet)

			try:
				sourcePort = self.packet.payload.payload.fields['sport']
				destinationPort = self.packet.payload.payload.fields['dport']
			except Exception, e:
				sourcePort = None
				destinationPort = None

			if self.sniffer.OFPort in [sourcePort, destinationPort] and of_message_parse:
				try:
					_packet = of_message_parse(str(self.packet.payload.payload.load))
					packet = {'pkt': _packet.show()}
				except Exception as e:
					packet = self.packet_to_dict(self.packet)
			else:
				packet = self.packet_to_dict(self.packet)

			data = {
				'sniffer': {
					'ip': self.sniffer.userip,
					'hostname': self.sniffer.userhost,
					'username': self.sniffer.username,
				},
				'packet_head': self.packetHead,
				'packet': packet,
				'packet_str': packet_str,
			}
			data['packet_head'].update([('id', str(uuid4())), ])
			data = b2a_base64(str(data))
			packetListToSend.append(data)

		packetListToSend = b2a_base64(str(packetListToSend))

		# push packet to central engines
		try:
			for ce in self.sniffer.ceObjects:
				response = ce.proxy.runPlugin(self.sniffer.username,
										'PacketSnifferPlugin',
										{'command': 'pushpkt', 'data': packetListToSend})
				if not response['status']['success']:
					self.ce_status_update()

					response = ce.proxy.runPlugin(self.sniffer.username,
										'PacketSnifferPlugin',
										{'command': 'pushpkt', 'data': packetListToSend})
					if not response['status']['success']:
						print 'PT debug: [SENDING] response: {r}'.format(r=response)
		except Exception, e:
			print 'PT debug: [RECEIVING] {err}'.format(err=e)
