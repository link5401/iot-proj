package dashboard.android;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    MQTTHelper mqttHelper;
    TextView txtTemp;
    TextView toggleText1;
    TextView txtHumid;
    ToggleButton toggleButton1;
    public class MQTTMessage{
        public String topic;
        public String mess;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        txtTemp = findViewById(R.id.Temperature);
        txtHumid = findViewById(R.id.Humid);
        toggleButton1 = findViewById(R.id.toggleButton1);
        toggleText1 = findViewById(R.id.TogglePush);
        toggleButton1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
                toggleButton1.setVisibility(View.INVISIBLE);
                if(isChecked == true) {
                    toggleText1.setText("ON");
                    sendDataMQTT("linhla/feeds/LED","ON");
                }
                else{
                    sendDataMQTT("linhla/feeds/LED","OFF");
                    toggleText1.setText("OFF");
                }
            }

        });
        toggleButton1.setVisibility(View.VISIBLE);
        startMQTT();
        setupScheduler();
    }
    int waiting_period = 0;
    boolean send_message_again = false;
    List<MQTTMessage> list = new ArrayList<>();
    private void setupScheduler(){
        Timer aTimer = new Timer();
        TimerTask scheduler = new TimerTask(){
            @Override
            public void run(){
                Log.d("mqtt","Timer is executed");
                if(waiting_period > 0){
                    waiting_period--;
                    if(waiting_period == 0){
                        send_message_again = true;
                    }
                }
                if(send_message_again == true){


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleButton1.setVisibility(View.VISIBLE);
                        }
                    });
                    sendDataMQTT(list.get(0).topic, list.get(0).mess);
                    list.remove(0);

                }
            }
        };
        aTimer.schedule(scheduler,0,1000);
    }
    private void sendDataMQTT(String topic,String value){
        waiting_period = 3;
        send_message_again = false;

        MQTTMessage aMessage = new MQTTMessage();
        aMessage.topic = topic; aMessage.mess = value;
        list.add(aMessage);


        MqttMessage msg = new MqttMessage();
        msg.setId(1234);
        msg.setQos(0);
        msg.setRetained(true);


        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);

        }catch (MqttException e){
        }
    }
    private void startMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext(), "123456");

        mqttHelper.setCallback(new MqttCallbackExtended(){
            @Override
            public void connectComplete(boolean b, String s) {
              Log.d("mqtt","Connection is Successful");
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d("mqtt","Received: " + mqttMessage.toString());
                if(mqttMessage.toString() == "ON") {
                    toggleButton1.setChecked(true);
                }
                else if(mqttMessage.toString() == "OFF"){
                    toggleButton1.setChecked(false);
                }
                if(topic.contains("error_control")){
                    waiting_period = 0;
                    send_message_again = false;
                    toggleButton1.setVisibility(View.VISIBLE);
                }
                if(topic.contains("MICROBIT_TEMP")){
                    txtTemp.setText(mqttMessage.toString() + "°C");
                }
                if(topic.contains("microbit-humid")){
                    txtHumid.setText(mqttMessage.toString()+"%");
                }


            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

              }



        });



    }

}
