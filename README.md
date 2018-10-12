# Istio Routing Mission

## Purpose
Showcases Istio's dynamic routing capabilities with a minimal set of example applications.

## Prerequisites
* Openshift 3.10 cluster with Istio. For local development, download the latest release from [Maistra](https://github.com/Maistra/origin/releases) and run:

```bash
# Set oc to be the Maistra one
oc cluster up --enable="*,istio"
oc login -u system:admin
oc apply -f https://raw.githubusercontent.com/Maistra/openshift-ansible/maistra-0.2.0-ocp-3.1.0-istio-1.0.2/istio/cr-minimal.yaml -n istio-operator
oc get pods -n istio-system -w
```
Wait until the `openshift-ansible-istio-installer-job-xxxx` job has completed. It can take several minutes. The OpenShift console is available on https://127.0.0.1:8443.

* Create a new project/namespace on the cluster. This is where your application will be deployed.

```bash
oc login -u system:admin
oc adm policy add-cluster-role-to-user admin developer --as=system:admin
oc login -u developer -p developer
oc new-project <whatever valid project name you want> # not required
```

## Build and deploy the application

### With Fabric8 Maven Plugin (FMP)
Execute the following command to build the project and deploy it to OpenShift:

```bash
mvn clean fabric8:deploy -Popenshift
```

Configuration for FMP may be found both in pom.xml and `src/main/fabric8` files/folders.

This configuration is used to define service names and deployments that control how pods are labeled/versioned on the OpenShift cluster. Labels and versions are key concepts for creating load-balanced or multi-versioned pods in a service.

### With S2I Templates

```bash
find . | grep openshiftio | grep application | xargs -n 1 oc apply -f

oc new-app --template=vertx-istio-routing-client-service -p SOURCE_REPOSITORY_URL=https://github.com/openshiftio-vertx-boosters/vertx-istio-routing-booster -p SOURCE_REPOSITORY_REF=master -p SOURCE_REPOSITORY_DIR=vertx-istio-routing-client
oc new-app --template=vertx-istio-routing-service-a -p SOURCE_REPOSITORY_URL=https://github.com/openshiftio-vertx-boosters/vertx-istio-routing-booster -p SOURCE_REPOSITORY_REF=master -p SOURCE_REPOSITORY_DIR=vertx-istio-routing-service-a
oc new-app --template=vertx-istio-routing-service-b -p SOURCE_REPOSITORY_URL=https://github.com/openshiftio-vertx-boosters/vertx-istio-routing-booster -p SOURCE_REPOSITORY_REF=master -p SOURCE_REPOSITORY_DIR=vertx-istio-routing-service-b
```

##  Use Cases

### Configure an ingress Route to access the application

* Create a RouteRule to forward traffic to the demo application. This is only necessary if your application accepts 
traffic at a different port/url than the default. In this case, our application accepts traffic at `/`, but we will access it with the path `/example`.

```bash
oc apply -f rules/client-route-rule.yaml -n $(oc project -q)
```

* Access the application
Run the following command to determine the appropriate URL to access our demo. Make sure you access the url with the HTTP scheme. HTTPS is NOT enabled by default:

```bash
 echo http://$(oc get route istio-ingressgateway -o jsonpath='{.spec.host}{"\n"}' -n istio-system)/example/
```

The result of the above command is the istio-system istio-ingress URL, appended with the RouteRule path. Open this URL in your a web browser.

### Transfer load between two versions of an application/service

* Access the application as described in the previous use case
  * Click "Invoke Service" in the client UI (Do this several times.)
  * Notice that the services are load-balanced at exactly 50%, which is the default cluster behavior.

* Configure a load-balancing RouteRule. Sometimes it is important to slowly direct traffic to a new service over time, or 
use alternate weighting. In this case, we will supply another Istio RouteRule to control load balancing behavior. Run the 
following command:

```bash
oc apply -f rules/load-balancing-rule.yaml
```

  The RouteRule defined in the file above uses labels "a" and "b" to identify each unique version of the service. If multiple services match any of these labels, traffic will be divided between them accordingly. Additional routes/weights can be supplied using additional labels/service versions as desired.

* Click "Invoke Service" in the client UI. Do this several times. You will notice that traffic is no longer routed at 50/50%, and more traffic is directed to service version B than service version A. Adjust the weights in the rule-file and re-run the command above. You should see traffic adjust accordingly.


_NOTE:_ It could take several seconds for the RouteRule to be detected and applied by Istio.

Congratulations! You now know how to direct traffic between different versions of a service using Istio RouteRules.

## Undeploy the application

### With Fabric8 Maven Plugin (FMP)
```bash
mvn fabric8:undeploy
```

### Remove the namespace
This will delete the project from the OpenShift cluster
```bash
oc delete project <your project name>
```
