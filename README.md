Bachelor-Thesis
===============

"Application Virtualization as a Strategy for Cyber Foraging in Resource-Constrained Environments"

This repository contains the implementation for my Bachelor Thesis in CS.

Concept
-------
Mobile devices can extend its computing power and save battery power by leveraging resource-strong surrogate machines that reside in close proximity. The technique of code offloading to such machines is known as cyber foraging. In our context, these surrogate machines are called cloudlets. Cloudlets have advantages over clouds in regard to latency. Furthermore, they can be operated in areas where a reliable Internet connection does not exist. Cloudlets are stateless and require the mobile device to provision the cloudlet before its usage.

In our implementation every application that should be offloaded has to be split into a client, which is an Android app, and a server that runs on the cloudlet. For example, a face recognition server that detects faces in images that are taken by the mobile's camera and the corresponding app for that.

The server is a virtualized application, that means there is another layer of abstraction that isolates the application from the operation system. That makes it portable across distribution boundaries (e.g. "modern" Linux distros) but not across OS family boundaries.

Architecture
------------
The Cloudlet Client is an Android app that lists all offload-ready applications. On clicking one of them, the application gets deployed on a cloudlet that matches the rewquirements listed in the application's metadate file (i.e. a json file). After successful deployment the application client Android app is started an can interact with the application server on the cloudlet.

The Cloudlet Server is a Jetty HTTP server running on the cloudlet machine. It offers a REST interface where POST delivers the application metadata, PUT delivers the actual compressed application server, DELETE deletes the application server and GET returns status about the application's deployment status. Every application is identified by the md5 checksum of the compressed application server. The Cloudlet Server registers via JmDNS and can thereby be discoverered by the cloudlet client.