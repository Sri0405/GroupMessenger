package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static int sequence =0;

    static final String Sequencer= "11108";

    static final int SERVER_PORT = 10000;
    //static int sequence = 0;
    static Uri muri;

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        muri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*  To see which avd it is listening from ?*/


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        //   findViewById(R.id.button4).setOnClickListener(this.insertMessages(myPort));

        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        final Button button = (Button) findViewById(R.id.button4);
        //final EditText editText = (EditText) findViewById(R.id.editText1);
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                final EditText editText = (EditText) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);


            }
        });
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket =sockets[0];
            if(serverSocket != null)
            {
                while(true)
                {
                    BufferedReader input =null;
                    String line ;
                    try{
                        Socket clientSocket = serverSocket.accept();
                        input = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()));
                    }catch( IOException e){
                        e.printStackTrace();
                    }
                    try{
                        line = input.readLine();
                        line =line.trim();
                        String lineparts[] = line.split("%");
                        Log.i(TAG,line);
                        if(!lineparts[1].isEmpty())
                        {
                            if(lineparts[0].equals(Sequencer))
                            {
                                // send to all avds with seq number
                                String msg = Integer.toString(sequence)+"%"+lineparts[1];
                                new ClientTask1().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, lineparts[1]);
                                sequence = sequence +1;
                            }
                            else if(!lineparts[0].equals(Sequencer))
                            {
                                publishProgress(line);
                            }
                        }


                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {

            String strReceived = strings[0].trim();
            String strparts[] = strReceived.split("%");

            ContentValues cv = new ContentValues();

            cv.put("key",strparts[0]);
            cv.put("value",strparts[1]);

            try {
                Log.i(Integer.toString(sequence), muri.toString());
                getContentResolver().insert(muri, cv);
            }catch (NullPointerException e){
                Log.e("null exception here : ++ ",Integer.toString(sequence));
            }
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String remotePorts[]= {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
                // int count = 4;
                for (int i =0;i < 5;i++)
                {
                    String remotePort1 = remotePorts[i];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort1));

                    String msgToSend =remotePort1+"%"+msgs[0];
                    Log.i("this is message sending", msgToSend);
                    PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                    toServer.println(msgToSend);
                    socket.close();
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }


    private class ClientTask1 extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String remotePorts[]= {REMOTE_PORT0,REMOTE_PORT1,REMOTE_PORT2,REMOTE_PORT3,REMOTE_PORT4};
                // int count = 4;
                for (int i =0;i < 5;i++)
                {
                    String remotePort1 = remotePorts[i];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort1));

                    String msgToSend = msgs[0];
                    PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
                    toServer.println(msgToSend);
                    socket.close();
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }
            return null;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}