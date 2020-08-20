// "THE BEER-WARE LICENSE" (Revision 42):
// Martin Bříza <m@rtinbriza.cz> wrote this file.
// As long as you retain this notice you can do whatever you want
// with this stuff.
// If we meet some day, and you think this stuff is worth it,
// you can buy me a beer in return.

import QtQuick 2.0

Rectangle {
    id: app
    width: 220; height: 220
    color: "#888888"
    focus: true

    property variant numbers: []
    property int cols: 4
    property int rows: 4
    property int finalValue: 2048
    property int score: 0
    property bool gameover: false

    function numberAt(col, row) {
        for (var i = 0; i < numbers.length; i++) {
            if (numbers[i].col == col && numbers[i].row == row)
                return numbers[i]
        }
    }
    function popNumberAt(col, row) {
        var tmp = numbers
        for (var i = 0; i < tmp.length; i++) {
            if (tmp[i].col == col && tmp[i].row == row) {
                tmp[i].destroy()
                tmp.splice(i, 1)
            }
        }
        numbers=tmp
    }
    function purge() {
        gameover = false;
        score = 0
        var tmp = numbers
        for (var i = 0; i < tmp.length; i++) {
            tmp[i].destroy()
        }
        tmp = Array()
        numbers = tmp
        gen2();
        gen2();
    }
    function checkNotStuck() {
        for (var i = 0; i < app.cols; i++) {
            for (var j = 0; j < app.rows; j++) {
                if (!numberAt(i, j))
                    return true
                if (numberAt(i+1,j) && numberAt(i,j).number == numberAt(i+1,j).number)
                    return true
                if (numberAt(i-1,j) && numberAt(i,j).number == numberAt(i-1,j).number)
                    return true
                if (numberAt(i,j+1) && numberAt(i,j).number == numberAt(i,j+1).number)
                    return true
                if (numberAt(i,j-1) && numberAt(i,j).number == numberAt(i,j-1).number)
                    return true
            }
        }
        return false
    }
    function victory() {
        app.gameover = true;
        message.show("You win. Finally.")
    }
    function defeat() {
        app.gameover = true;
        message.show("May I suggest some Burial?")
    }

    Component {
        id: number

        Rectangle {
            id: colorRect
            color: number <=    1 ? "transparent" :
                   number <=    2 ? "#eee4da" :
                   number <=    4 ? "#ede0c8" :
                   number <=    8 ? "#f2b179" :
                   number <=   16 ? "#f59563" :
                   number <=   32 ? "#f67c5f" :
                   number <=   64 ? "#f65e3b" :
                   number <=  128 ? "#edcf72" :
                   number <=  256 ? "#edcc61" :
                   number <=  512 ? "#edc850" :
                   number <= 1024 ? "#edc53f" :
                   number <= 2048 ? "#edc22e" :
                                    "#3c3a32"

            property int col
            property int row

            property int number: Math.random() > 0.9 ? 4 : 2

            x: cells.getAt(col, row).x
            y: cells.getAt(col, row).y
            width: cells.getAt(col, row).width
            height: cells.getAt(col, row).height
            radius: cells.getAt(col, row).radius

            function move(h, v) {
                if (h == col && v == row)
                    return false
                if (app.numberAt(h, v)) {
                    number += app.numberAt(h, v).number
                    app.score += number
                    if (number == finalValue)
                        app.victory()
                    app.popNumberAt(h, v)
                }
                col = h
                row = v
                return true
            }

            Text {
                id: text

                width: parent.width * 0.9
                height: parent.height * 0.9
                anchors.centerIn: parent

                font.family: "monospace"
                font.bold: true
                font.pixelSize: parent.height
                fontSizeMode: Text.Fit
                horizontalAlignment: Text.AlignHCenter
                verticalAlignment: Text.AlignVCenter

                text: parent.number > 1 ? parent.number : ""
            }

            Behavior on x {
                NumberAnimation {
                    duration: 50
                    easing {
                        type: Easing.InOutQuad
                    }
                }
            }
            Behavior on y {
                NumberAnimation {
                    duration: 50
                    easing {
                        type: Easing.InOutQuad
                    }
                }
            }

            transform: Scale {
                id: zoomIn
                origin.x: colorRect.width / 2
                origin.y: colorRect.height / 2
                xScale: 0
                yScale: 0
                Behavior on xScale {
                    NumberAnimation {
                        duration: 200
                        easing {
                            type: Easing.InOutQuad
                        }
                    }
                }
                Behavior on yScale {
                    NumberAnimation {
                        duration: 200
                        easing {
                            type: Easing.InOutQuad
                        }
                    }
                }
            }

            Component.onCompleted: {
                zoomIn.xScale = 1
                zoomIn.yScale = 1
            }
        }
    }

    Rectangle {
        anchors.centerIn: parent
        color: "transparent"
        height: parent.height / 32 * 31
        width: parent.width / 32 * 31
        Rectangle {
            id: scorePanelRect
            anchors.top: parent.top
            height: parent.height * 0.1
            width: parent.width
            opacity: 0.66
            color: "white"
            radius: 2
            z: 2
            Text {
                id: scorePanel
                width: parent.width / 32 * 31
                height: parent. height / 32 * 31
                anchors.centerIn: parent

                font.pixelSize: height * 0.5
                horizontalAlignment: Text.AlignRight
                verticalAlignment: Text.AlignVCenter

                text: "Score: " + app.score
            }
        }
        Grid {
            id: cellGrid
            width: parent.width
            height: (parent.height - scorePanel.height) / 32 * 31
            anchors.bottom: parent.bottom
            rows: app.rows
            columns: app.cols
            spacing: (parent.width + parent.height) / app.rows / app.cols / 4

            property real cellWidth: (width - (columns - 1) * spacing) / columns
            property real cellHeight: (height - (rows - 1) * spacing) / rows

            Repeater {
                id: cells
                model: app.cols * app.rows
                function getAt(h, v) {
                    return itemAt(h + v * app.cols)
                }
                function getRandom() {
                    return itemAt(Math.floor((Math.random() * 16)%16))
                }
                function getRandomFree() {
                    var free = new Array()
                    for (var i = 0; i < app.cols; i++) {
                        for (var j = 0; j < app.rows; j++) {
                            if (!numberAt(i, j)) {
                                free.push(getAt(i, j))
                            }
                        }
                    }
                    return free[Math.floor(Math.random()*free.length)]
                }
                Rectangle {
                    width: parent.cellWidth
                    height: parent.cellHeight
                    color: "#AAAAAA"
                    radius: 2

                    property int col : index % app.cols
                    property int row : index / app.cols
                }
            }
        }
        Rectangle {
            id: message
            width: app.width
            height: app.height
            anchors.centerIn: parent
            opacity: 0.66
            color: "black"
            visible: true
            z: 1
            function hide() {
                visible = false
                opacity = 0.0
                messageText.text = ""
            }
            function show(text) {
                visible = true
                opacity = scorePanelRect.opacity
                messageText.text = text
            }
            Rectangle {
                anchors.centerIn: parent
                width: parent.width * 0.66
                height: parent.height * 0.33
                color: "black"
                Rectangle {
                    anchors.fill: parent
                    width: parent.width - 2
                    height: parent.height -.2
                    color: "white"
                    Text {
                        anchors.fill: parent
                        id: messageText
                        text: "Start game"
                        font.pixelSize: parent.height * 0.13
                        anchors.centerIn: parent
                        horizontalAlignment: Text.AlignHCenter
                        verticalAlignment: Text.AlignVCenter
                    }
                }
            }
            Behavior on opacity { 
                NumberAnimation {
                    duration: 200
                }
            }
        }
    }


    MouseArea {
        anchors.fill: parent
        property int minimumLength: app.width < app.height ? app.width / 5 : app.height / 5
        property int startX
        property int startY
        onPressed: {
            startX = mouse.x
            startY = mouse.y
        }
        onReleased: {
            var length = Math.sqrt(Math.pow(mouse.x - startX, 2) + Math.pow(mouse.y - startY, 2))
            if (length < minimumLength)
                return
            var diffX = mouse.x - startX
            var diffY = mouse.y - startY
            // not sure what the exact angle is but it feels good
            if (Math.abs(Math.abs(diffX) - Math.abs(diffY)) < minimumLength / 2)
                return
            if (Math.abs(diffX) > Math.abs(diffY))
                if (diffX > 0)
                    app.move(1, 0)
                else
                    app.move(-1, 0)
            else
                if (diffY > 0)
                    app.move(0, 1)
                else
                    app.move(0, -1)
        }
    }
    function gen2() {
        var tmp = numbers
        var cell = cells.getRandomFree()
        var newNumber = number.createObject(cellGrid,{"col":cell.col,"row":cell.row})
        tmp.push(newNumber)
        numbers = tmp
    }
    // oh  my, this HAS TO be rewritten
    function move(col, row) {
        var somethingMoved = false
        var tmp = numbers
        if (col > 0) {
            for (var j = 0; j < app.rows; j++) {
                var filled = 0
                var canMerge = false
                for (var i = app.cols - 1; i >= 0; i--) {
                    if (numberAt(i,j)) {
                        if (canMerge) {
                            if (numberAt(i,j).number == numberAt(app.cols-filled,j).number) {
                                canMerge = false
                                filled--
                            }
                        }
                        else {
                            canMerge = true
                        }
                        if (numberAt(i,j).move(app.cols-1-filled,j))
                            somethingMoved = true
                        filled++
                    }
                }
            }
        }
        if (col < 0) {
            for (var j = 0; j < app.rows; j++) {
                var filled = 0
                var canMerge = false
                for (var i = 0; i < app.cols; i++) {
                    if (numberAt(i,j)) {
                        if (canMerge) {
                            if (numberAt(i,j).number == numberAt(filled-1,j).number) {
                                canMerge = false
                                filled--
                            }
                        }
                        else {
                            canMerge = true
                        }
                        if (numberAt(i,j).move(filled,j))
                            somethingMoved = true
                        filled++
                    }
                }
            }
        }
        if (row > 0) {
            for (var i = 0; i < app.cols; i++) {
                var filled = 0
                var canMerge = false
                for (var j = app.rows - 1; j >= 0; j--) {
                    if (numberAt(i,j)) {
                        if (canMerge) {
                            if (numberAt(i,j).number == numberAt(i,app.rows-filled).number) {
                                canMerge = false
                                filled--
                            }
                        }
                        else {
                            canMerge = true
                        }
                        if (numberAt(i,j).move(i,app.rows-1-filled))
                            somethingMoved = true
                        filled++
                    }
                }
            }
        }
        if (row < 0) {
            for (var i = 0; i < app.cols; i++) {
                var filled = 0
                var canMerge = false
                for (var j = 0; j < app.rows; j++) {
                    if (numberAt(i,j)) {
                        if (canMerge) {
                            if (numberAt(i,j).number == numberAt(i,filled-1).number) {
                                canMerge = false
                                filled--
                            }
                        }
                        else {
                            canMerge = true
                        }
                        if (numberAt(i,j).move(i,filled))
                            somethingMoved = true
                        filled++
                    }
                }
            }
        }
        if (somethingMoved)
            gen2()
        if (!checkNotStuck())
            app.defeat()


        return somethingMoved;
    }
    Component.onCompleted: {
        app.purge()
    }

    Keys.onLeftPressed: {
        if (!message.visible) {
            //event.accepted = app.move(-1, 0);
            app.move(-1, 0);
        } else {
            event.accepted = false;
        }
    }

    Keys.onRightPressed: {
        if (!message.visible) {
            //event.accepted = app.move(1, 0);
            app.move(1, 0);
        } else {
            event.accepted = false;
        }
    }
    

    Keys.onUpPressed: {
        if (!message.visible) {
            app.move(0, -1)
        } else if (!app.gameover) {
            message.hide()
        }
    }
    Keys.onDownPressed: {
        if (!message.visible) {
            app.move(0, 1)
        } else {
            app.purge()
            message.hide() 
        }
    }

    Keys.onSelectPressed: {
        if (!message.visible) {
           message.show("Paused")
        } else if (app.gameover) {
            app.purge()
            message.hide()
        }
    }

    //Keys.onPressed: {
    //    if (!message.visible) {
    //        //if (event.key == Qt.Key_Left)
    //        //    app.move(-1, 0)
    //        //if (event.key == Qt.Key_Right)
    //        //    app.move(1, 0)
    //        if (event.key == Qt.Key_Up)
    //            app.move(0, -1)
    //        if (event.key == Qt.Key_Down)
    //            app.move(0, 1)
    //    }
    //    if (event.key == Qt.Key_Select) {
    //        app.purge()
    //        message.hide()
    //    }
    //}
}
