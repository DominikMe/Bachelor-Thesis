Bachelor-Thesis
===============

"Application Virtualization as a Strategy for Cyber Foraging in Resource-Constrained Environments"

This repository contains the implementation for my Bachelor Thesis in CS.

Concept
-------
Mobile devices can extend its computing power and save battery power by leveraging resource-strong surrogate machines that reside in close proximity. The technique of code offloading to such machines is known as cyber foraging. In our context, these surrogate machines are called cloudlets. Cloudlets have advantages over clouds in regard to latency. Furthermore, they can be operated in areas where a reliable Internet connection does not exist. We call those areas hostile environments, e.g. think of natural disaster regions or the desert. A cloudlet is stateless and requires provisioning by the mobile device before it can serve as a surrogate.

In our implementation every offload-ready application is split into a client that is an Android app and a server application that runs on the cloudlet. One example is a face recognition server plus the corresponding Android app for taking and sending images to the server.

The server is a virtualized application, that means there is an underlying abstraction layer that isolates the application from the operation system. This makes it portable across OS distribution boundaries (e.g. across Linux distros) but not across OS family (Linux vs. Windows) boundaries.

Architecture
------------
The Cloudlet Client is an Android app that lists all offload-ready applications. When clicking on one of them, the application server gets transferred and deployed on a nearby cloudlet. This cloudlet must match the requirements listed in the offload-ready application's metadate file (i.e. a json file). After successful deployment the application client (the corresponding Android app) is started and can interact with the application server on the cloudlet.

The Cloudlet Server is a Jetty HTTP server running on the cloudlet machine. It offers a REST interface where POST delivers the application metadata, PUT delivers the (compressed) application server, DELETE deletes the application server and GET returns status about the application's deployment status. Every application is identified by the md5 checksum of the compressed application server. The Cloudlet Server registers via JmDNS and can thereby be discoverered by the cloudlet client.

Dependencies
------------
Cloudlet Server: >= Java 7
Cloudlet Client: >= Android 4.1.1

Application Virtualization Tools: Cameyo (cameyo.com), CDE (pgbovine.net/cde.html)

If you use other virtualization tools, you need to add an Executor implementation for this new package type.

Setup
-----

A cloudlet should be setup as follows:
Run a VM hypervisor that manages VMs of different kind. (In my thesis context, I used Windows XP and Ubuntu 10.04.) Every VM has to be operated in Bridged mode so that it has access to the local WiFi with its own IP address. Edit the properties.json file in the folder with the CloudletServer.jar, its content defines what information the cloudlet server will distribute via JmDNS (e.g., "architecture": "x86-64"). Start the cloudlet server (java 7 required).

For the Android phone:
Install the cloudlet client. Install all application clients. For each application, put a folder in "sdcard/myCloudlets/apps/servers/" that holds two files: the metadata json file and the compressed archive with the application server. The application server itself is a folder containing the executable and all its dependencies. The metadata is the counterpart of the cloudlet properties; it gives necessary information such as the name, filesize and md5sum, but it also declares the application's requirements on the cloudlet. For details what needs to be declared and how, please read my thesis.

Creating virtual applications:
Please read the instructions on cameyo.com and pgbovine.net/cde.html, respectively.