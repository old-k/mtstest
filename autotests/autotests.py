import requests
import json
import pprint
import urllib
import uuid

host='http://computerica.asuscomm.com:40400'

ses = requests.Session()


def sendPost(ses: requests.Session, url):
    headers = {'content-type': 'application/json'}
    print('Sending POST to: %s' % url)
    rsp = ses.post(url, headers=headers)
    print('Response: code:%s text=%s' % (rsp.status_code, rsp.content.decode("utf-8")))
    return rsp;

def sendGet(ses: requests.Session, url):
    headers = {'content-type': 'application/json'}
    print('Sending GET to: %s' % url)
    rsp = ses.get(url, headers=headers)
    print('Response: code:%s text=%s' % (rsp.status_code, rsp.content.decode("utf-8")))
    return rsp



rsp = sendPost(ses, '%s/task' % host)
assert rsp.status_code == 202
taskUID = rsp.content.decode("utf-8")

rsp = sendGet(ses, '%s/task/abde-123-456' % host)
assert rsp.status_code == 400

rsp = sendGet(ses, '%s/task/%s' % (host, str(uuid.uuid4())))
assert rsp.status_code == 404

rsp = sendGet(ses, '%s/task/%s' % (host, taskUID))
assert rsp.status_code == 200
print('Status: %s' % rsp.content.decode("utf-8"))

rsp = sendGet(ses, '%s/task/%s' % (host, taskUID))
assert rsp.status_code == 200
print('Status: %s' % rsp.content.decode("utf-8"))





