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
    with open('/run/secrets/kubernetes.io/serviceaccount/token', 'r') as file:
        aToken = file.read().replace('\n', '')
    
    aConfiguration = client.Configuration()

    # # Specify the endpoint of your Kube cluster
    aConfiguration.host = "https://" + os.environ['K8S_API_ENDPOINT']
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
