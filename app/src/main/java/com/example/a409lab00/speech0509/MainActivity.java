package com.example.a409lab00.speech0509;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private TextView mVoiceInputTv,mqttCon,deliveryText,MessageText,pubMessage;
    private Button mSpeakBtn;
    public static MqttAndroidClient client;

    public static String myTopic = "test";

    public static MqttConnectOptions options;
    public static String mqttHost = "tcp://192.168.49.119";//改為自己的MQTT SERVER IP

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVoiceInputTv = (TextView)findViewById(R.id.voiceInput);
        mSpeakBtn = (Button)findViewById(R.id.mSpeakBtn);
        mqttCon = (TextView)findViewById(R.id.mqttCon);
        deliveryText = (TextView)findViewById(R.id.deliveryText);
        MessageText = (TextView)findViewById(R.id.MessageText) ;
        pubMessage = (TextView)findViewById(R.id.pubMessage);

        pubMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPub("hello");
            }
        });

        mSpeakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceInput();
            }
        });

        try {
            client = new MqttAndroidClient(MainActivity.this, "tcp://192.168.49.119:1883",
                    "f");            options = new MqttConnectOptions();
//            options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
//            options.setCleanSession(true);
            options.setUserName("apple");//如果有帳號，可以這裡設定
            options.setPassword("abc".toCharArray());//如果有密碼，可以這裡設定
//            options.setConnectionTimeout(10);
//            options.setKeepAliveInterval(20);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // 這裡可以寫重連程式
                    mqttCon.setText("Connection Lost");
                }


                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    deliveryText.setText("發送完成 --" + token.isComplete());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // subscribe后得到的訊息如下
                    String s = "主题 : " + topic + "\n" + "Qos : " + message.getQos() + "\n" + "内容 : " + new String(message.getPayload());

                }
            });
            client.connect(options);


        }catch(MqttException me) {

            me.printStackTrace();
        }
    }
    public static void startPub(String m){

        try {


            MqttMessage message = new MqttMessage(m.getBytes());message.setQos(0);

            client.publish(myTopic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    private void startVoiceInput(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hello, How can I help you ?");
        try{
            startActivityForResult(intent,REQ_CODE_SPEECH_INPUT);
        }catch(ActivityNotFoundException a){
            String x="sds";
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        switch(requestCode){
            case REQ_CODE_SPEECH_INPUT:{
                if(resultCode==RESULT_OK && null != data){
                    ArrayList<String> result= data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    mVoiceInputTv.setText(result.get(0));
                    String json_message = convert2Json(result.get(0));
                    startPub(json_message);
                }
                break;
            }
        }
    }

    private String convert2Json(String result){
        String floor = "";
        String device = "";
        String onoff = "";

        if(result.contains("1樓") || result.contains("一樓")){
            floor="1";
        }else if(result.contains("2樓") || result.contains("二樓")){
            floor="2";
        }else{
            floor="err";
        }

        if(result.contains("風扇") || result.contains("電風扇")){
            device="fan";
        }else if(result.contains("電燈") ){
            device="light";
        }else{
            device="err";
        }

        if(result.contains("打開") || result.contains("開啟")){
            onoff="1";
        }else if(result.contains("關閉") || result.contains("關掉")){
            onoff="0";
        }else{
            onoff="err";
        }

        String json = "{\"floor\":\""+floor+"\",\"device\":\""+device+"\",\"onoff\":\""+onoff+"\"}";

        return json;
    }
}
