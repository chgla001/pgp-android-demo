package de.emporacreative.pgpandroiddemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserListActivity extends AppCompatActivity {
    ArrayList<String> arrayListUsernames;
    OkHttpClient httpClient;
    ArrayAdapter listAdapter;
    JSONArray userlist;
    String host = getString(R.string.host);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        httpClient = new OkHttpClient();
        arrayListUsernames = new ArrayList<>();

        loadUsernames();
        initListView();
        setTitle("Gefundene Chatpartner");
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            loadUsernames();
        }
        return super.onOptionsItemSelected(item);
    }

    public void initListView() {
        ListView listViewUsers = findViewById(R.id.listViewUsers);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayListUsernames);
        listViewUsers.setAdapter(listAdapter);
        listViewUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String chatPartner = String.valueOf(parent.getItemAtPosition(position));
                int userid = -1;
                try {
                    userid = userlist.getJSONObject(position).getInt("id");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Intent showChatActivity = new Intent(getApplicationContext(), ChatActivity.class);
                showChatActivity.putExtra("chatPartnerName", chatPartner);
                showChatActivity.putExtra("chatPartnerId", userid);
                showChatActivity.putExtra("userdata", getIntent().getStringExtra("userdata"));
                startActivity(showChatActivity);
            }
        });
    }

    private void loadUsernames() {
        Request request = new Request.Builder()
                .url(host + "/login/user")
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
                            userlist = new JSONArray(data);
                            arrayListUsernames.clear();
                            for (int i = 0; i < userlist.length(); i++) {
                                JSONObject chatpartner = userlist.getJSONObject(i);
                                arrayListUsernames.add(chatpartner.getString("name"));
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
}
