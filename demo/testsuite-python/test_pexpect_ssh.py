
# version: 2.001

import time
import pexpect

#
# <title>Test pExpect SSH</title>
# <description>This test is connecting to a SSH server, using pExpect.</description>
#

def test():

	testName = 'test_pexpect_ssh.py'
	logMsg('logTest', "\nTestCase:%s starting\n" % testName)

	error_code = "PASS"

	print '=== Connecting to SSH ==='
	child = pexpect.spawn('ssh user@localhost')

	child.expect('.+assword:', timeout=10)
	child.sendline("password")
	print child.before[:-4]
	time.sleep(1)

	child.expect('user@localhost:', timeout=5)
	child.sendline("cd twister")
	print child.before[:-4]
	print child.after
	time.sleep(1)

	child.expect('user@localhost:', timeout=5)
	child.sendline("ls -la")
	print child.before[:-4]
	print child.after
	time.sleep(1)

	child.expect('user@localhost:', timeout=5)
	child.sendline("exit")
	print child.before[:-4]
	print child.after
	time.sleep(1)

	logMsg('logTest', "TestCase:%s %s\n" % (testName, error_code))

	# This return is used by the framework!
	return error_code

#

# Must have one of the statuses:
# 'pass', 'fail', 'skipped', 'aborted', 'not executed', 'timeout', 'invalid'
_RESULT = test()
