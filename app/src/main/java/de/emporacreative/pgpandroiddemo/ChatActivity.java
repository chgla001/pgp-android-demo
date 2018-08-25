package de.emporacreative.pgpandroiddemo;


import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.openpgp.PGPException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import de.emporacreative.pgpandroiddemo.PgpUtil.PgpHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {
    String chatPartnerName;
    int chatPartnerId;
    ArrayList<String> arrayListReceivedMessages = new ArrayList<>();
    ArrayList<String> arrayListSentMessages = new ArrayList<>();
    ArrayList<String> arrayListAllMessages = new ArrayList<>();
    ArrayAdapter listAdapter;
    ListView listViewChat;

    OkHttpClient httpClient;
    MediaType JSON = MediaType.get("application/json; charset=utf-8");

    JSONObject userdataUser;
    JSONObject userdataChatpartner;
    private String plainTextFile = "plain-text.txt";
    private String cipherTextFile = "cypher-text.txt";
    private String decPlainTextFile = "dec-plain-text.txt";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        httpClient = new OkHttpClient();
        getDataFromIntent();

        getDataFromChatPartner();

        initActionbar();

        initList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.update_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //Klick Methode für den Zurück-Nutton
        if (id == android.R.id.home) {
            this.finish();
        }
        //Klick Methode für den update-Nutton
        if (id == R.id.action_update) {
            Log.e("TAG", "update Menu item clicked");
            updateMessages();

        }
        return super.onOptionsItemSelected(item);
    }

    private void initList() {
        listViewChat = findViewById(R.id.listViewChat);

//        messages = arrayListReceivedMessages.toArray(new String[0]);
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayListAllMessages);
        listViewChat.setAdapter(listAdapter);
    }

    public void sendMessage(View view) {
        EditText editTextMessage = findViewById(R.id.editTextMessage);
        String clearMessage = editTextMessage.getText().toString();
        Long timestamp = new Date().getTime();
        Log.e("TAG", "timestamp: " + timestamp);
        JSONObject jsonObject = new JSONObject();

        try {
            /*save in sendList*/
            arrayListSentMessages.add("(" + MyUtils.convertTime(timestamp) + ") " + userdataUser.getString("name") + ": " + clearMessage);
            updateMessageList();

            encrypt(userdataChatpartner.getInt("id"), clearMessage);
            String encryptedMessage = MyUtils.convertInputStreamToString(openFileInput(cipherTextFile));

            jsonObject.put("text", encryptedMessage);
            jsonObject.put("timestamp", timestamp);
            jsonObject.put("read", false);
            jsonObject.put("senderid", userdataUser.getInt("id"));
            jsonObject.put("receiverid", userdataChatpartner.getInt("id"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("TAG", "sendMessage() ich schicke ab: " + jsonObject.toString());
        RequestBody body = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url("http://192.168.2.116:4000/messages/new-message")
                .post(body)
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String responseString = response.body().string();
                            Log.e("TAG", "sendMessage() responseString " + responseString);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void updateMessages() {
        arrayListReceivedMessages.clear();
        try {
            //Log.e("userdata", "updateMessages: " + userdataUser.toString());
            int userid = userdataUser.getInt("id");
            int chatpartnerid = userdataChatpartner.getInt("id");

            //received the sent messages from the chatpartner to the user
            loadMessages(chatpartnerid, userid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadMessages(final int senderid, final int recipientid) {
        Log.e("TAG", "sender: " + senderid + ", recipient " + recipientid);


        Request request = new Request.Builder()
                .url("http://192.168.2.116:4000/messages/getMessages?senderid=" + senderid + "&recipientid=" + recipientid)
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TAG", e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) {
                final String data;
                if (response.code() == 204) {
                    Toast.makeText(ChatActivity.this, "Keine Nachrichten", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "loadMessages() Statuscode " + response.code());
                } else {
                    try {
                        data = response.body().string();
                        //Log.e("arraylist", data);
                        JSONArray messageArray = null;
                        if (data.length() > 0) {
                            messageArray = new JSONArray(data);
                            for (int i = 0; i < messageArray.length(); i++) {
                                JSONObject messageObject = messageArray.getJSONObject(i);

                                MyUtils.createFile(getApplicationContext(), cipherTextFile, messageObject.getString("text"));
                                decrypt(userdataUser.getString("name"), userdataUser.getString("password"));

                                Log.e("TAG", "loadMessages() timestamp" + messageObject.getLong("timestamp"));
                                String decryptedMessage = MyUtils.readFile(getApplicationContext(), decPlainTextFile);
                                arrayListReceivedMessages.add("(" + MyUtils.convertTime(messageObject.getLong("timestamp")) + ") " + userdataChatpartner.getString("name") + ": " + decryptedMessage);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateMessageList();
                    }
                });
            }
        });
    }


    private void updateMessageList() {
        arrayListAllMessages.clear();
        arrayListAllMessages.addAll(arrayListReceivedMessages);
        arrayListAllMessages.addAll(arrayListSentMessages);

        Collections.sort(arrayListAllMessages, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

        Log.e("TAG", "updateMessageList() arrayListReceivedMessages: " + arrayListReceivedMessages.toString());
        Log.e("TAG", "updateMessageList() arrayListSentMessages: " + arrayListSentMessages.toString());
        Log.e("TAG", "updateMessageList() arrayListReceivedMessages: " + arrayListAllMessages.toString());
        listAdapter.notifyDataSetChanged();
    }

    //reading the chatpartner and the userdata from the activity before
    private void getDataFromIntent() {
        Intent intent = getIntent();
        chatPartnerName = intent.getStringExtra("chatPartnerName");
        chatPartnerId = intent.getIntExtra("chatPartnerId", -1);
        try {
            userdataUser = new JSONObject(intent.getStringExtra("userdata"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void initActionbar() {
        setTitle(chatPartnerName);
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getDataFromChatPartner() {
        Request request = new Request.Builder()
                .url("http://192.168.2.116:4000/login/userById?id=" + chatPartnerId)
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String data = response.body().string();
                            Log.e("TAG", "userdataChatpartner " + data);
                            userdataChatpartner = new JSONObject(data);
                            MyUtils.createFile(getApplicationContext(), "pubKey" + userdataChatpartner.getInt("id") + ".txt", userdataChatpartner.getString("pgpKey"));
                        } catch (Exception e) {
                            Log.e("Error", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * @see "https://github.com/damico/OpenPgp-BounceCastle-Example/blob/master/src/org/jdamico/bc/openpgp/tests/TestBCOpenPGP.java"
     */
    private void encrypt(int receiverId, String nachricht) throws IOException, PGPException {
        boolean isArmored = true;
        boolean integrityCheck = true;
        MyUtils.createFile(getApplicationContext(), plainTextFile, nachricht);
//        FileInputStream pubKeyIs = openFileInput("pub_alice.txt");
        FileInputStream pubKeyIs = openFileInput("pubKey" + receiverId + ".txt");
        FileOutputStream cipheredFileIs = openFileOutput(cipherTextFile, Context.MODE_PRIVATE);
        PgpHelper.getInstance().encryptFile(cipheredFileIs, getFilesDir().getAbsolutePath() + "/" + plainTextFile, PgpHelper.getInstance().readPublicKey(pubKeyIs), isArmored, integrityCheck);
        cipheredFileIs.close();
        pubKeyIs.close();
    }

    /**
     * @see "https://github.com/damico/OpenPgp-BounceCastle-Example/blob/master/src/org/jdamico/bc/openpgp/tests/TestBCOpenPGP.java"
     */
    private void decrypt(String username, String passwd) {
        FileInputStream cipheredFileIs = null;
        try {
            cipheredFileIs = openFileInput(cipherTextFile);

            FileInputStream privKeyIn = openFileInput("privKey" + username + ".txt");
            FileOutputStream plainTextFileIs = openFileOutput(decPlainTextFile, Context.MODE_PRIVATE);

            PgpHelper.getInstance().decryptFile(cipheredFileIs, plainTextFileIs, privKeyIn, passwd.toCharArray());
            cipheredFileIs.close();
            plainTextFileIs.close();
            privKeyIn.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PGPException e) {
            e.printStackTrace();
        }
    }
}
