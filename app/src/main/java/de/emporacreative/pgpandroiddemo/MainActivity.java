package de.emporacreative.pgpandroiddemo;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import de.emporacreative.pgpandroiddemo.PgpUtil.RSAKeyPairGenerator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    EditText editTextName;
    EditText editTextPassword;

    JSONObject jsonObject;

    OkHttpClient httpClient;
    MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextName = findViewById(R.id.editTextName);
        editTextPassword = findViewById(R.id.editTextPassword);

        httpClient = new OkHttpClient();
    }

    public void register(View view) {

        try {
            genKeyPair(editTextName.getText().toString(), editTextPassword.getText().toString());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Fehler beim Erstellen der Schlüssel", Toast.LENGTH_SHORT).show();
        }
        sendUserDataToDatabase();
    }

    private void genKeyPair(String name, String passwd) throws IOException, PGPException, NoSuchAlgorithmException {
        boolean isArmored = true;

        RSAKeyPairGenerator rkpg = new RSAKeyPairGenerator();

        Security.addProvider(new BouncyCastleProvider());

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", new BouncyCastleProvider());

        kpg.initialize(2048);

        KeyPair kp = kpg.generateKeyPair();

        FileOutputStream out1 = openFileOutput("privKey" + name + ".txt", Context.MODE_PRIVATE);
        FileOutputStream out2 = openFileOutput("pubKey" + name + ".txt", Context.MODE_PRIVATE);

        rkpg.exportKeyPair(out1, out2, kp.getPublic(), kp.getPrivate(), name, passwd.toCharArray(), isArmored);


    }

    private void sendUserDataToDatabase() {
        /*send userdata to DB*/
        jsonObject = new JSONObject();
        FileInputStream pubKeyIs = null;
        String username = editTextName.getText().toString();
        String password = editTextPassword.getText().toString();
        try {
            pubKeyIs = openFileInput("pubKey" + username + ".txt");
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "FileNotFoundException", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        String publicKey = MyUtils.convertInputStreamToString(pubKeyIs);
        try {
            jsonObject.put("name", username);
            jsonObject.put("email", "not.set@email.yet");
            jsonObject.put("password", password); //todo encrypt
            jsonObject.put("pgpkey", publicKey);

        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(JSON, jsonObject.toString());
        //Log.e("body", body.toString());
        Request request = new Request.Builder()
                .url("http://192.168.2.116:4000/login/userdata")
                .post(body)
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String userdata = response.body().string();
                            Log.e("TAG", "sendUserDataToDatabase() userdata from response" +userdata);
                            //Toast.makeText(MainActivity.this,  response.code() +" the code" , Toast.LENGTH_SHORT).show();

                            Intent showUserListActivity = new Intent(getApplicationContext(), UserListActivity.class);
                            showUserListActivity.putExtra("userdata", userdata);
                            startActivity(showUserListActivity);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }


}
