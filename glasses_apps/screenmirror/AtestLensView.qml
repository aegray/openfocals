import QtQuick 2.0
import QtWebSockets 1.1
import QtQuick.Controls 1.2
import ImageStreamHandler 1.0
import Wakelock 1.0


Rectangle {
    id: app
    width: 220 
    height: 220
    focus: true

    color: "#00000000"

    Wakelock {
        id: keepscreenon
    }

    property bool try_reconnect: false

	Timer {
		id: timer
		function setTimeout(cb, delayTime) {
			timer.interval = delayTime;
			timer.repeat = false;
			timer.triggered.connect(cb);
			timer.triggered.connect(function release () {
				timer.triggered.disconnect(cb); // This is important
				timer.triggered.disconnect(release); // This is important as well
			});
			timer.start();
		}
	}



    WebSocket {
        id: socket
        url: "ws://screenmirror.ofocals.com:59423"
        onTextMessageReceived: {
            //console.log("Got frame")
            socket.sendTextMessage("1")
            img.add_frame(message)

            // send next frame
            //socket.sendTextMessage("{ \"msg\" : \"ready\" }")
            //{ \"msg\" : \"ready\" }")
        }
        onStatusChanged: {
            if (socket.status == WebSocket.Error) {
                //socket.active=false;
	            //timer.setTimeout(function(){ console.log("Trying reconnect"); socket.active=true; }, 5000);
                console.log("Websocket Error: " + socket.errorString)
            } else if (socket.status == WebSocket.Open) {
                // ready
                //socket.sendTextMessage("Hello World")
                console.log("Websocket open")
                //socket.sendTextMessage("{ \"msg\" : \"ready\" }")
                socket.sendTextMessage("1")
                //test\n")
                //{ \"msg\" : \"ready\" }")
            } else if (socket.status == WebSocket.Closed) {
                //socket.active=false;

                if (try_reconnect)
                {
	                timer.setTimeout(function(){ if (try_reconnect) { console.log("Trying reconnect"); socket.active=true; } }, 1000);
                }

                console.log("Websocket Closed")
                //messageBox.text += "\nSocket closed"
            }
        }
        active: false
    }

    Keys.onSelectPressed: {
        try_reconnect = !try_reconnect

        //var b = !socket.active;
        var b = try_reconnect

        //if (!b)
        //{
        //    img.clear();
        //}

        keepscreenon.set_enabled(b)
        console.log("Setting enabled: " + b)
        socket.active = b;
    }

    Keys.onUpPressed: {
        if (socket.active) {
            socket.sendTextMessage("u");
        }
    }
    Keys.onDownPressed: {
        if (socket.active) {
            socket.sendTextMessage("d");
        }
    }
    Keys.onLeftPressed: {
        if (try_reconnect) {
            if (socket.active) {
                socket.sendTextMessage("l");
            }
        } else {
            event.accepted = false
        }
    }
    Keys.onRightPressed: {
        if (try_reconnect) {
            if (socket.active) {
                socket.sendTextMessage("r");
            }
        } else {
            event.accepted = false
        }
    }

    ImageStreamHandler {
        anchors.fill: parent
        id: img

        visible: try_reconnect
        //socket.active
    }
    
    Text {
        id: infotext
        anchors.fill: parent
        text: "Click to enable"
        visible: !try_reconnect
        //socket.active
        font.pointSize: 24
        color: "red"
    }


    Component.onCompleted: {
        console.log("Starting websocket")
        //try_reconnect = true;
        //keepscreenon.set_enabled(true);
        //socket.active = true
    }
}

