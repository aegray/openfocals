#pragma once



#include <stdlib.h>
#include "utils/RefBase.h"
//#include "utils/Log.h"
//#include "utils/TextOutput.h"
#include <binder/IInterface.h>
#include <binder/IBinder.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <binder/IPCThreadState.h>

#include <QObject>
#include <QTimer>


class Wakelock : public QObject
{
    Q_OBJECT
public:
    explicit Wakelock(QObject *parent = 0);
    Q_INVOKABLE void set_enabled(bool enabled);
    Q_INVOKABLE bool get_enabled() const;

public slots:
    void on_timer();

private:
    bool enabled_ = false; 
    QTimer timer_;
//    android::sp<android::IBinder> binder_;
};

