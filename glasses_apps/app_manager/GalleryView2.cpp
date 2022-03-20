#include "GalleryView2.hpp"


namespace thalmic {
    GalleryView2::GalleryView2(QString name) : GalleryView(name) { }
    GalleryView2::~GalleryView2() { }

    void GalleryView2::exposeEvent(QExposeEvent* event) 
    { 
        GalleryView::exposeEvent(event);
    }

}
