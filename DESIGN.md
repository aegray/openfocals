
# Design



## Prerequisite info on focals

The glasses run an Android Wear OS.  Your phone connects to the glasses by keeping an open bluetooth
socket connection and sending and receiving bluetooth messages/commands.  Additionally, the glases 
can ask the phone to open a network connection, and if the phone successfully connects, can send network
packets (which are forwarded by the phone to the remote site) and receive network packets (the phone
receives and forwards these to the glasses).

Some of the requests the glasses make are raw socket data, some are plaintext http website requests,
some are ssl encrypted/https requests, and some are websocket requests.

## High level
This app generally works by setting up a bluetooth connection to the glasses and sending commands
and handling requests from the glasses.  It also manages network connections for the glasses - serving 
data from local services for some network requests and making external connections for others.  

In order to be able to locally serve some services which make https requests from the glasses, there 
is also logic to handle ssl encrypted connections to some subset of destination addresses.  This logic
does a standard ssl handshake, provides certificates, decrypts incoming data and encrypts outgoing 
data sent from internal services in response to these requsts.  

To support ssl decryption/encryption, On connecting to the glasses, the app sends a command to 
tell the glasses to use a different address than the default one for North's cloud service.  This 
address is ofocals.com (a domain I own in order to get an ssl certificate issued) - and uses the 
certificate I have for this domain in order to serve cloud requests from the glasses (This 
certificate may periodically need to be updated in the app).

Internally, the service has a registration mechanism for different types of socket + web services.
The cloud mock service provides an ssl webserver and serves requests which were intended for cloud.
The presentation provider provides slides of text for the presentation app on the glasses.  There is
also an http endpoint handler to register various http hosts/websites.

The frontend application starts up a service which manages all of these things, then controls
commands and settings that are sent to this service.  


## Event bus

All different parts of the system are linked together using an EventBus (the greenrobot library provides this).  
This lets different subcomponents operate semi independently without knowledge of what they'll be
linked to by pushing various messages onto a global event bus, and listening to the event bus
for messages from other components.


## Device + Device service

When the app starts up, it starts an android service to manage the focals device through bluetooth (DeviceService).
This can be found at focals_buddy/app/src/main/java/com/openfocals/services/DeviceService.java .
I used a service so that it could run in the background even when the app is closed.  

This service first creates a handle to a Device (focals_buddy/app/src/main/java/com/openfocals/focals/Device.java), 
which manages the bluetooth connection to the glasses and provides methods to send various messages/commands
to the glasses.  It also takes various events (glasses connected, disconnected) and incoming 
bluetooth messages and publishes them to the event bus for other classes/subservices to pick up.

After creating a device, the service then starts/sets up:
* a NetworkService - which manages network requets to/from the device.  It additionally
sets up handlers for :
** Managing network connections for the glasses (through 
(focals_buddy/app/src/main/java/com/openfocals/services/network/NetworkSocketManager.java)
** Local classes to manage various different types of requests from the glasses where the application
can act as the remote webserver without making any actual external network connections. 
(focals_buddy/app/src/main/java/com/openfocals/services/network/InterceptedNetworkServiceManager.java)

* a subservice which listens to the phones network connectivity state and, when it changes or 
the glasses initially connect, sends a message to the glasses informing them of the network state,
(so they know if it's ok to open network connections).
(through focals_buddy/app/src/main/java/com/openfocals/services/network/NetConnectedService.java)

* a notification subservice to handle pushing notifications from the phone.
(through focals_buddy/app/src/main/java/com/openfocals/services/notifications/NotificationSender.java)

* an alexa authorization service that listens to the event bus for messages coming from the glasses
related to the Alexa authorization state on the glasses (this gets used by the frontend to decide
if it should show a dialog to setup / login to alexa)
(through focals_buddy/app/src/main/java/com/openfocals/services/alexa/AlexaAuthState.java)

* a class/subservice to intercept attempts to connect to North's cloud webserver and acts as a fake
cloud webserver (serving various standard pages that the glasses may request - for example to 
send notes, tasks, weather). 
(through focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/ClloudMockService.java)

* a mock location service which sends a hardcoded location to the glasses whenever the 
glasses connect.  I forget why this was needed - it may have been an experiment or it may have 
been required to make the glasses think they had all the information they needed.
(through focals_buddy/app/src/main/java/com/openfocals/services/location/LocationService.java)

* a custom app service - which registers an http endpoint with the NetworkManager to 
manage communications with a custom application I wrote that ran on the glasses.  The application
would connect to this endpoint, list out available qml applications which are available from within
the app, download them, and launch them as a sub application which could provide new lenses (screens) 
on the glasses main carousel.  
(through focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/CustomFocalsAppService.java)

* a presentation intercept subservice which handles network connections from the device to a
site that used to serve presentation slides to the glasses - instead providing custom applications 
which can provide different sets of slides to the presentation app on the glasses (for example
to show scrolling text, show fixed text slides, show audio subtitles - although this most likely
doesn't work currently without uploading separate speech recognition model file to the phone).  
New custom apps can be built to provide slides of text to the phone relatively easily.
(through focals_buddy/app/src/main/java/com/openfocals/services/network/present/PresentationInterceptService.java)

* a media playback service - which handles requests made to the cloud music service - which handles 
commands from the phone to control audio playback on the phone.  
(through focals_buddy/app/src/main/java/com/openfocals/services/media/MediaPlaybackService.java)

* a file transfer service - which can handle file transfers to the glasses and has some debug 
logic for sending an ota update file to the glasses.  File transfers are mainly used for sending 
icons and images to the glasses (generally for notifications+messages)
(through focals_buddy/app/src/main/java/com/openfocals/services/files/FileTransferService.java)

* A software update service - which was a relatively unimplemented class that just logs when the 
glasses ask for if there's an available ota update.
(through focals_buddy/app/src/main/java/com/openfocals/services/update/SoftwareUpdateService.java)

* A screen mirror service - which provides a websocket server to a custom app I wrote and sends 
images of a subsection of the phones screen in order to mirror part of the phone to the glasses
(through focals_buddy/app/src/main/java/com/openfocals/services/network/ScreenMirrorWSService.java)

* A tasks mock cloud integration - providing dummy task data to the glasses
(through focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/integrations/TasksIntegration.java)

* A notes mock cloud integration - providing dummy notes data to the glasses 
(through focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/integrations/NotesIntegration.java)

* A music mock cloud integration - providing music controls to the glasses
(through focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/integrations/MusicIntegration.java)

* A weather mock cloud integration - providing dummy weather info to the glasses
(through focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/integrations/CloudWeatherService.java)

* Registers two presentation integrations - one to stream some scrolling text and one to 
use a voice to text model to stream audio subtitles to the glasses
(through focals_buddy/app/src/main/java/com/openfocals/services/network/present/providers/StreamingTextPresentationProvider.java 
 and focals_buddy/app/src/main/java/com/openfocals/services/network/present/providers/AudioSubtitlePresentationProvider.java)

* Registers a service to monitor the bluetooth connection state

* Also starts a screen frame listener services (through a member - no explicit call) which, when
enabled, listens to image captures of the phone screen and forwards those on the event bus, in order 
to provide screen mirroring to the glasses/custom app.
(through focals_buddy/app/src/main/java/com/openfocals/services/screenmirror/ScreenFrameListener.java)


## Network Service

The network service monitors the event bus for bluetooth messages from the glasses related to 
network/socket requests.  Internally this has two subcomponents - a NetworkSocketManager (which 
handles actual external network connections) and an InterceptedNetworkServiceManager (which 
handles mocked network connections that will actually be responded to by the app pretending to 
be an external server).  

For exach request from the glasses, we first check if the InterceptedNetworkServiceManager wants
to handle the request, then if it doesn't, we pass the request on to the NetworkSocketManager.


The following commands may come in from the glasses:

* HostWhois - requesting an ip address for a given host.  To facilitate intercepting network 
traffic, the InterceptedNetworkServiceManager may provide manual internal ip addresses which it 
tracks for a subset of domains - so that when it seems further requests to these addresses, it 
knows it should handle them.

* SocketOpen - a request to open a new socket connection to a given ip

* SocketClose - a request to close an existing socket connection

* SocketError - a request to reset a socket connection

* SocketData - a request to send some data on the socket connection


Each open socket connection is labelled with an incrementing id so that the glasses can refer to 
an open connection by id.

The NetworkSocketManager is relatively straightforward - managing a set of external socket 
connections to remote sites and feeding data back and forth to the glasses.

The InterceptedNetworkServiceManager is more interesting and tracks:
* domain name to ip remappings, for serving fake or remapped whois information to the glasses

* created/open "internal" intercepted socket connections - basically socket connections the glasses
requested that we open and are being handled internally by some subcomponent.  The base class for
this is InterceptedNetworkServiceManager.InterceptedNetworkSession - and other components implement
their own custom sessions by overriding the onOpen, onData, onError, and onClose methods of these.

* ip address to InterceptedNetworkSessionFactory mappings - mapping which subcomponent should 
handle creating InterceptedNetworkSessions for different ip addresses.

The InterceptedNetworkServiceManager doesn't register any default services - but provides methods
to register new domain remappings (registerRemapping) and new services (registerServiceForIP and 
registerServiceForDomain)


### Presentation intercept
Probably the simplest service is the PresentationInterceptService 
(focals_buddy/app/src/main/java/com/openfocals/services/network/present/PresentationInterceptService.java)

This provides an InterceptedNetworkSessionFactory which creates sessions for new connections destined
for a presentation site (something on herokuapp).  These sessions do the following:
* Wait for an initial http request coming from the glasses - this http request is expected to be
a request to switch to a websocket protocol.  The request also specifies the code that was typed
in the glasses (I think it's a 3 character code each comprised of A-F)

* Once that request is received, sends back the necessary data to tell the glasses they're now 
communicating through the websocket protocol.  It also looks up what sub providers we have registered
for the provided code and instantiates one of them (provided we have one) - which will then handle
providing the actual slide/text content going forward

* Send an initial json message through the websocket indicating the glasses are connected

* Handle incoming json commands from the glasses to go to the next or previous slide

* Handle feeding data from the chosen sub provider to the glasses

Each subprovider inherits from PresentationProvider 
(/focals_buddy/app/src/main/java/com/openfocals/services/network/present/PresentationProvider.java)
and can implement the onNext, onPrevious (which are called when the glasses request a new slide),
resetPresentation() which is called on initial startup of a connection, and onClose when the 
connection is closed.  Additionally the subprovider may call sendCard(string_text) at any time, 
possibly from another thread, which can allow you to stream changing text to the glasses screen
rather than having to wait for a next/previous slide command.

### Cloud mock
The cloud mock service (focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/CloudMockService.java)
is a bit more complicated.  It intercepts https requests to the registered cloud address (cloud.ofocals.com),
handles decryption/encryption of the ssl connection, and provides an http endpoint handler 
to handle different web requests (For example get /v1/device/companions).  

Similar to the presentation intercept, the cloud mock service implements an InterceptedNetworkSessionFactory
which can create new socket sessions (CloudMockService.InterceptedCloudSSLSession).

Internal to each session is an instance of SSLServerDataHandler 
(focals_buddy/app/src/main/java/com/openfocals/commutils/ssl/SSLServerDataHandler.java) which 
handles the initial ssl handshake, decrypts incoming data and encrypts outgoing data.  There is also
an HTTPEndpointHandler session - which handles the plaintext http requests after decryption.

All data received from the glasses is fed to this ssl handler, then the resulting output data 
is fed to the http request handler.

The CloudMockService registers several default http endpoints which the glasses expects:
* /v1/device/companions which is and endpoint meant for the glasses to register that they're active

* /v1/device which is meant to provide device information 

* /v1/messenger/user which provides user information for the messaging app

* /v1/feature-manager/user which provides a list of features available 

It also holds a handle to a CloudIntegrationHandler 
(focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/CloudIntegrationHandler.java)
which manages further registration of cloud integrations (for example notes, tasks, etc).  










