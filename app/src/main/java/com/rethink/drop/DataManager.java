package com.rethink.drop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.rethink.drop.models.Listing;

import java.util.ArrayList;
import java.util.HashMap;

import static com.rethink.drop.fragments.LocalFragment.listingsAdapter;

public class DataManager {

    public static ArrayList<String> keys;
    public static HashMap<String, Bitmap> imageBitmaps;
    public static HashMap<String, Listing> listings;
    public static HashMap<String, Boolean> loadedListings;
    private FirebaseStorage firebaseStorage;
    private DataListener dataListener;
    private DatabaseReference listingsRef;
    private float degreesPerMile = 0.01449275362f;

    DataManager() {
        keys = new ArrayList<>();
        imageBitmaps = new HashMap<>();
        listings = new HashMap<>();
        loadedListings = new HashMap<>();
        firebaseStorage = FirebaseStorage.getInstance();
        listingsRef = FirebaseDatabase.getInstance()
                                      .getReference()
                                      .child("listings");
        listingsRef.orderByChild("timestamp")
                   .limitToFirst(100);
        dataListener = new DataListener();
        listingsRef.addChildEventListener(dataListener);
    }

    public void detachListeners() {
        listingsRef.removeEventListener(dataListener);
    }

    private class DataListener
            implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            final String key = dataSnapshot.getKey();
            Listing listing = dataSnapshot.getValue(Listing.class);
            keys.add(key);
            listings.put(key, listing);
            if (listing.getImageURL() != null) {
                loadedListings.put(key, false);
                if (!listing.getImageURL().equals("")) {
                    firebaseStorage.getReferenceFromUrl(listing.getImageURL())
                                   .getBytes(4 * (1024 * 1024))
                                   .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                       @Override
                                       public void onSuccess(final byte[] bytes) {
                                           Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                           int dimens = Math.min(bmp.getWidth(), bmp.getHeight());
                                           imageBitmaps.put(key, Bitmap.createBitmap(bmp, 0, 0, dimens, dimens));
                                           loadedListings.put(key, true);
                                           listingsAdapter.notifyDataSetChanged();
                                       }
                                   });
                }
            }
            listingsAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            final String key = dataSnapshot.getKey();
            Listing listing = dataSnapshot.getValue(Listing.class);
            listings.put(key, listing);
            if (!listing.getImageURL().equals("")) {
                final long ONE_KILOBYTE = 1024;
                firebaseStorage.getReferenceFromUrl(listing.getImageURL())
                               .getBytes(512 * ONE_KILOBYTE)
                               .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                   @Override
                                   public void onSuccess(final byte[] bytes) {
                                       Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                       int dimens = Math.min(bmp.getWidth(), bmp.getHeight());
                                       imageBitmaps.put(key, Bitmap.createBitmap(bmp, 0, 0, dimens, dimens));
                                       listingsAdapter.notifyDataSetChanged();
                                   }
                               });
            }
            listingsAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            final String key = dataSnapshot.getKey();
            String imageURL = listings.get(key).getImageURL();
            if (imageURL != null && !imageURL.equals("")) {
                firebaseStorage.getReferenceFromUrl(listings.get(key).getImageURL()).delete();
            }
            keys.remove(key);
            listings.remove(key);
            imageBitmaps.remove(key);
            listingsAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
        }
    }
}
