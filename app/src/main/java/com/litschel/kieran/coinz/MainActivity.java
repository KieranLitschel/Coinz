package com.litschel.kieran.coinz;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.litschel.kieran.coinz.NoInternetDialogFragment.NoInternetDialogCallback;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.StampedLock;

public class MainActivity extends AppCompatActivity implements NoInternetDialogCallback, CoinsUpdateWithDeltaCallback {

    private MainActivity activity = this;
    public SharedPreferences settings;
    private final String settingsFile = "SettingsFile";
    private static final int RC_SIGN_IN = 9000;
    public String uid;
    public FirebaseFirestore db;
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private FragmentManager fragmentManager;
    private Fragment currentFragment = null;
    private Timer myTimer;
    private Handler myTimerTaskHandler;
    public StampedLock mapUpdateLock = new StampedLock();
    public Boolean waitingToUpdateCoins = false;
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 0);
        } else {
            System.out.println("PERMISSION ACESS_NETWORK_STATE IS GRANTED");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 1);
        } else {
            System.out.println("PERMISSION INTERNET IS GRANTED");
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        settings = getSharedPreferences(settingsFile, Context.MODE_PRIVATE);
        uid = settings.getString("uid", "");

        System.out.println("GOT UID OF " + uid + " FROM LOCAL STORAGE");

        db = FirebaseFirestore.getInstance();

        mDrawerLayout = findViewById(R.id.drawer_layout);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        int itemId = menuItem.getItemId();

                        if (!navigationView.getMenu().findItem(menuItem.getItemId()).isChecked()) {
                            Class fragmentClass = null;

                            switch (itemId) {
                                case R.id.nav_map:
                                    fragmentClass = MapFragment.class;
                                    break;
                                case R.id.nav_balance:
                                    fragmentClass = BalanceFragment.class;
                                    break;
                                case R.id.nav_logout:
                                    logout();
                                    break;
                                default:
                                    fragmentClass = MapFragment.class;
                            }

                            // If we use logout we don't want the currently displayed fragment to change
                            if (itemId != R.id.nav_logout) {
                                try {
                                    currentFragment = (Fragment) fragmentClass.newInstance();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // Insert the fragment by replacing any existing fragment
                                fragmentManager = getSupportFragmentManager();
                                fragmentManager.beginTransaction().replace(R.id.flContent, currentFragment).commit();

                                // Highlight the selected item has been done by NavigationView
                                menuItem.setChecked(true);
                                // Set action bar title
                                setTitle(menuItem.getTitle());
                            }
                        }

                        // Close the navigation drawer
                        mDrawerLayout.closeDrawers();

                        return true;
                    }
                });

        signIn();
    }

    private void setDefaultFragment() {
        // Set default fragment to the map

        Class fragmentClass = MapFragment.class;

        try {
            currentFragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.flContent, currentFragment).commit();

        navigationView.setCheckedItem(R.id.nav_map);
        setTitle("Map");
    }

    private void signIn() {
        if (uid.equals("")) {
            if (isNetworkAvailable()) {
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build());

                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            } else {
                DialogFragment noInternetFragment = new NoInternetDialogFragment();
                noInternetFragment.setCancelable(false);
                noInternetFragment.show(getSupportFragmentManager(), "no_internet_dialog");
            }
        } else {
            for (String currency : currencies) {
                if (!settings.getString(currency + "Delta", "").equals("")) {
                    waitingToUpdateCoins = true;
                    break;
                }
            }
            setDefaultFragment();
        }
    }

    // I found this method here (https://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android)
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                // When only using one sign in method (in this case email), the uid for each user is unique
                // so we can use this to uniquely identify them in the database
                uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("uid", uid);
                editor.apply();
                checkFirstTimeUser();
                // ...
            } else {
                if (response != null) {
                    System.out.println("USER FAILED TO SIGN IN WITH ERROR CODE" + response.getError().getErrorCode());
                    Toast.makeText(this, String.format("Failed to sign in with error code %s, please try again", response.getError().getErrorCode()), Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(this, "You must sign in to use this app.", Toast.LENGTH_LONG)
                            .show();
                    signIn();
                }
            }
        }
    }

    private void checkFirstTimeUser() {
        DocumentReference docRef = db.collection("users").document(uid);
        System.out.println("CHECKING IF USER EXISTS IN DATABASE");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (!document.exists()) {
                        System.out.println("FIRST TIME USER, SETTING DEFAULTS");
                        setFirstTimeUser();
                    } else {
                        System.out.println("USER EXISTS IN DATABASE");
                        setDefaultFragment();
                    }
                } else {
                    System.out.println("FAILED TO GET WHETHER USER IS LOGGED IN");
                }
            }
        });
    }

    private void setFirstTimeUser() {
        Map<String, Object> user_defaults = new HashMap<>();
        user_defaults.put("username", "");
        for (String currency : currencies) {
            user_defaults.put(currency, 0.0);
        }
        user_defaults.put("coinsRemainingToday", 0.0);
        user_defaults.put("map", "");
        user_defaults.put("lastDownloadDate", LocalDate.MIN.toString());

        db.collection("users").document(uid)
                .set(user_defaults)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        System.out.println("SUCCESSFULLY ADDED USER TO DATABASE");
                        setDefaultFragment();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("FAILED TO ADD USER TO DATABASE");
                    }
                });
    }

    private void logout() {
        fragmentManager.beginTransaction().remove(currentFragment).commit();
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();
        uid = "";
        signIn();
    }

    public void setToUpdateCoinsOnInternet() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Could not connect to the internet, progress offline will be updated in the cloud when connection is established.", Toast.LENGTH_LONG).show();
            }
        });
        updateCoinsOnInternet();
    }

    private void updateCoinsOnInternet() {
        System.out.println("CREATED UPDATE COINS TIMER TASK");
        TimerTask mTtInternet = new TimerTask() {
            public void run() {
                myTimerTaskHandler.post(() -> {
                    System.out.println("UPDATE COINS TIMER TASK TRIGGERED");
                    if (isNetworkAvailable()) {
                        System.out.println("UPDATE COINS TIMER TASK FOUND INTERNET");
                        new Thread(new CoinsUpdateWithDeltaTask(activity, db, settings, mapUpdateLock, uid)).start();
                    } else {
                        System.out.println("UPDATE COINS TIMER TASK FOUND NO INTERNET");
                        updateCoinsOnInternet();
                    }
                });
            }
        };
        System.out.println("UPDATE COINS TIMER TASK SCHEDULED");
        myTimer.schedule(mTtInternet, 5000);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void tryInternetAgain() {
        signIn();
    }

    @Override
    public void closeApp() {
        finish();
        System.exit(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        myTimer = new Timer();
        myTimerTaskHandler = new Handler();
        // If onStart called, then that means onStop was called prior to it, meaning the timer will
        // have been purged, meaning we need to restart the timer for looking for the internet, which
        // we do below
        if (waitingToUpdateCoins) {
            setToUpdateCoinsOnInternet();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        myTimer.cancel();
        myTimer.purge();
    }

    @Override
    public void onCoinsUpdateWithDeltaComplete(long lockStamp) {
        waitingToUpdateCoins = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Progress offline has been updated in the cloud.", Toast.LENGTH_LONG).show();
            }
        });
        mapUpdateLock.unlockWrite(lockStamp);
    }
}