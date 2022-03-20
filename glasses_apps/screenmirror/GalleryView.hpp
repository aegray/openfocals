#pragma once
#include <QQuickView>
#include <QObject>
//#include <QQuickView>
//#include <QQuickWindow>

namespace thalmic{

class GalleryView : public QQuickView
{

public:
    //Q_OBJECT
    //QT_WARNING_PUSH 
    // Q_OBJECT_NO_OVERRIDE_WARNING 
    static const QMetaObject staticMetaObject;
    virtual const QMetaObject *metaObject() const;
    virtual void *qt_metacast(const char *);
    virtual int qt_metacall(QMetaObject::Call, int, void **);
    //QT_TR_FUNCTIONS 


public:
    //GalleryView(const QString name);
    explicit GalleryView(QString name);
    //GalleryView(QString &name);
    virtual ~GalleryView(); // = 0;
protected:
    virtual void exposeEvent(QExposeEvent* event) override;
    // lenses should remain permanently setup for performance
    virtual void setupScene() {}
    virtual void teardownScene() {}

private:
    //Q_OBJECT_NO_ATTRIBUTES_WARNING 
    //Q_DECL_HIDDEN_STATIC_METACALL static void qt_static_metacall(QObject *, QMetaObject::Call, int, void **); 
    static void qt_static_metacall(QObject *, QMetaObject::Call, int, void **); 
    //QT_WARNING_POP 
    struct QPrivateSignal {}; 
    //QT_ANNOTATE_CLASS(qt_qobject, "")
};


//
//class GalleryView2 : public GalleryView
//{
//    Q_OBJECT
//public:
//    GalleryView2(QString name) : GalleryView(name) { }
//};

}

