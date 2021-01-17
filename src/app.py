from flask import Flask, render_template,request
from kubernetes import client
from configparser import SafeConfigParser
import socket
#import requests
import subprocess
import os

app = Flask(__name__)
parser = SafeConfigParser()

@app.route('/')
def index():
    containerId=subprocess.check_output('cat /proc/self/cgroup | grep kubepods | cut -d "/" -f 4 | tail -1', shell=True, encoding='utf-8').strip()
    hostname = socket.gethostname()
    IPAddr = socket.gethostbyname(hostname) 
    clientIP = request.remote_addr
    requestMethod = request.method
    requestPath = request.path
    projectID = os.environ['PROJECTID']

    pods = getPods()
    #return render_template('index.html', pods=pods)
    return render_template('index.html', containerId=containerId, hostname=hostname, 
                                         IPAddr=IPAddr, clientIP=clientIP, requestMethod=requestMethod, 
                                         requestPath=requestPath, projectID=projectID, pods=pods)

@app.route('/alive')
def alive():
    return "Yes"

@app.route('/hello/<name>')
def hello(name=None):
    return render_template('hello.html',
                           greeting=parser.get('features', 'greeting', fallback="Howdy"),
                           name=name)

def getPods():
    aToken = "eyJhbGciOTzU5bnR2MncifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJsaXN0LXBvZHMtdG9rZW4tNG40NmgiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC5uYW1lIjoibGlzdC1wb2RzIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiOThhNjZlZDgtYjE0My00NDM1LWJhZDYtMzg1ZTI2OWNmZjUzIiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50Omt1YmUtc3lzdGVtOmxpc3QtcG9kcyJ9.H8vqg9YK2OFbR3ywItai_tNMU70KDxF4QyfzB64vn7obKZgwYTHk-uF7twr6xk6mDRWlsWe5xLADWOc4VP7ZF5cA3eZUobKGDRCkHvKxd4sOSNgRmwkrRrFp-Rm87Q2K-_rxZDptUCVPPzT2nG2VIFGVFJuqvebt7GzvOuSrWi6NuAag_LyE9Y4eZydJ_Uq24fqiWVGd7fCVSOOf_QrBoS73kkRYRBxO_xB_Q-MV8BCA90UJUUUCSTAcC3OV0QpoPxJmzlLp9dgk0yEpexvlCtR_0gBKoHVLPY1NBVHAqSxrRmkhjP68wZEOb6Jr4Gji2byGfyzYl746uVTV0fN8MQ"
    aConfiguration = client.Configuration()

    # # Specify the endpoint of your Kube cluster
    aConfiguration.host = "https://34.126.xx.xx"
    aConfiguration.verify_ssl = False
    aConfiguration.api_key = {"authorization": "Bearer " + aToken}

    # # Create a ApiClient with our config
    aApiClient = client.ApiClient(aConfiguration)

    v1 = client.CoreV1Api(aApiClient)
    #ret = v1.list_namespaced_pod("default",label_selector=label_selector)
    ret = v1.list_namespaced_pod("default")
    return ret.items

if __name__ == "__main__":
    app.run(host="0.0.0.0")
