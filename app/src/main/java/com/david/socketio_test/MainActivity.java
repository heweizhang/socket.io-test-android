package com.david.socketio_test;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.david.socketio_test.callback.OnServerMsgCallbackListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button btn_link;
    private Button btn_send;
    private Button btn_disconet;
    private EditText et_msg;
    private TextView tv_showMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_link = (Button) findViewById(R.id.btn_link);
        btn_link.setOnClickListener(this);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        btn_disconet = (Button) findViewById(R.id.btn_disconet);
        btn_disconet.setOnClickListener(this);
        et_msg = (EditText) findViewById(R.id.et_msg);
        tv_showMsg = (TextView) findViewById(R.id.tv_showMsg);

        MessagePushService.setMsgListener(new OnServerMsgCallbackListener() {
            @Override
            public void onMsgCallBack(final String msg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_showMsg.setText(msg);
                    }
                });
            }
        });


    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.btn_link:
                Intent linkIntent = new Intent(this, MessagePushService.class);
                Bundle linkBundle = new Bundle();
                linkBundle.putInt("type", MessagePushService.TYPE_LINK);
                linkIntent.putExtras(linkBundle);
                startService(linkIntent);
                tv_showMsg.setText("正在建立socket连接");
                break;
            case R.id.btn_send:
                Intent sendIntent = new Intent(this, MessagePushService.class);
                Bundle bundle = new Bundle();
                bundle.putInt("type", MessagePushService.TYPE_SEND);
                bundle.putString("msg", et_msg.getText().toString());
                sendIntent.putExtras(bundle);
                startService(sendIntent);
                break;
            case R.id.btn_disconet:
                Intent diconnectIntent = new Intent(this, MessagePushService.class);
                Bundle diconnectBundle= new Bundle();
                diconnectBundle.putInt("type", MessagePushService.TYPE_DISCONNET);
                diconnectIntent.putExtras(diconnectBundle);
                startService(diconnectIntent);
                break;

        }
    }

}