import QtQuick 2.0
import QtWebSockets 1.1
import QtQuick.Controls 1.2
import ImageStreamHandler 1.0


Rectangle {
    id: app
    width: 220 
    height: 220
    focus: true

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
            img.add_frame(message)
        }
        onStatusChanged: {
            if (socket.status == WebSocket.Error) {
                //socket.active=false;
	            //timer.setTimeout(function(){ console.log("Trying reconnect"); socket.active=true; }, 10000);
                console.log("Websocket Error: " + socket.errorString)
            } else if (socket.status == WebSocket.Open) {
                // ready
                //socket.sendTextMessage("Hello World")
                console.log("Websocket open")
            } else if (socket.status == WebSocket.Closed) {
                //socket.active=false;
	            //timer.setTimeout(function(){ console.log("Trying reconnect"); socket.active=true; }, 10000);
                console.log("Websocket Closed")
                //messageBox.text += "\nSocket closed"
            }
        }
        active: false
    }

    Keys.onSelectPressed: {
        var b = !socket.active;
        console.log("Setting enabled: " + b)
        socket.active = b;
    }

    ImageStreamHandler {
        anchors.fill: parent
        id: img
    }

    Component.onCompleted: {
        console.log("Starting websocket")
        socket.active = true
    }
}

