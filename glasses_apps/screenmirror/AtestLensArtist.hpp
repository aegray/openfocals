#pragma once

//#include <black_coral_app/GalleryView.hpp>
#include "GalleryView.hpp"
//#include <thalmic_qt/QObjectDeleter.hpp>

class AtestLensArtist : public QQuickView { 
    //thalmic::GalleryView {
    Q_OBJECT
public:
    AtestLensArtist(QString name);
    //virtual ~AtestLensArtist() override;

//signals:
//    void exposed() const;
//
//private:
////    void exposeEvent(QExposeEvent* event) override;
//    // lenses should remain permanently setup for performance
//    //void setupScene() override {}
//    //void teardownScene() override {}
//    void setupScene() {}
//    void teardownScene() {}
};

