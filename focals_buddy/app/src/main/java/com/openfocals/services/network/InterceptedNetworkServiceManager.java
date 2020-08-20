package com.openfocals.services.network;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import okio.Buffer;

public class InterceptedNetworkServiceManager {

    ISocketManagerListener listener_;

    public static class InterceptedNetworkSession {
        private InterceptedNetworkServiceManager parent_;
        private int sock_id_;

        private void setupSession(int sock_id, InterceptedNetworkServiceManager p) {
            sock_id_ = sock_id;
            parent_ = p;
        }

        // two controls allowed to subclasses
        protected void sendData(Buffer b) {
            parent_.sendDataToFocals(sock_id_, b);
        }

        protected void sendData(String s) {
            Buffer b = new Buffer();
            b.writeUtf8(s);
            sendData(b);
        }

        public void close() { parent_.socketError(104); }


        // callbacks
        public void onOpen() {

        }

        public void onData(Buffer b) {

        }

        public void onError() {
            // same as on close
        }

        public void onClose() {

        }
    }

    public interface InterceptedNetworkSessionFactory {
        public abstract InterceptedNetworkSession createSession() throws Exception;
    }


    // Socket id -> Session
    final Map<Integer, InterceptedNetworkSession> sessions_ = new HashMap();

    // Hostname -> remapped ip address
    final Map<String, String> domain_to_ip_ = new HashMap<>();

    // Remapped ip address -> session factory
    final Map<String, InterceptedNetworkSessionFactory> ip_to_factory_ = new HashMap();

    // bottom octet for now
    int next_ip_ = 0;


    public void setListener(ISocketManagerListener l) {
        listener_ = l;
    }


    // from session out to glasses
    private void sendDataToFocals(int id, Buffer data) {
        listener_.onSocketData(id, data);
    }




    //// registration interface

    public String allocateInternalIp() {
        next_ip_ += 1;
        if (next_ip_ > 250) {
            throw new RuntimeException("Allocated too many internal ips - the limit is 250");
        }
        return "0.0.0." + next_ip_;
    }


    public void registerRemapping(String domain, String ip) {
        domain_to_ip_.put(domain, ip);
    }

    public void registerServiceForIP(String ip, InterceptedNetworkSessionFactory sessionFactory) {
        ip_to_factory_.put(ip, sessionFactory);
    }

    public void registerServiceForDomain(String domain, InterceptedNetworkSessionFactory sessionFactory) {
        String ip = allocateInternalIp();
        domain_to_ip_.put(domain, ip);
        registerServiceForIP(ip, sessionFactory);
    }




    // from network service to us
    public boolean handlesHost(String host) {
        return ip_to_factory_.containsKey(host);
    }

    public boolean handles(int id) {
        return sessions_.containsKey(Integer.valueOf(id));
    }

    public String getHostWhois(String host) {
        return domain_to_ip_.get(host);
    }


    // from focals to session
    public void openSocket(int id, String host, int port) {
        InterceptedNetworkSessionFactory res = ip_to_factory_.get(host);
        // this should only be called when there's already been a valid remapping
        if (res == null)
        {
            throw new RuntimeException("Internal bug - attempted to open a socket for unmapped internal id");
        }

        InterceptedNetworkSession session;
        try {
            session = res.createSession();
        } catch (Exception e)
        {
            throw new RuntimeException("Could not create session");
        }
        session.setupSession(id, this);
        sessions_.put(Integer.valueOf(id), session);
        session.onOpen();
        listener_.onSocketOpenResult(id, true, 0);
    }

    public void closeSocket(int id) {
        Integer iid = Integer.valueOf(id);
        InterceptedNetworkSession res = sessions_.get(iid);
        if (res != null)
        {
            res.onClose();
            sessions_.remove(iid);
            listener_.onSocketCloseResult(id, true, 0);
        }
    }

    public void socketData(int id, Buffer b) {
        InterceptedNetworkSession res = sessions_.get(Integer.valueOf(id));
        if (res != null)
        {
            res.onData(b);
        }
    }

    public void socketError(int id) {
        Integer iid = Integer.valueOf(id);
        InterceptedNetworkSession res = sessions_.get(iid);
        if (res != null)
        {
            res.onError();
            sessions_.remove(iid);
            listener_.onSocketCloseResult(id, true, 0);
        }
    }
}


