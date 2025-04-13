package com.example.apis;


import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import adapter.ContactAdapter;
import beans.Contact;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_READ_CONTACTS = 100;
    private static final int REQUEST_CALL_PHONE = 101;
    private static final String TAG = "MainActivity";
    private ContactAdapter adapter;
    private RecyclerView lv;
    private final ArrayList<Contact> aa = new ArrayList<>(); // Made final
    private ProgressDialog progressDialog;
    private ApiService apiService;
    private FloatingActionButton  btn;// Moved Retrofit instance to class level

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        lv = findViewById(R.id.lv);
        btn = findViewById(R.id.floatingActionButton);


        // Initialize Retrofit
        initRetrofit();

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Syncing contacts...");
        progressDialog.setCancelable(false);

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    REQUEST_READ_CONTACTS);
        } else {
            getNumber(getContentResolver());
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create intent to launch new activity
                Intent intent = new Intent(MainActivity.this, AddContact.class);
                startActivity(intent);

                // Optional: Add transition animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void initRetrofit() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.0.171:8080/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getNumber(getContentResolver());
            } else {
                Toast.makeText(this, "Permission READ_CONTACTS denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Retry the call action if needed
            }
        }
    }

    public void getNumber(ContentResolver cr) {
        progressDialog.show();

        new Thread(() -> {
            Cursor phones = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );

            if (phones != null) {
                try {
                    if (phones.moveToFirst()) {
                        do {
                            String name = phones.getString(
                                    phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            String phoneNumber = phones.getString(
                                    phones.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));


                            phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");


                            Contact contact = new Contact(name, phoneNumber);

                            runOnUiThread(() -> aa.add(contact));


                            sendContactToBackend(name, phoneNumber);

                        } while (phones.moveToNext());
                    }
                } finally {
                    phones.close();
                }
            }

            runOnUiThread(() -> {
                adapter = new ContactAdapter(this, aa);
                lv.setAdapter(adapter);
                lv.setLayoutManager(new LinearLayoutManager(this));
                progressDialog.dismiss();
            });
        }).start();
    }

    public void showContactActionsDialog(String name, String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Action for " + name);
        builder.setMessage("Select an action");

        builder.setPositiveButton("Call", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phone));
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                startActivity(intent);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CALL_PHONE},
                        REQUEST_CALL_PHONE);
            }
        });

        builder.setNegativeButton("SMS", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("sms:" + phone));
            startActivity(intent);
        });

        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void sendContactToBackend(String name, String phoneNumber) {
        Contact contact = new Contact(name, phoneNumber);
        apiService.createContact(contact).enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Contact sent successfully: " + name);
                } else {
                    try {
                        Log.e(TAG, "Failed to send contact. Code: " + response.code()
                                + " Error: " + (response.errorBody() != null ? response.errorBody().string() : "null"));
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        MenuItem menuItem = menu.findItem(R.id.app_bar_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText); // This triggers the filtering
                return false;
            }
        });
        return true;
    }

}