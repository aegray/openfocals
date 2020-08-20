package com.openfocals.services.files;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.openfocals.focals.Device;
import com.openfocals.focals.events.FocalsBluetoothMessageEvent;
import com.openfocals.focals.events.FocalsConnectedEvent;
import com.openfocals.focals.events.FocalsDisconnectedEvent;
import com.openfocals.focals.messages.FileDataRequest;
import com.openfocals.focals.messages.FileTransferRequest;
import com.openfocals.focals.messages.FileTransferStatus;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import okio.Buffer;

public class FileTransferService {
    private static final String TAG = "FOCALS_FILES";


    static FileTransferService instance_;

    public static FileTransferService getInstance() { return instance_; }


    class FileInfo {
        String id;
        String path;

        public FileInfo(String id, String path) {
            this.id = id;
            this.path = path;
        }
    }

    class TransferInfo {
        File file;
        FileInputStream stream;

        public TransferInfo(File f, FileInputStream i) {
            file = f;
            stream = i;
        }
    }

    Device device_;

    Context context_;

    HashMap<String, FileInfo> files_ = new HashMap<>();

    HashMap<String, TransferInfo> transfers_ = new HashMap<>();


    public FileTransferService(Context c, Device d) {
        instance_ = this;
        device_ = d;
        context_ = c;

        device_.getEventBus().register(this);

        files_.put("icon", new FileInfo("icon", context_.getExternalFilesDir(null) + "/icon_edit.png"));

        //FileOutputStream fos = new FileOutputStream(new File(context_.getExternalFilesDir(null) + "/icon_edit.png"));
    }

    void addFile(String id, String path) {
        files_.put(id, new FileInfo(id, path));
    }

    private static long calculateChecksum(InputStream inputStream) throws IOException {
        Checksum csum = new CRC32();
        byte[] bArr = new byte[32768];
        while (true) {
            int read = inputStream.read(bArr);
            if (read <= 0) {
                return csum.getValue();
            }
            csum.update(bArr, 0, read);
        }
    }

    class FinishedUpdateDownload
    {
        public String message;
        public FinishedUpdateDownload(String msg) { message = msg; }
    };

    public void downloadUpdateFile(final String url){
        Log.e(TAG, "Doing download from " + url);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL u = new URL(url);
                    InputStream is = u.openStream();
                    DataInputStream dis = new DataInputStream(is);

                    byte[] buffer = new byte[1024];
                    int length;

                    FileOutputStream fos = new FileOutputStream(new File(context_.getExternalFilesDir(null) + "/update.zip"));
                    while ((length = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }

                    device_.getEventBus().post(new FinishedUpdateDownload("Finished update download"));
                    return;
                } catch (MalformedURLException mue) {
                    Log.e(TAG, "Malformed url in file download: " + mue.toString());
                } catch (IOException ioe) {
                    Log.e(TAG, "IOException in file download: " + ioe.toString());
                } catch (SecurityException se) {
                    Log.e(TAG, "SecurityException in file download: " + se.toString());
                }
                device_.getEventBus().post(new FinishedUpdateDownload("Error downloading update"));
            }


        }).start();
     //   new Thread(new Runnable() {
     //       @Override
     //       public void run() {
     //           try {
     //               URL u = new URL("http://192.168.1.6:8000/icon_edit.png");
     //               InputStream is = u.openStream();
     //               DataInputStream dis = new DataInputStream(is);

     //               byte[] buffer = new byte[1024];
     //               int length;

     //               FileOutputStream fos = new FileOutputStream(new File(context_.getExternalFilesDir(null) + "/icon_edit.png"));
     //               while ((length = dis.read(buffer)) > 0) {
     //                   fos.write(buffer, 0, length);
     //               }

     //               device_.getEventBus().post(new FinishedUpdateDownload("Finished update download"));
     //               return;
     //           } catch (MalformedURLException mue) {
     //               Log.e(TAG, "Malformed url in file download: " + mue.toString());
     //           } catch (IOException ioe) {
     //               Log.e(TAG, "IOException in file download: " + ioe.toString());
     //           } catch (SecurityException se) {
     //               Log.e(TAG, "SecurityException in file download: " + se.toString());
     //           }
     //           device_.getEventBus().post(new FinishedUpdateDownload("Error downloading update"));
     //       }


     //   }).start();
    }

    public void sendUpdateToFocals() {
        String path = context_.getExternalFilesDir(null) + "/update.zip";
        File f = new File(path);
        if (f.exists()) {
            Toast.makeText(context_, "Sending update", Toast.LENGTH_LONG).show();
            addFile("update", path);

            device_.sendSoftwareUpdateStart("update", "1.119.0-4672", null);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFinishedUpdateDownload(FinishedUpdateDownload e) {
        Toast.makeText(context_, e.message, Toast.LENGTH_LONG).show();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsConnected(FocalsConnectedEvent e) {

    }

    private void sendStartError(String id) {
        device_.sendFileTransferStartResponse(id, FileTransferStatus.FileTransferStatus_ERROR, null, null);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsMessage(FocalsBluetoothMessageEvent e) {
        if (e.message.hasFileTransfer()) {
            Log.i(TAG, "Got file transfer message: " + e.message.toString());
            FileTransferRequest r = e.message.getFileTransfer();
            if (r.hasStartFileTransfer()) {
                String id = r.getStartFileTransfer().getId();

                try {
                    FileInfo f = files_.get(id);

                    if (f == null) {
                        sendStartError(id);
                    } else {
                        File fo = new File(f.path);
                        FileInputStream is = new FileInputStream(fo);
                        long len = fo.length();
                        long csum = calculateChecksum(is);
                        device_.sendFileTransferStartResponse(id, FileTransferStatus.FileTransferStatus_OK,
                                Long.valueOf(len), Long.valueOf(csum));

                        transfers_.put(id, new TransferInfo(fo, is));
                    }
                } catch (Exception ex) {
                    sendStartError(id);
                }

            } else if (r.hasStopFileTransfer()) {
                String id = r.getStopFileTransfer().getId();
                closeTransfer(id);
            } else if (r.hasFileData()) {
                FileDataRequest r2 = r.getFileData();
                String id = r2.getId();
                TransferInfo ti = transfers_.get(r2.getId());
                if (ti == null) {
                    device_.sendFileData(id, FileTransferStatus.FileTransferStatus_ERROR, null, null, null);
                } else {
                    // read
                    FileChannel channel = ti.stream.getChannel();

                    try {
                        long size = channel.size();
                        if (r2.getOffset() < size) {
                            channel.position(r2.getOffset());
                            Buffer buffer = new Buffer();
                            try {
                                buffer.readFrom(ti.stream, r2.getLength());
                            } catch (EOFException unused) {
                            }

                            device_.sendFileData(id, FileTransferStatus.FileTransferStatus_OK, Long.valueOf(r2.getOffset()), null, buffer);

                        } else {
                            device_.sendFileData(id, FileTransferStatus.FileTransferStatus_INVALID, Long.valueOf(r2.getOffset()), null, null);
                        }
                    } catch (Exception ex) {
                        device_.sendFileData(id, FileTransferStatus.FileTransferStatus_ERROR, Long.valueOf(r2.getOffset()), null, null);
                    }
                }
            }
        }
    }


    private void closeTransfer(String id) {
        TransferInfo ti = transfers_.remove(id);
        if (ti != null) {
            try {
                ti.stream.close();
            } catch (IOException ex) {
                // fine if it fails
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFocalsDisconnected(FocalsDisconnectedEvent e) {
        for (String i : transfers_.keySet()) {
            closeTransfer(i);
        }
    }


}
