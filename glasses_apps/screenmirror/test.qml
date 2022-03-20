import QtQuick 2.7
import QtGraphicalEffects 1.0

//import QtMultimedia 5.9

import BlackCoralStyle 1.0
import BlackCoralApp 1.0
import Direction 1.0

import LauncherApp 1.0


Item {
    id: root
    MediaPlayer {
        id: player
        source: "file://data/app/test.avi"
        autoPlay: false
    }

    VideoOutput {
        id: videoOutput
        source: player
        anchors.fill: parent
        focus: true
        Keys.onSelectPressed: video.playbackState == MediaPlayer.PlayingState ? video.pause() : video.play()
    }


//    Video {
//        id: video
//        anchor.fill: parent
//        source: "/data/app/test.avi"
//        
//        focus: true
//
//        Keys.onSelectPressed: video.playbackState == MediaPlayer.PlayingState ? video.pause() : video.play()
//    }
//    
//    //Launcher { id : mylauncher }
////
//    Component {
//        id: numberDelegate
//        Text {
//            text: "cmd: " + name
//            color: ListView.isCurrentItem?"orange":"blue"
//            font.pointSize: 18
//
//            Keys.onSelectPressed : {
//                console.log("Lenter: " + name);
//                mylauncher.launch(path)
//            }
//        }
//    }
//    ListModel {
//        id: commandsListModel
//        
//        ListElement { name : "killer"; path : "killall demo_app" }
//        ListElement { name : "cmd_and_control"; path : "/data/app/cmd_and_control.sh" }
//        ListElement { name : "log_dumper"; path : "/data/app/log_dumper.sh"  }
//    }
//
//    ListView {
//        id: lv1
//        anchors.fill : parent
//        clip: true
//        model: commandsListModel
//        delegate: numberDelegate
//        spacing: 5
//        focus: true
//    }
//
//

}

