package com.openfocals.services.network.present;


public class PresentationProvider {
    private IPresenter presenter_;
    // don't override
    public void setPresenter(IPresenter p) {
        presenter_ = p;
    }

    // to be called by subclasses
    protected void sendCard(String data) {
        if (presenter_ != null) {
            presenter_.sendCard(data);
        }
    }


    // to be overridden
    public void onNext() { }
    public void onPrevious() { }
    public void resetPresentation() { }
    public void onClose() { }
}
