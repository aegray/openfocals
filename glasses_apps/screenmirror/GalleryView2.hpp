#pragma once

#include "GalleryView.hpp"

namespace thalmic{

//class GalleryView2 : public QQuickView
//{
//    //Q_OBJECT
//public:
//    //GalleryView(const QString name);
//    explicit GalleryView2(QString name);
//    //GalleryView(QString &name);
////    virtual ~GalleryView();
////protected:
////    virtual void exposeEvent(QExposeEvent* event) override;
////    // lenses should remain permanently setup for performance
////    virtual void setupScene() {}
////    virtual void teardownScene() {}
//};
//
//

class GalleryView2 : public GalleryView
{
    Q_OBJECT
public:
    explicit GalleryView2(QString name);
    ~GalleryView2();
protected:
    virtual void exposeEvent(QExposeEvent* event) override;
    // lenses should remain permanently setup for performance
    virtual void setupScene() {}
    virtual void teardownScene() {}
};

}

