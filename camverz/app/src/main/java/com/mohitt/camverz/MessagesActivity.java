package com.mohitt.camverz;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MessagesActivity extends BaseActivity {

    private RecyclerView messagesRecyclerView;
    private TextView noMessagesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        // Back button
        findViewById(R.id.back_button_container).setOnClickListener(v -> finish());

        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        noMessagesText = findViewById(R.id.no_messages_text);

        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // TODO: Replace with actual data
        ArrayList<String> conversations = new ArrayList<>();

        if (conversations.isEmpty()) {
            noMessagesText.setVisibility(View.VISIBLE);
            messagesRecyclerView.setVisibility(View.GONE);
        } else {
            noMessagesText.setVisibility(View.GONE);
            messagesRecyclerView.setVisibility(View.VISIBLE);
            // messagesRecyclerView.setAdapter(new MessagesAdapter(this, conversations));
        }
    }
}
