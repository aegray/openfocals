#include "AtestLensArtist.hpp"

#include <QQmlContext>

namespace {
constexpr auto atestLensArtistView = "qrc:/atest/AtestLensView.qml";
} // namespace

AtestLensArtist::AtestLensArtist(QString name)
: QQuickView() //thalmic::GalleryView{name}
{
    // implies GalleryView inherits from QQuickView -> 
    setSource(QUrl{atestLensArtistView});
}

//AtestLensArtist::~AtestLensArtist() { }
//= default;
//
//void AtestLensArtist::exposeEvent(QExposeEvent* event)
//{
//    //if (isExposed()) {
//    //    emit exposed();
//    //}
//
//    //thalmic::GalleryView::exposeEvent(event);
//}
//

