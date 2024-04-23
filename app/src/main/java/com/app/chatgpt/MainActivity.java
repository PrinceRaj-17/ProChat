package com.app.chatgpt;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    EditText message_text_text;
    ImageView send_btn;
    List<Message> messageList = new ArrayList<>();
    MessageAdapter messageAdapter;

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //====================================
        message_text_text = findViewById(R.id.message_text_text);
        send_btn = findViewById(R.id.send_btn);
        recyclerView = findViewById(R.id.recyclerView);

        // Create Layout behaves and set it in recyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        //====================================

        //====================================
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        //====================================

        if(!isConnected(MainActivity.this)) {
            buildDialog(MainActivity.this).show();
        }

        message_text_text.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if(s.toString().trim().length()==0){
                    send_btn.setEnabled(false);
                    // Toast.makeText(MainActivity.this, "Type your message", Toast.LENGTH_SHORT).show();
                } else {
                    send_btn.setEnabled(true);
                    send_btn.setOnClickListener(view -> {
                        String question = message_text_text.getText().toString().trim();
                        addToChat(question,Message.SEND_BY_ME);
                        message_text_text.setText("");
                        callAPI(question);
                    });
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub

            }
        });

    } // OnCreate Method End Here ================

    void addToChat (String message, String sendBy){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sendBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    } // addToChat End Here =====================

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response, Message.SEND_BY_BOT);
    } // addResponse End Here =======

    void callAPI(String question){
        // okhttp
        messageList.add(new Message("Thinking...", Message.SEND_BY_BOT));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model","gpt-3.5-turbo");
            jsonBody.put("messages", new JSONArray()
                    .put(new JSONObject().put("role", "user").put("content", question)));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + API.API)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray choices = jsonObject.getJSONArray("choices");
                        JSONObject completion = choices.getJSONObject(0);
                        JSONObject message = completion.getJSONObject("message");
                        String assistantResponse = message.getString("content");
                        addResponse(assistantResponse);
                    } else {
                        addResponse("Failed to load response due to " + response.body().string());
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (response.body() != null) {
                        response.body().close(); // Close the response body
                    }
                }
            }
        });
    } // callAPI End Here =============

    public boolean isConnected(Context context){
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info= manager.getActiveNetworkInfo();
        if(info!= null && info.isConnectedOrConnecting()){
            NetworkInfo wifi= manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobile= manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if((mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null && wifi.isConnectedOrConnecting()))
                return true;
            else
                return false;
        } else
            return false;
    }

    public AlertDialog.Builder buildDialog(Context context){
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("No Internet Connection");
        builder.setMessage("Please check your internet connection.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAffinity();
            }
        });
        return builder;
    }
} // Public Class End Here =========================
