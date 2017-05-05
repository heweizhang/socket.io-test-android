package com.david.socketio_test;

/**
 * Created by david on 17/5/5.
 */

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.david.socketio_test.callback.OnServerMsgCallbackListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MessagePushService extends Service {

    public static final int TYPE_LINK = 100;
    public static final int TYPE_SEND = 200;
    public static final int TYPE_DISCONNET = 300;

    private static final String TAG = MessagePushService.class.getSimpleName();

    private Socket mSocket;

    private boolean isConnected;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        initSocketHttp();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        Bundle bundle = intent.getExtras();
        int type = bundle.getInt("type", 0x00);
        Log.e(TAG, "type= " + type);
        switch (type) {
            case TYPE_LINK:
                connectSocket();
                break;

            case TYPE_SEND:
                sendMsg(bundle.getString("msg"));
                break;

            case TYPE_DISCONNET:
                if (isConnected) {
                    disConnectSocket();
                }
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 使用socket 发送消息
     */
    private void sendMsg(String msg) {
        try {

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("fromClient", msg);
            mSocket.emit("clientSendMsg", jsonObject); //向服务器发起消息
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "clientSendMsg:" + msg);
    }


    /**
     * 初始化Socket,Https的连接方式
     */
    private void initSocketHttps() {
        SSLContext sc = null;
        TrustManager[] trustCerts = new TrustManager[]{new X509TrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {

            }
        }};
        try {
            sc = SSLContext.getInstance("TLS");
            sc.init(null, trustCerts, null);
            IO.Options opts = new IO.Options();
            opts.sslContext = sc;
            opts.hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            };
            mSocket = IO.socket("https://guarded-river-41531.herokuapp.com", opts);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化Socket,Http的连接方式
     */
    private void initSocketHttp() {
        try {
            mSocket = IO.socket("http://guarded-river-41531.herokuapp.com"); // 初始化Socket
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void connectSocket() {

        mSocket.connect();

        mSocket.on(Socket.EVENT_CONNECT, onConnect);// 连接成功
        mSocket.on("connect_result", connected);// 连接成功
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);// 断开连接
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);// 连接异常
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectTimeoutError);// 连接超时
        mSocket.on("serverGetMsg", onConnectMsg);// 监听消息事件回调
        mSocket.on("groupMsg", onGroupConnectMsg);// 监听群发消息事件回调

        Log.e(TAG, "connectSocket");
    }

    private void disConnectSocket() {
        mSocket.disconnect();

        mSocket.off(Socket.EVENT_CONNECT, onConnect);// 连接成功
        mSocket.on("connect_result", connected);// 连接成功
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);// 断开连接
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);// 连接异常
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectTimeoutError);// 连接超时
        mSocket.off("serverGetMsg", onConnectMsg);// 监听消息事件回调
        mSocket.off("groupMsg", onGroupConnectMsg);// 监听群发消息事件回调
    }

    private Emitter.Listener onConnectMsg = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            // 在这里处理你的消息
            Log.e(TAG, "服务器返回来的消息 : " + args[0]);
            if (msgListener != null) {
                msgListener.onMsgCallBack("clientSendMsg：" + args[0]);
            }
        }
    };
    private Emitter.Listener onGroupConnectMsg = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            // 在这里处理你的消息
            Log.e(TAG, "服务器返回来的群发消息 : " + args[0]);
            if (msgListener != null) {
                msgListener.onMsgCallBack("groupMsg：" + args[0]);
            }
        }
    };

    /**
     * 实现消息回调接口
     */
    private Emitter.Listener connected = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.e(TAG, "连接成功 " + args[0]);
            isConnected = true;
            if (msgListener != null) {
                msgListener.onMsgCallBack("连接成功：" + args[0]);
            }
        }
    };


    /**
     * 实现消息回调接口
     */
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.e(TAG, "连接成功 ");

        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e(TAG, "断开连接 " + args[0]);
            isConnected = false;
            if (msgListener != null) {
                msgListener.onMsgCallBack("断开连接：" + args[0]);
            }
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.e(TAG, "连接 失败" + args[0]);
        }
    };

    private Emitter.Listener onConnectTimeoutError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.e(TAG, "连接 超时" + args[0]);

        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static OnServerMsgCallbackListener msgListener;

    public static void setMsgListener(OnServerMsgCallbackListener listener) {
        msgListener = listener;
    }


}