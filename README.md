# openfocals

Replacement Android app for Focals smart glasses (originally by North)


# Setup

I generally keep the latest build at http://www.aegray.com/openfocals/openfocals.apk - you can 
download this to your phone and install it manually.

You'll have to disconnect your glasses in the North app before using this - by going to Menu->Device Information and clicking Disconnect

After setup, open the app, approve its permission requests (see the note on this next), and click
connect to connect to your focals.

Once the glasses are connected, you should generally click in the top left to open the drawer
and select "Features".  To fully demo all the parts of the app, I generally click 
"enable hidden features", then ensure the following are enabled:

* Android Notification Actions
* Notes (to demo providing custom notes)
* Spotify (to enable controlling phone audio playback and volume from the glasses)
* Tasks (to demo providing custom tasks)
* Focals connect (to demo providing streaming text services through the speaker notes)
* Weather view (to demo providing weather data)


One note - the app requests permission to record audio solely because I included a demo of 
using a speech recognition model to display speech to text results recorded on your phone 
through the presentation provider.  This by default will not work even if you enable permissions, 
because it requires manually downloading some large model files - feel free to deny this permission.


# Notes / Compatability

* Music/media control only works on sdk 26 (Android 8.0 / Oreo) or above.

* In one case on an older api version (24 / Android 7.0), logging into alexa results in a 
blank screen from amazon's login library


# Building 

For building, I've taken out my amazon api key (used for authenticating alexa), ssl 
certificates and private keys (used for intercepting cloud traffic), and a keystore file, 
which means by default some features will not work if you build yourself.

I'll add build directions and information soon on how to provide this for yourself...


# Todo list

Feature todo list:

[x] Network support

[x] Network intercept based services

[x] SSL support for cloud intercept

[x] Feature manager

[x] Moving device control to service

[x] Notifications (no icons, no separate sms)

[x] Pairing / connection ui (rather than using a fixed bluetooth address)

[x] Calibration ui

[x] Screencasting (mainly for debug use) - works to a server

[x] Alexa / amazon (left my api key out of the repo though)

[x] Custom projector apps - just streaming text as an example right now, but api is there

[x] Music control (no media info, but playback control over phone media + volume control)

[x] Custom app handler (requires installing software to glasses)

[x] File transfer to focals (for icons, media)

[-] Weather (mocked up - returning fixed values, no integration with external services yet)

[-] Todos (api is there with mocked data, no integration with external services yet)

[-] Notes (api is there with mocked data, no integration with external services yet)

[ ] Control fragment for projector apps

[ ] Experience launcher

[ ] Onboarding + permissions ui (mainly for notification services)

[ ] Settings

[ ] Sms + messaging, separate from notifications

[ ] Icon support for notifications

[ ] Nicer/clean up ui




Not currently planning to implement, but open to requests (I can also provide info on expected messages):

[ ] Trivia

[ ] Sports scores

[ ] Google health

[ ] Calendar

[ ] Uber

[ ] Navigation / eta / location

[ ] Local location information

[ ] Screenshotting focals view

[ ] Screencasting to the phone (does anyone care?)




Still researching how to get apps onto the glasses without requiring a connector cable and jumping through a lot of hoops - if anyone with intimate android/aosp knowledge or arm based reverse engineering experience (especially c++) wants to help out, please reach out. 



# Providing your own content:


## Presentation Provider

This is probably the simplest method.  This will allow you to show your own slides in the 
teleprompter / chrome slides app on the glasses.  To do this, see as an example:

focals_buddy/app/src/main/java/com/openfocals/services/network/present/providers/StreamingTextPresentationProvider.java

You'll need to add a class which extends PresentationProvider and implements one or more of the following methods:

* resetPresentation() - called when the presentation is opened.  Here is where you should reset + initialize content

* onNext() - this is called when you right click the loop 

* onPrevious() - this is called when you left click the loop

* onClose() - this is called when the teleprompter app is disconnected 


There is one call to send data to the glasses, and you can call this at any time after 
resetPresentation is called and before onClose is called:

* sendCard(String data) - this will push the given string to the display 


Finally, to register your provider, in 

focals_buddy/app/src/main/java/com/openfocals/services/DeviceService.java

in onCreate, you need to call:

presentation.registerPresentationProvider(CODETOUSE, new YourClassNameHere());

where CODETOUSE is a string "AAA" - "EEE" that corresponds to the code you enter in the teleprompter
app on the glasses.  This will select which code will open your presenter class.



## Notes + Tasks

For notes, see:

focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/integrations/NotesIntegration.java

There is a function addNote - see an example usage in the constructor.  This should be enough 
to add code to add your own notes to be displayed on the glasses.

Make sure that you have enabled notes on the features section of the app in order to see them.



Tasks:

Similarly, for tasks, see:

focals_buddy/app/src/main/java/com/openfocals/services/network/cloudintercept/integrations/TasksIntegration.java

And see the constructor for an example of how to add projects and tasks programatically. 

Both of these are only wired up to test data currently.

Similar to the above - make sure to enable tasks in the feature section of the app.



