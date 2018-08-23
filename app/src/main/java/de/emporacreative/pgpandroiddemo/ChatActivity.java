package de.emporacreative.pgpandroiddemo;


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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

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
    ArrayList<String> arrayListMessages = new ArrayList<>();
    ArrayAdapter listAdapter;
    ListView listViewChat;

    OkHttpClient httpClient;
    MediaType JSON = MediaType.get("application/json; charset=utf-8");

    JSONObject userdataUser;
    JSONObject userdataChatpartner;

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
            //todo update messages
            updateMessages();
            Toast.makeText(this, "update", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initList() {
        listViewChat = findViewById(R.id.listViewChat);

        //Todo: Nachrichten aus Datenbank laden -> kann später rausgenommen werden, da in updateMessages die nachrichten neu geladen werden
        arrayListMessages.add("Testnachricht1");
        arrayListMessages.add("Testnachricht2");

//        messages = arrayListMessages.toArray(new String[0]);
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayListMessages);
        listViewChat.setAdapter(listAdapter);
    }

    public void sendMessage(View view) {
        EditText editTextMessage = findViewById(R.id.editTextMessage);

        JSONObject jsonObject = new JSONObject();
//        Log.e("userdata", userdataUser.toString());
//        Log.e("userdataChat", userdataChatpartner.toString());
        try {
            jsonObject.put("text", editTextMessage.getText().toString());
            jsonObject.put("timestamp", new Date().getTime() + "");
            jsonObject.put("read", false);
            jsonObject.put("senderid", userdataUser.getInt("id"));
            jsonObject.put("receiverid", userdataChatpartner.getInt("id"));
        } catch (Exception e) {
            e.printStackTrace();
        }


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
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String responseString = response.body().string();
                            Log.e("TAG", "responseString " + responseString);
                            JSONObject jsonObject1 = new JSONObject(responseString);
                            upddateMessageList(jsonObject1.getString("text"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        Log.e("arraylist", arrayListMessages.toString());
        listAdapter.notifyDataSetChanged();
    }

    private void updateMessages() {
        arrayListMessages.clear();
        try {
            Log.e("userdata", "updateMessages: " + userdataUser.toString());
            int userid = userdataUser.getInt("id");
            int chatpartnerid = userdataChatpartner.getInt("id");
            //sent messages
            loadMessages(userid, chatpartnerid);
            //received messages
            loadMessages(chatpartnerid, userid);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void loadMessages(int senderid, int recipientid) {
        Log.e("sender recipient", senderid + " " + recipientid);
        Request request = new Request.Builder()
                .url("http://192.168.2.116:4000/messages/getMessages?senderid=" + senderid + "&recipientid=" + recipientid)
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
                            //Log.e("arraylist", data);
                            JSONArray jsonArray = null;
                            if (data.length() > 0) {
                                jsonArray = new JSONArray(data);
                            }
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject userdata = jsonArray.getJSONObject(i);
                                arrayListMessages.add(userdata.getString("text"));
                            }
                            listAdapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            Log.e("Error", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void upddateMessageList(String data) {
        arrayListMessages.add(data);
        Log.e("arraylist", arrayListMessages.toString());
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
                            Log.e("userdataChatpartner", data);
                            userdataChatpartner = new JSONObject(data);
                        } catch (Exception e) {
                            Log.e("Error", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

}
