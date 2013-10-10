
# File: CentralEngineClasses.py ; This file is part of Twister.

# version: 2.030

# Copyright (C) 2012-2013 , Luxoft

# Authors:
#    Adrian Toader <adtoader@luxoft.com>
#    Andrei Costachi <acostachi@luxoft.com>
#    Andrei Toma <atoma@luxoft.com>
#    Cristi Constantin <crconstantin@luxoft.com>
#    Daniel Cioata <dcioata@luxoft.com>

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at:

# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Central Engine Class
********************

All functions from Central Engine are EXPOSED and can be accesed via XML-RPC.\n
The Central Engine and each EP have a status that can be: start/ stop/ paused.\n
Each test file has a status that can be: pending, working, pass, fail, skip, etc.\n
All the statuses are defined in "constants.py".\n
Central Engine role is to send files and libraries to User EPs, collect statistics and logs from
each file executed, send e-mail and save to database after each Project execution, run plug-ins.
"""
from __future__ import with_statement

import os
import sys
import time
import datetime
import traceback
import socket
socket.setdefaulttimeout(4)
import binascii
import tarfile
import xmlrpclib
import urlparse
import MySQLdb

import pickle
try: import simplejson as json
except Exception as e: import json

import cherrypy
from cherrypy import _cptools

TWISTER_PATH = os.getenv('TWISTER_PATH')
if not TWISTER_PATH:
    print('$TWISTER_PATH environment variable is not set! Exiting!')
    exit(1)
if TWISTER_PATH not in sys.path:
    sys.path.append(TWISTER_PATH)


from common.constants  import *
from common.helpers    import *
from common.tsclogging import *
from common.xmlparser  import *


# --------------------------------------------------------------------------------------------------
# # # #    C L A S S    C e n t r a l - E n g i n e    # # #
# --------------------------------------------------------------------------------------------------


class CentralEngine(_cptools.XMLRPCController):

    """
    *This class is the core of all operations.*
    """

    def __init__(self, proj):

        proj.parent  = self
        self.project = proj


    @cherrypy.expose
    def default(self, *vpath, **params):
        user_agent = cherrypy.request.headers['User-Agent'].lower()
        if 'xmlrpc' in user_agent or 'xml rpc' in user_agent:
            return super(CentralEngine, self).default(*vpath, **params)
        # If the connection is not XML-RPC, redirect to REST
        raise cherrypy.HTTPRedirect('/web/' + '/'.join(vpath))


# --------------------------------------------------------------------------------------------------
#           H E L P E R   F U N C T I O N S
# --------------------------------------------------------------------------------------------------


    @cherrypy.expose
    def echo(self, msg):
        """
        Simple echo function, for testing connection.
        """
        if msg != 'ping':
            logInfo(':: %s' % str(msg))
        return 'CE reply: ' + msg


    @cherrypy.expose
    def getTwisterPath(self):
        '''
        Returns the Twister Path.
        '''
        global TWISTER_PATH
        return TWISTER_PATH


    @cherrypy.expose
    def getSysInfo(self):
        '''
        Returns some system information.
        '''
        return systemInfo()


    @cherrypy.expose
    def getLogsPath(self, user):
        '''
        Returns the path to Logs files.
        '''
        return self.project.getUserInfo(user, 'logs_path')


    @cherrypy.expose
    def encryptText(self, text):
        """
        Encrypt a piece of text, using AES.\n
        This function is called from the Java GUI.
        """
        if not text: return ''
        return self.project.encryptText(text)


    @cherrypy.expose
    def decryptText(self, text):
        """
        Decrypt a piece of text, using AES.\n
        This function is called from the Java GUI.
        """
        if not text: return ''
        return self.project.decryptText(text)


# # #


    @cherrypy.expose
    def serviceManagerCommand(self, command, name='', *args, **kwargs):
        """
        Send commands to Service Manager.\n
        Valid commands are: list, start, stop, status, get config, save config, get log.
        """
        # Check the username from CherryPy connection
        cherry_roles = self.project._checkUser()
        if not cherry_roles: return False
        if 'CHANGE_SERVICES' not in cherry_roles['roles']:
            logDebug('Privileges ERROR! Username `{user}` cannot use Service Manager!'.format(**cherry_roles))
            return False
        return self.project.manager.sendCommand(command, name, args, kwargs)


    @cherrypy.expose
    def usersAndGroupsManager(self, cmd, name='', *args, **kwargs):
        """
        Manage users, groups and permissions.
        """
        return self.project.usersAndGroupsManager(cmd, name, args, kwargs)


    @cherrypy.expose
    def runUserScript(self, script_path):
        """
        Executes a script.
        Returns a string containing the text printed by the script.\n
        This function is called from the Java GUI.
        """
        return execScript(script_path)


    @cherrypy.expose
    def runDBSelect(self, user, field_id):
        """
        Selects from database.
        This function is called from the Java GUI.
        """
        # Get the path to DB.XML
        db_file = self.project.getUserInfo(user, 'db_config')
        if not db_file:
            errMessage = 'Null DB.XML file for user `{}`! Nothing to do!'.format(user)
            logError(errMessage)
            return errMessage

        # Database parser, fields, queries
        dbparser = DBParser(db_file)
        query = dbparser.getQuery(field_id)
        db_config = dbparser.db_config
        del dbparser

        # Decode database password
        db_password = self.project.decryptText( db_config.get('password') )
        if not db_password:
            errMessage = 'Cannot decrypt the database password!'
            logError(errMessage)
            return errMessage

        try:
            conn = MySQLdb.connect(host=db_config.get('server'), db=db_config.get('database'),
                user=db_config.get('user'), passwd=db_password)
            curs = conn.cursor()
            curs.execute(query)
        except MySQLdb.Error, e:
            errMessage = 'MySQL Error %d: %s' % (e.args[0], e.args[1])
            logError(errMessage)
            return errMessage

        rows = curs.fetchall()
        msg_str = ','.join( '|'.join([str(i) for i in row]) for row in rows )

        curs.close()
        conn.close()

        return msg_str


    @cherrypy.expose
    def sendMail(self, user, force=False):
        """
        Send e-mail after the suites are run.\n
        Server must be in the form `adress:port`.\n
        Username and password are used for authentication.\n
        This function is called every time the Central Engine stops.
        """
        try:
            ret = self.project.sendMail(user, force)
            return ret
        except Exception as e:
            trace = traceback.format_exc()[34:].strip()
            logError('E-mail: Sending e-mail exception `{}` !'.format(trace))
            return False


    @cherrypy.expose
    def commitToDatabase(self, user):
        """
        For each File from each EP, from each Suite, the results of the tests are saved to database,
        exactly as the user defined them in Database.XML.\n
        This function is called from the Java GUI.
        """
        logDebug('CE: Preparing to save into database...')
        time.sleep(3)

        ret = self.project.saveToDatabase(user)
        if ret:
            logDebug('CE: Saving to database was successful!')
        else:
            logDebug('CE: Could not save to database!')
        return ret


# --------------------------------------------------------------------------------------------------
#           S E T T I N G S
# --------------------------------------------------------------------------------------------------


    @cherrypy.expose
    def listSettings(self, user, config='', x_filter=''):
        """
        List all available settings, for 1 config of a user.
        """
        return self.project.listSettings(user, config, x_filter)


    @cherrypy.expose
    def getSettingsValue(self, user, config, key):
        """
        Fetch a value from 1 config of a user.
        """
        return self.project.getSettingsValue(user, config, key)


    @cherrypy.expose
    def setSettingsValue(self, user, config, key, value):
        """
        Set a value for a key in the config of a user.
        """
        return self.project.setSettingsValue(user, config, key, value)


    @cherrypy.expose
    def delSettingsKey(self, user, config, key, index=0):
        """
        Del a key from the config of a user.
        """
        return self.project.delSettingsKey(user, config, key, index)


# --------------------------------------------------------------------------------------------------
#           E P ,   S U I T E   AND   F I L E   V A R I A B L E S
# --------------------------------------------------------------------------------------------------


    @cherrypy.expose
    def listUsers(self, active=False):
        """
        Function called from the CLI,
        to list the users that are using Twister.
        """
        data = self.project.listUsers(active)
        return data


    @cherrypy.expose
    def getUserVariable(self, user, variable):
        """
        Function called from the Execution Process,
        to get information that is available only here, or are hard to get.
        """

        data = self.project.getUserInfo(user, variable)
        if data is None: data = False
        return data


    @cherrypy.expose
    def setUserVariable(self, user, key, variable):
        """
        Function called from the Execution Process,
        to set information that is available only here, or are hard to get.
        """

        return self.project.setUserInfo(user, key, variable)


    @cherrypy.expose
    def searchEP(self, user, epname):
        """
        Search one EP and return True or False.
        """
        epDict = self.project.getUserInfo(user, 'eps')
        return epname in epDict


    @cherrypy.expose
    def listEPs(self, user):
        """
        Returns all EPs for current user.
        """
        epList = self.project.getUserInfo(user, 'eps').keys()
        return ','.join(epList)


    @cherrypy.expose
    def getEpVariable(self, user, epname, variable, compress=False):
        """
        This function is called from the Execution Process,
        to get information that is available only here, or are hard to get:

        - what the user selected in the Java GUI (release, build, comments, etc)
        - the name of the suite, the test files, etc.
        """

        data = self.project.getEpInfo(user, epname).get(variable, False)
        if compress:
            return pickle.dumps(data)
        else:
            return data


    @cherrypy.expose
    def setEpVariable(self, user, epname, variable, value):
        """
        This function is called from the Execution Process,
        to inject values inside the EP classes.\n
        The values can saved in the Database, when commiting.\n
        Eg: the OS, the IP, or other information can be added this way.
        """

        return self.project.setEpInfo(user, epname, variable, value)


    @cherrypy.expose
    def listSuites(self, user, epname):
        """
        Returns all Suites for one EP from current user.
        """
        if not self.searchEP(user, epname):
            logError('CE ERROR! EP `%s` is not in the list of defined EPs: `%s`!' %\
                     (str(epname), self.listEPs(user)) )
            return False

        suiteList   = [str(k)+':'+v['name'] for k, v in self.project.getEpInfo(user, epname)['suites'].items()]
        return ','.join(suiteList)


    @cherrypy.expose
    def getSuiteVariable(self, user, epname, suite, variable):
        """
        Function called from the Execution Process,
        to get information that is available only here, or are hard to get.
        """

        data = self.project.getSuiteInfo(user, epname, suite)
        if not data: return False
        return data.get(variable, False)


    @cherrypy.expose
    def getFileVariable(self, user, epname, file_id, variable):
        """
        Get information about a test file: dependencies, runnable, status, etc.
        """

        data = self.project.getFileInfo(user, epname, file_id)
        if not data: return False
        return data.get(variable, False)


    @cherrypy.expose
    def setFileVariable(self, user, epname, filename, variable, value):
        """
        Set extra information for a Filename, like Crash detected, OS, IP.\n
        Can be called from the Runner.\n
        This change only happens in the memory structure and it is reset every time
        Central Engine is start. If you need to make a persistent change, use setPersistentFile.
        """

        return self.project.setFileInfo(user, epname, filename, variable, value)


    @cherrypy.expose
    def setStartedBy(self, user, data):
        """
        Remember the user that started the Central Engine.\n
        Called from the Java GUI.
        """
        name = data.split(';')[0]
        proj = ';'.join(data.split(';')[1:])
        logDebug('CE: Started by name `{}`, project `{}`.'.format(name, proj))
        self.project.setUserInfo(user, 'started_by', str(name))
        self.project.setUserInfo(user, 'proj_xml_name', str(proj))
        return 1


    @cherrypy.expose
    def getGlobalVariable(self, user, var_path):
        """
        Send a global variable, using a path to the variable.
        """
        return self.project.getGlobalVariable(user, var_path, False)


    @cherrypy.expose
    def setGlobalVariable(self, user, var_path, value):
        """
        Set a global variable path, for a user.\n
        The change is not persistent.
        """
        return self.project.setGlobalVariable(user, var_path, value)


    @cherrypy.expose
    def getConfig(self, user, cfg_path, var_path):
        """
        Send a config file, using the full path to a config file and
        the full path to a config variable in that file.
        """
        return self.project.getGlobalVariable(user, var_path, cfg_path)


# --------------------------------------------------------------------------------------------------
#           C L I E N T   C O N T R O L
# --------------------------------------------------------------------------------------------------


    def _getClientEpProxy(self, user, epname):
        """ Helper function. """

        # Check if epname is known and registered
        userClientsInfo = self.project.getUserInfo(user, 'clients')
        if not userClientsInfo:
            return False
        else:
            userClientsInfo = json.loads(userClientsInfo)

        userClientsInfoEPs = list()
        for cl in userClientsInfo:
            userClientsInfoEPs += userClientsInfo[cl]
        if not self.searchEP(user, epname) or not epname in userClientsInfoEPs:
            logError('Error: Unknown EP : `{}`.'.format(epname))
            return False

        # Get proxy address
        for cl in userClientsInfo:
            if epname in userClientsInfo[cl]:
                return cl

        logError('Error: Unknown proxy for EP : `{}`.'.format(epname))
        return False


    @cherrypy.expose
    def registerClient(self, user, clients):
        """ Register client. """

        clients = json.loads(clients)
        _clients = {}

        for client in clients:
            if client.split(':')[0]:
                addr = client.split(':')[0]
            else:
                addr = cherrypy.request.headers['Remote-Addr']
            _clients.update([('{}:{}'.format(addr,
                                      client.split(':')[1]), clients[client]), ])
        clients = json.dumps(_clients)

        self.setUserVariable(user, 'clients', clients)
        logDebug('Registered client manager for user\n\t`{}` -> {}.'.format(user, clients))
        return True


    @cherrypy.expose
    def startEP(self, user, epname):
        """ Start EP for client. """

        _proxy = self._getClientEpProxy(user, epname)
        if not _proxy:
            logDebug('Cannot start `{}` for user `{}` ! The Client Manager is not started !'.format(epname, user))
            return False

        proxy = xmlrpclib.ServerProxy('http://{pr}/twisterclient/'.format(pr=_proxy))
        ip, port = _proxy.split(':')

        try:
            logDebug('Trying to start `{} {}`.'.format(user, epname))
            return proxy.startEP(epname)
        except Exception as e:
            trace = traceback.format_exc()[34:].strip()
            logError('Error: Start EP error: {}'.format(trace))
            return False


    @cherrypy.expose
    def stopEP(self, user, epname):
        """ Stop EP for client. """

        _proxy = self._getClientEpProxy(user, epname)
        if not _proxy:
            logDebug('Cannot stop `{}` for user `{}` ! The Client Manager is not started !'.format(epname, user))
            return False

        proxy = xmlrpclib.ServerProxy('http://{pr}/twisterclient/'.format(pr=_proxy))
        ip, port = _proxy.split(':')

        try:
            logWarning('Trying to stop `{} {}`.'.format(user, epname))
            return proxy.stopEP(epname)
        except Exception as e:
            trace = traceback.format_exc()[34:].strip()
            logError('Error: Stop EP error: {}'.format(trace))
            return False


    @cherrypy.expose
    def restartEP(self, user, epname):
        """ Restart EP for client. """

        _proxy = self._getClientEpProxy(user, epname)
        if not _proxy:
            logDebug('Cannot restart `{}` for user `{}` ! The Client Manager is not started !'.format(epname, user))
            return False

        proxy = xmlrpclib.ServerProxy('http://{pr}/twisterclient/'.format(pr=_proxy))
        ip, port = _proxy.split(':')

        try:
            logWarning('Trying to restart `{} {}`.'.format(user, epname))
            return proxy.restartEP(epname)
        except Exception as e:
            trace = traceback.format_exc()[34:].strip()
            logError('Error: Restart EP error: {}'.format(trace))
            return False


    @cherrypy.expose
    def queueFile(self, user, suite, fname):
        """
        Queue a file at the end of a suite, during runtime.
        If there are more suites with the same name, the first one is used.\n
        This function writes in TestSuites.XML file, so the change is persistent.
        """
        return self.project.queueFile(user, suite, fname)


    @cherrypy.expose
    def deQueueFiles(self, user, data):
        """
        Remove a file from the files queue.
        """
        return self.project.deQueueFiles(user, data)


# --------------------------------------------------------------------------------------------------
#           C R E A T E   P E R S I S T E N T   S U I T E S
# --------------------------------------------------------------------------------------------------


    @cherrypy.expose
    def setPersistentSuite(self, user, suite, info={}, order=-1):
        """
        Create a new suite, using the INFO, at the position specified.\n
        This function writes in TestSuites.XML file.\n
        The changes will be available at the next START.
        """
        return self.project.setPersistentSuite(user, suite, info, order)


    @cherrypy.expose
    def delPersistentSuite(self, user, suite):
        """
        Delete an XML suite, using a name ; if there are more suites with the same name,
        only the first one is deleted.\n
        This function writes in TestSuites.XML file.
        """
        return self.project.delPersistentSuite(user, suite)


    @cherrypy.expose
    def setPersistentFile(self, user, suite, fname, info={}, order=-1):
        """
        Create a new file in a suite, using the INFO, at the position specified.\n
        This function writes in TestSuites.XML file.\n
        The changes will be available at the next START.
        """
        return self.project.setPersistentFile(user, suite, fname, info, order)


    @cherrypy.expose
    def delPersistentFile(self, user, suite, fname):
        """
        Delete an XML file from a suite, using a name ; if there are more files
        with the same name, only the first one is deleted.\n
        This function writes in TestSuites.XML file.
        """
        return self.project.delPersistentFile(user, suite, fname)


# --------------------------------------------------------------------------------------------------
#           E X E C U T I O N   S T A T U S
# --------------------------------------------------------------------------------------------------


    @cherrypy.expose
    def resetProject(self, user):
        """
        Reset project for 1 user.
        """
        twister_cache = userHome(user) + '/twister/.twister_cache'
        setFileOwner(user, twister_cache)
        return self.project.reset(user)


    @cherrypy.expose
    def getExecStatus(self, user, epname):
        """
        Return execution status for one EP. (stopped, paused, running, invalid)\n
        Called from the EP.
        """
        if not self.searchEP(user, epname):
            logError('CE ERROR! EP `%s` is not in the list of defined EPs: `%s`!' %
                     (str(epname), self.listEPs(user)) )
            return False

        data = self.project.getEpInfo(user, epname)

        # Set EP last seen alive
        self.project.setEpInfo(user, epname, 'last_seen_alive', datetime.datetime.today().strftime('%Y-%m-%d %H:%M:%S'))
        # Return a status, or stop
        reversed = dict((v,k) for k,v in execStatus.iteritems())
        return reversed[data.get('status', 8)]


    @cherrypy.expose
    def getExecStatusAll(self, user):
        """
        Return execution status for all EPs. (stopped, paused, running, invalid)\n
        Called from the Java GUI.
        """
        # If this is a temporary run, return the statuses of the backup user!
        user_agent = cherrypy.request.headers['User-Agent'].lower()
        if 'xml rpc' in user_agent and (user+'_old') in self.project.users:
            user += '_old'

        data = self.project.getUserInfo(user)
        reversed = dict((v,k) for k,v in execStatus.iteritems())
        status = reversed[data.get('status', 8)]

        # If start time is not define, then define it
        if not data.get('start_time'):
            start_time = datetime.datetime.today()
            self.project.setUserInfo(user, 'start_time', start_time.strftime('%Y-%m-%d %H:%M:%S'))
        else:
            start_time = datetime.datetime.strptime(data['start_time'], '%Y-%m-%d %H:%M:%S')

        # If the engine is not stopped, update elapsed time
        if data.get('status', 8) != STATUS_STOP:
            elapsed_time = str(datetime.datetime.today() - start_time).split('.')[0]
            self.project.setUserInfo(user, 'elapsed_time', elapsed_time)
        else:
            elapsed_time = data.get('elapsed_time', 0)

        # Status + start time + elapsed time
        start_time = start_time.strftime('%Y-%m-%d %H:%M:%S')
        return '{0}; {1}; {2}; {3}'.format(status, start_time, elapsed_time, data.get('started_by', 'X'))


    @cherrypy.expose
    def setExecStatus(self, user, epname, new_status, msg=''):
        """
        Set execution status for one EP. (0, 1, 2, or 3)\n
        Returns a string (stopped, paused, running).\n
        The `message` parameter can explain why the status has changed.\n
        Called from the EP.
        """
        return self.project.setExecStatus(user, epname, new_status, msg)


    @cherrypy.expose
    def setExecStatusAll(self, user, new_status, msg=''):
        """
        Set execution status for all EPs. (STATUS_STOP, STATUS_PAUSED, STATUS_RUNNING).\n
        Returns a string (stopped, paused, running).\n
        The `message` parameter can explain why the status has changed.
        Called from the applet.
        """
        return self.project.setExecStatusAll(user, new_status, msg)


# --------------------------------------------------------------------------------------------------
#           T E S T   F I L E   S T A T U S E S
# --------------------------------------------------------------------------------------------------


    @cherrypy.expose
    def getFileStatusAll(self, user, epname=None, suite=None):
        """
        Returns a list with all statuses, for all files, in order.\n
        The status of one file can be obtained with get File Variable.\n
        Called from the Java GUI.
        """
        if epname and not self.searchEP(user, epname):
            logError('CE ERROR! EP `%s` is not in the list of defined EPs: `%s`!' % \
                (str(epname), self.listEPs(user)) )
            return ''

        # If this is a temporary run, return the statuses of the backup user!
        user_agent = cherrypy.request.headers['User-Agent'].lower()
        if 'xml rpc' in user_agent and (user+'_old') in self.project.users:
            statuses = self.project.getFileStatusAll(user + '_old', epname, suite)
            return ','.join(statuses)

        statuses = self.project.getFileStatusAll(user, epname, suite)
        return ','.join(statuses)


    @cherrypy.expose
    def setFileStatus(self, user, epname, file_id, new_status=10, time_elapsed=0.0):
        """
        Set status for one file and write in log summary.\n
        Called from the Runner.
        """
        return self.project.setFileStatus(user, epname, file_id, new_status, time_elapsed)


    @cherrypy.expose
    def setFileStatusAll(self, user, epname, new_status):
        """
        Reset file status for all files of one EP.\n
        Called from the Runner.
        """
        return self.project.setFileStatusAll(user, epname, new_status)


# --------------------------------------------------------------------------------------------------
#           L I B R A R Y   AND   T E S T   S U I T E   F I L E S
# --------------------------------------------------------------------------------------------------


    @cherrypy.expose
    def listPlugins(self, user):

        parser = PluginParser(user)
        pluginsList = parser.getPlugins()

        return pluginsList.keys()


    @cherrypy.expose
    def runPlugin(self, user, plugin, args):

        # If argument is a string
        if type(args) == type(str()):
            try:
                args = urlparse.parse_qs(args)
            except:
                msg = 'CE ERROR: Cannot run plugin `%s` with arguments `%s`!' % (plugin, args)
                logError(msg)
                return msg
        # If argument is a valid dict, pass
        elif type(args) == type(dict()):
            if not 'command' in args:
                return 'CE ERROR: Invalid dictionary for plugin `%s` : %s !' % (plugin, args)
        else:
            return 'CE ERROR: Invalid type of argument for plugin `%s` : %s !' % (plugin, type(args))

        plugin_p = self.project._buildPlugin(user, plugin)

        if not plugin_p:
            msg = 'CE ERROR: Plugin `{0}` does not exist for user `{1}`!'.format(plugin, user)
            logError(msg)
            return msg
        # else:
        #    logDebug('Running plugin:: `{0}` ; user `{1}` ; {2}'.format(plugin, user, args))

        try:
            return plugin_p.run(args)
        except Exception as e:
            trace = traceback.format_exc()[34:].strip()
            logError('CE ERROR: Plugin `{}`, ran with arguments `{}` and returned EXCEPTION: `{}`!'\
                     .format(plugin, args, trace))
            return 'Error on running plugin `{}` - Exception: `{}`!'.format(plugin, e)


    @cherrypy.expose
    def runTemporary(self, user, files_xml):
        """
        This function allows the Central Engine to run a temporary Test-Suite XML file
        that contains any valid combination of suites and files.
        The temporary run does not affect the normal suites and files.
        The results are Not saved to database and No report is sent on e-mail.
        """

        # Cannot run temporary more than once
        if user + '_old' in self.project.users:
            logError('CE ERROR: User `{0}` is already running temporary!'.format(user))
            return '*ERROR!* User `{0}` is already running temporary!'.format(user)
        # If the user is already running something, DON'T run temporary
        if self.project.getUserInfo(user, 'status'):
            logError('*ERROR!* User `{0}` must be stopped, before running temporary!'.format(user))
            return '*ERROR!* User `{0}` must be stopped, before running temporary!'.format(user)

        # Backup all username data, under a different name
        self.project.renameUser(user, user + '_old')
        # Create a temporary user
        self.project.createUser(user, base_config='', files_config=files_xml)

        # Update status for temporary user
        self.project.setUserInfo(user, 'status', STATUS_RUNNING)
        # Update status for all active EPs
        active_eps = self.project.parsers[user].getActiveEps()
        for epname in active_eps:
            self.project.setEpInfo(user, epname, 'status', STATUS_RUNNING)

        i = 0
        while self.project.getUserInfo(user, 'status') == STATUS_RUNNING:
            if not i:
                logWarning('Temporary user `{}` is running on EP list `{}`...'.format(user, active_eps))
            i += 1
            if i == 12: i = 0
            time.sleep(1)

        # Delete temporary user
        self.project.deleteUser(user)
        # Restore previous user
        self.project.renameUser(user + '_old', user)

        return True


    @cherrypy.expose
    def getLibrariesList(self, user='', all=True):
        """
        Returns the list of exposed libraries, from CE libraries folder.\n
        This list will be used to syncronize the libs on all EP computers.\n
        Called from the Runner and the Java GUI.
        """
        global TWISTER_PATH
        libs_path = (TWISTER_PATH + '/lib/').replace('//', '/')
        user_path = ''
        if self.project.getUserInfo(user, 'libs_path'):
            user_path = self.project.getUserInfo(user, 'libs_path') + os.sep
        if user_path == '/':
            user_path = ''

        glob_libs = [] # Default empty
        user_libs = []

        # All Python source files from Libraries folder AND all library folders
        if all:
            glob_libs = [d for d in os.listdir(libs_path) if \
                ( os.path.isfile(libs_path + d) and \
                '__init__.py' not in d and \
                os.path.splitext(d)[1] in ['.py', '.zip']) or \
                os.path.isdir(libs_path + d) ]

            if user_path and os.path.isdir(user_path):
                user_libs = [d for d in os.listdir(user_path) if \
                        ( os.path.isfile(user_path + d) and \
                        '__init__.py' not in d and \
                        os.path.splitext(d)[1] in ['.py', '.zip']) or \
                        os.path.isdir(user_path + d) ]
        # All libraries for user
        else:
            if user:
                # If `libraries` is empty, will default to ALL libraries
                tmp_libs = self.project.getUserInfo(user, 'libraries') or ''
                glob_libs = [x.strip() for x in tmp_libs.split(';')] if tmp_libs else []
                del tmp_libs

        # Return a list with unique names, sorted alphabetically
        return sorted( set(glob_libs + user_libs) )


    @cherrypy.expose
    def downloadLibrary(self, user, name):
        """
        Sends required library to EP, to be syncronized.\n
        The library can be global for all users, or per user.\n
        Called from the Runner.
        """
        global TWISTER_PATH

        lib_path = (TWISTER_PATH + '/lib/' + name).replace('//', '/')
        if self.project.getUserInfo(user, 'libs_path'):
            user_lib = self.project.getUserInfo(user, 'libs_path') + os.sep + name
        else:
            user_lib = ''

        # If the requested library is in the second path (user path)
        if os.path.exists(user_lib):
            final_path = user_lib
        # If the requested library is in the main path (global path)
        elif os.path.exists(lib_path):
            final_path = lib_path
        else:
            logError('ERROR! Library `{}` does not exist!'.format(name))
            return False

        # Python and Zip files
        if os.path.isfile(final_path):
            logDebug('CE: Requested library file: `{}`.'.format(name))
            with open(final_path, 'rb') as binary:
                return xmlrpclib.Binary(binary.read())

        # Library folders must be compressed
        else:
            logDebug('CE: Requested library folder: `{}`.'.format(name))
            split_name = os.path.split(final_path)
            rnd = binascii.hexlify(os.urandom(5))
            tgz = split_name[1] + '_' + rnd + '.tgz'
            os.chdir(split_name[0])
            with tarfile.open(tgz, 'w:gz') as binary:
                binary.add(name=split_name[1], recursive=True)
            with open(tgz, 'r') as binary:
                data = xmlrpclib.Binary(binary.read())
            os.remove(tgz)
            return data


    @cherrypy.expose
    def getEpFiles(self, user, epname):
        """
        Returns all files that must be run on one EP.\n
        Called from the Runner.
        """
        try: data = self.project.getEpFiles(user, epname)
        except: data = False
        return data


    @cherrypy.expose
    def getSuiteFiles(self, user, epname, suite):
        """
        Returns all files that must be run on one Suite.\n
        Called from the Runner.
        """
        try: data = self.project.getSuiteFiles(user, epname, suite)
        except: data = False
        return data


    @cherrypy.expose
    def getTestFile(self, user, epname, file_id):
        """
        Sends requested file to TC, to be executed.\n
        Called from the Runner.
        """
        if not self.searchEP(user, epname):
            logError('*ERROR* `{}` is not in the list of defined EPs: `{}`!'.format(epname, self.listEPs(user)))
            return False
        if not self.project.getEpInfo(user, epname).get('status'):
            logError('*ERROR* `{}` requested file list, but the EP is closed!'.format(epname))
            return False

        data = self.project.getFileInfo(user, epname, file_id)
        if not data:
            logError('*ERROR* Invalid File ID `{}` !'.format(file_id))
            return False

        filename = data['file']
        tests_path = self.project.getUserInfo(user, 'tests_path')

        # Inject this empty variable just to be sure.
        self.project.setFileInfo(user, epname, file_id, 'twister_tc_revision', '')

        # Fix ~ $HOME path
        if filename.startswith('~'):
            filename = userHome(user) + filename[1:]

        # Fix incomplete file path
        if not os.path.isfile(filename):
            if not os.path.isfile(tests_path + os.sep + filename):

                # Injected ClearCase file ?
                if 'ClearCase' in self.listPlugins(user) and data.get('clearcase'):
                    plugin_p = self.project._buildPlugin(user, 'ClearCase')
                    try:
                        data = plugin_p.getTestFile(filename)
                    except Exception as e:
                        trace = traceback.format_exc()[34:].strip()
                        logError('Error getting ClearCase file `{}` : `{}`!'.format(filename, trace))
                        return ''
                    try:
                        descr = plugin_p.getTestDescription(user, filename)
                        cctag = '<b>ClearCase Version</b> :'
                        if descr:
                            pos = descr.find(cctag) + len(cctag)
                            rev = descr[pos:].strip()
                            self.project.setFileInfo(user, epname, file_id, 'twister_tc_revision', rev)
                    except Exception as e:
                        pass
                    logDebug('CE: Execution process `{}:{}` requested file `{}`.'.format(user, epname, filename))
                    return data

                logError('*ERROR* TestCase file: `{}` does not exist!'.format(filename))
                return ''
            else:
                filename = tests_path + os.sep + filename

        logDebug('CE: Execution process `{}:{}` requested file `{}`.'.format(user, epname, filename))

        with open(filename, 'rb') as handle:
            return xmlrpclib.Binary(handle.read())


    @cherrypy.expose
    def getTestDescription(self, fname):
        """
        Returns the title, description and all tags from a test file.\n
        Called from the Java GUI.
        """
        if os.path.isfile(fname):
            # This function is defined in helpers.
            return getFileTags(fname)

        # If the user has roles and the ClearCase plugin is enabled...
        cherry_roles = self.project._checkUser()
        if cherry_roles:
            user = cherry_roles['user']
            if 'ClearCase' in self.listPlugins(user):
                plugin_p = self.project._buildPlugin(user, 'ClearCase')
                try:
                    return plugin_p.getTestDescription(user, fname)
                except Exception as e:
                    trace = traceback.format_exc()[34:].strip()
                    logError('Error getting description from ClearCase file `{}` : `{}`!'.format(fname, trace))
                    return ''

        return ''


# --------------------------------------------------------------------------------------------------
#           L O G S
# --------------------------------------------------------------------------------------------------


    @cherrypy.expose
    def getLogFile(self, user, read, fstart, filename):
        """
        Called in the Java GUI to show the logs.
        """
        return self.project.getLogFile(user, read, fstart, filename)


    @cherrypy.expose
    def logMessage(self, user, logType, logMessage):
        """
        This function is exposed in all tests, all logs are centralized in the HOME of the user.\n
        In order for the user to be able to access the logs written by CE, which runs as ROOT,
        CE will start a small process in the name of the user and the process will write the logs.
        """
        return self.project.logMessage(user, logType, logMessage)


    @cherrypy.expose
    def logLIVE(self, user, epname, logMessage):
        """
        Writes CLI messages in a big log, so all output can be checked LIVE.\n
        Called from the EP.
        """
        return self.project.logLIVE(user, epname, logMessage)


    @cherrypy.expose
    def resetLog(self, user, logName):
        """
        Resets one log.\n
        Called from the Java GUI.
        """
        return self.project.resetLog(user, logName)


    @cherrypy.expose
    def resetLogs(self, user):
        """
        All logs defined in master config are erased.\n
        Called from the Java GUI and every time the project is reset.
        """
        return self.project.resetLogs(user)


    @cherrypy.expose
    def panicDetectConfig(self, user, command, data=None):
        """
        Configure Panic Detect.
        """
        # If argument is a string
        if type(data) == type(str()):
            try:
                _data = urlparse.parse_qs(data)
                if _data:
                    data = {k: v[0] if isinstance(v, list) else v for k,v in _data.iteritems()}
            except:
                msg = 'CE ERROR: PD cannot parse data: {d}!'.format(d=data)
                logError(msg)
                return msg

        if data:
            args = {
                'command': command,
                'data': data,
            }
        else:
            args = {
                'command': command,
            }

        return self.project.panicDetectConfig(user, args)


# Eof()
