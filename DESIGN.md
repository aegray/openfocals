
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
tell the glasses to use a different address than the default one for North's cloud service 
- ofocals.com (a domain I own in order to get an ssl certificate issued) - and uses the 
certificate I have for this domain in order to serve cloud requests from the glasses.  

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
(through focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/CloudMockService.java)

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







