package com.rethink.drop.tools;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rethink.drop.MainActivity;
import com.rethink.drop.R;
import com.rethink.drop.models.Drop;

import java.util.ArrayList;
import java.util.Calendar;

public class Notifications {
    private ArrayList<DatabaseReference> refs;
    private ArrayList<ChildEventListener> listeners;
    private ArrayList<String> texts;
    private long startTime;
    private NotificationManagerCompat notificationManager;
    private Context context;

    public Notifications(Context context, NotificationManagerCompat notificationManager, String userID) {
        this.context = context;
        this.notificationManager = notificationManager;
        refs = new ArrayList<>();
        listeners = new ArrayList<>();
        texts = new ArrayList<>();
        startTime = Calendar.getInstance()
                            .getTimeInMillis();
        FirebaseDatabase.getInstance()
                        .getReference()
                        .child("profiles")
                        .child(userID)
                        .child("friends")
                        .addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                DatabaseReference ref = FirebaseDatabase.getInstance()
                                                                        .getReference()
                                                                        .child("profiles")
                                                                        .child(dataSnapshot.getKey())
                                                                        .child("posts");
                                refs.add(ref);
                                listeners.add(ref.addChildEventListener(new FriendPostListener()));

                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {
                                DatabaseReference ref = FirebaseDatabase.getInstance()
                                                                        .getReference()
                                                                        .child("profiles")
                                                                        .child(dataSnapshot.getKey())
                                                                        .child("posts");
                                int index = refs.indexOf(ref);
                                refs.remove(index);
                                ref.removeEventListener(listeners.get(index));
                                listeners.remove(index);
                            }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
    }

    private class FriendPostListener implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Drop drop = dataSnapshot.getValue(Drop.class);
            if (drop.getTimestamp() > startTime) {
                texts.add(drop.getText());
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                for (String text : texts) {
                    inboxStyle.addLine(text);
                }
                Intent resultIntent = new Intent(
                        context,
                        MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                Notification notification = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_drop)
                                                                                   .setContentTitle("Your friend made a Drop!")
                                                                                   .setStyle(inboxStyle)
                                                                                   .setContentIntent(pendingIntent)
                                                                                   .build();
                // Builds the notification and issues it.
                notificationManager.notify(1,
                                           notification);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }
}
