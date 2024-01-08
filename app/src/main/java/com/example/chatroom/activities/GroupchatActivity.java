package com.example.chatroom.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.chatroom.R;
import com.example.chatroom.adapters.ChatAdapter;
import com.example.chatroom.databinding.ActivityChatBinding;
import com.example.chatroom.models.ChatMessage;
import com.example.chatroom.models.User;
import com.example.chatroom.utilities.Constants;
import com.example.chatroom.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class GroupchatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    public String senderProfileImage;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversationId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
//        loadReceiverDetails();
        init();
        listenMessages();
    }
    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
               null,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void sendMessage(){
        HashMap<String,Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
//        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_GROUPCHAT).add(message);
        binding.inputMessage.setText(null);
    }

    private void listenAvailabilityOfReceiver(){
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id).addSnapshotListener(GroupchatActivity.this,(value,error) -> {
                    if(error!= null){
                        return;
                    }
                    if (value != null){
                        if(value.getLong(Constants.KEY_AVAILABILITY) != null){
                            int availability = Objects.requireNonNull(
                                    value.getLong(Constants.KEY_AVAILABILITY)
                            ).intValue();
                            isReceiverAvailable = availability == 1;
                        }

                    }
                    if(isReceiverAvailable){
                        binding.textAvailability.setVisibility(View.VISIBLE);
                    }
                    else {
                        binding.textAvailability.setVisibility(View.GONE);
                    }
                }
        );
    }

    private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_GROUPCHAT)
                .orderBy(Constants.KEY_TIMESTAMP) // Optional: Order messages by timestamp
                .addSnapshotListener(eventListener);
    }
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }

            }
                chatAdapter.notifyDataSetChanged();

            int targetPosition = chatMessages.size() - 1;
            if (targetPosition >= 0) {
                binding.chatRecyclerView.smoothScrollToPosition(targetPosition);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);

        }
        binding.progressBar.setVisibility(View.GONE);

    };

//    private String loadSenderProfilePicture(String senderId) {
//        database.collection(Constants.KEY_COLLECTION_USERS)
//                .document(senderId)
//                .get()
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful() && task.getResult() != null) {
//                        DocumentSnapshot documentSnapshot = task.getResult();
//                        if (documentSnapshot.exists()) {
//                            String senderProfileImage = documentSnapshot.getString(Constants.KEY_IMAGE);
//                            Bitmap senderBitmap = getBitmapFromEncodedString(senderProfileImage);
//                        }
//                    }
//                });
//    }
//    private String loadSenderImg(){
//        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
//        byte [] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
//        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
//        return bitmap;
//    }
    private Bitmap getBitmapFromEncodedString(String encodedImage){
        byte [] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }

    private void loadReceiverDetails(){
        receiverUser = (User)getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }

    private void setListeners(){

        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
    }

    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversation(HashMap<String,Object> conversation){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }

    private void updateConversation(String message){
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .document(conversationId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,message,Constants.KEY_TIMESTAMP,new Date()
        );
    }

    private void checkForConversation(){
        if(chatMessages.size() != 0){
            checkForConversationRemotely(

            );
            checkForConversationRemotely(

            );
        }
    }
    private void checkForConversationRemotely() {
        database.collection(Constants.KEY_COLLECTION_GROUPCHAT)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);

    }
    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            // A conversation (messages in the group chat) exists

            // Assuming the conversation ID is the document ID of the first message
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            String messageId = documentSnapshot.getId();

            // Do something with the messageId if needed, or store it in your activity/fragment
            // For example, you might use it to uniquely identify the conversation

            // If you have a specific use for the conversationId, you might set it as a field in your activity/fragment
            // conversationId = messageId;
        } else {
          Toast.makeText(getApplicationContext(),"No Group Messages", Toast.LENGTH_LONG).show();
        }
    };


    @Override
    protected void onResume() {
        super.onResume();

    }
}
