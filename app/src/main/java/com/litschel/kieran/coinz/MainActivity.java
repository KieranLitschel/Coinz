package com.litschel.kieran.coinz;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.litschel.kieran.coinz.NoInternetDialogFragment.NoInternetDialogCallback;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.StampedLock;

// COMMENT MAIN ACTIVITY

public class MainActivity extends AppCompatActivity implements NoInternetDialogCallback, CoinsUpdateWithDeltaCallback, CoinsUpdateTaskCallback {

    private String[] testerUids = new String[]{
            "EdrUWa2aEjNP0PAnyzi34AZRKGG3",
            "9dAfz9WWB2SWalZdcHmLhjrGdvT2",
            "GwNuEg0pEUQvpuaBXiCU5uWip0G2",
            "nXt3iQm2uOhpwtAPocgd2ii9imi2",
            "d9X1huNfATNk0tsDM8T8AWLM4qb2",
            "VLtSELSe31cLPvNsvF692VyO5Py1",
            "7SaETL8QlXUQtgJMaJb9xO5JVAY2",
            "ceFCMjlFOhXqrAHyuhKwEZGUUHn1",
            "dUfwCZBX2EW1QIHwHkhNZoqOWf52",
            "8SpoGV9JFlXKlIiuAXkQ22PB0MF3",
            "ROtiCeFTuIZ3xNOhEweThG3htXj1"
    };
    public boolean tester = false;
    private boolean testerInternet = true;
    public String users;
    public String gifts;
    public String users_gifts;
    private MainActivity activity = this;
    public SharedPreferences settings;
    private static final int RC_SIGN_IN = 9000;
    public String uid;
    public FirebaseFirestore db;
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private FragmentManager fragmentManager;
    private Fragment currentFragment = null;
    public StampedLock settingsWriteLock = new StampedLock();
    public Boolean waitingToUpdateCoins = false;
    public ExecutorService coinsUpdateExecutor;
    private ListenerRegistration userGiftListener;
    private boolean waitingToListenForGifts = false;
    public boolean waitingToUpdateMap = false;
    public boolean waitingToInitialMapSetup = false;
    private boolean settingUpUserGiftListener = false;
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
    private BroadcastReceiver networkChangeReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onNetworkChange();
        }
    };
    private FloatingActionButton flipInternetStateBtn;
    private FloatingActionButton nextDayBtn;
    private LocalDate fixedTestDay = LocalDate.of(2018, 12, 1);
    private boolean justLoggedIn;
    private boolean justUpdated;

    private void onNetworkChange() {
        if (isNetworkAvailable()) {
            if (waitingToListenForGifts) {
                waitingToListenForGifts = false;
                setUpListenerForGifts();
            }
            if (currentFragment != null) {
                if (currentFragment.getClass() == MapFragment.class && waitingToInitialMapSetup) {
                    System.out.println("FOUND INTERNET CHECKING FOR INITIAL MAP SETUP");
                    waitingToInitialMapSetup = false;
                    ((MapFragment) currentFragment).initialMapSetup();
                }
                if (currentFragment.getClass() == MapFragment.class && waitingToUpdateMap) {
                    System.out.println("FOUND INTERNET CHECKING FOR MAP UPDATE");
                    waitingToUpdateMap = false;
                    ((MapFragment) currentFragment).checkForMapUpdate();
                }
            }
            if (waitingToUpdateCoins) {
                waitingToUpdateCoins = false;
                coinsUpdateExecutor.submit(new CoinsUpdateWithDeltaTask(users, activity, db, settings, settingsWriteLock, uid));
            }
        } else {
            settingUpUserGiftListener = false;
            waitingToListenForGifts = true;
            if (userGiftListener != null) {
                userGiftListener.remove();
            }
            userGiftListener = null;
        }
    }

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

        coinsUpdateExecutor = Executors.newFixedThreadPool(1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        } else {
            System.out.println("EXPECTED ACTION BAR TO BE NON-NULL BUT FOUND TO BE NULL");
        }

        nextDayBtn = findViewById(R.id.changeDayButton);
        nextDayBtn.setOnClickListener(view -> {
            fixedTestDay = fixedTestDay.plusDays(1);
            if (currentFragment.getClass() == MapFragment.class) {
                ((MapFragment) currentFragment).checkForMapUpdate();
            }
        });
        flipInternetStateBtn = findViewById(R.id.internetButton);
        flipInternetStateBtn.setOnClickListener(view -> {
            testerInternet = !testerInternet;
            onNetworkChange();
        });

        String settingsFile = "SettingsFile";
        settings = getSharedPreferences(settingsFile, Context.MODE_PRIVATE);
        uid = settings.getString("uid", "");
        setupIfTester();

        System.out.println("GOT UID OF " + uid + " FROM LOCAL STORAGE");

        db = FirebaseFirestore.getInstance();

        mDrawerLayout = findViewById(R.id.drawer_layout);

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
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
                            case R.id.nav_leaderboard:
                                fragmentClass = LeaderboardFragment.class;
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
                List<AuthUI.IdpConfig> providers = Collections.singletonList(
                        new AuthUI.IdpConfig.EmailBuilder().build());
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false)
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

    public boolean isNetworkAvailable() {
        if (tester && !testerInternet) {
            return false;
        } else {
            // I found this method here (https://stackoverflow.com/questions/4238921/detect-whether-there-is-an-internet-connection-available-on-android)
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            } else {
                System.out.println("EXECTED CONECTIVITY MANAGER TO BE NON-NULL BUT FOUND TO BE NULL");
                return false;
            }
        }
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
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    justLoggedIn = true;
                    setupIfTester();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("uid", uid);
                    editor.apply();
                    checkFirstTimeUser();
                } else {
                    System.out.println("FIREBASEAUTH RETURN NULL USER WHEN EXPECTED TO BE NON-NULL");
                }
                // ...
            } else {
                if (response != null) {
                    if (response.getError() != null) {
                        System.out.println("USER FAILED TO SIGN IN WITH ERROR CODE " + response.getError().getErrorCode());
                        Toast.makeText(this, String.format("Failed to sign in with error code %s, please try again", response.getError().getErrorCode()), Toast.LENGTH_LONG)
                                .show();
                    } else {
                        System.out.println("USER FAILED TO SIGN IN WITH AN UNKNOWN ERROR");
                        Toast.makeText(this, "Failed to sign in with an unknown error, please try again", Toast.LENGTH_LONG)
                                .show();
                    }
                } else {
                    Toast.makeText(this, "You must sign in to use this app.", Toast.LENGTH_LONG)
                            .show();
                    signIn();
                }
            }
        }
    }

    private void checkFirstTimeUser() {
        DocumentReference docRef = db.collection(users).document(uid);
        System.out.println("CHECKING IF USER EXISTS IN DATABASE");
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    System.out.println("FIRST TIME USER, SETTING DEFAULTS");
                    setFirstTimeUser();
                } else {
                    System.out.println("USER EXISTS IN DATABASE");
                    setDefaultFragment();
                    // Note if we've just logged in we show gifts rather setting up the listener,
                    // this is to allow us to prevent updating the local coins values with gifts
                    // we recieve on login, as we will already have the up to date values downloaded
                    settingUpUserGiftListener = true;
                    showGifts();
                }
            } else {
                System.out.println("FAILED TO GET WHETHER USER IS LOGGED IN");
            }
        });
    }

    private void setFirstTimeUser() {
        WriteBatch batch = db.batch();

        DocumentReference userDocRef = db.collection(users).document(uid);
        Map<String, Object> user_defaults = new HashMap<>();
        user_defaults.put("username", "");
        for (String currency : currencies) {
            user_defaults.put(currency, 0.0);
        }
        user_defaults.put("coinsRemainingToday", 0.0);
        user_defaults.put("map", "");
        user_defaults.put("lastDownloadDate", LocalDate.MIN.toString());
        batch.set(userDocRef, user_defaults);

        // We store gifts in a separate document to make listening for changes simpler
        DocumentReference userGiftDocRef = db.collection(users_gifts).document(uid);
        Map<String, Object> user_gift_defaults = new HashMap<>();
        user_gift_defaults.put("gifts", new ArrayList<String>());
        batch.set(userGiftDocRef, user_gift_defaults);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("SUCCESSFULLY ADDED USER TO DATABASE");
                    setDefaultFragment();
                    settingUpUserGiftListener = true;
                    showGifts();
                })
                .addOnFailureListener(e -> System.out.println("FAILED TO ADD USER TO DATABASE"));
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
        runOnUiThread(() -> Toast.makeText(activity, "Could not connect to the internet, progress offline will be updated in the cloud when connection is established.", Toast.LENGTH_LONG).show());
        waitingToUpdateCoins = true;
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
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReciever, intentFilter);
        // If onStart called, then that means onStop was called prior to it, meaning the timer will
        // have been purged, meaning we need to restart the timer for looking for the internet, which
        // we do below
        if (waitingToUpdateCoins) {
            setToUpdateCoinsOnInternet();
        }
        if (coinsUpdateExecutor.isShutdown()) {
            coinsUpdateExecutor = Executors.newFixedThreadPool(1);
        }
        if (!justLoggedIn) {
            setUpListenerForGifts();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(networkChangeReciever);
        if (userGiftListener != null) {
            userGiftListener.remove();
        }
        coinsUpdateExecutor.shutdown();
        if (userGiftListener != null) {
            userGiftListener.remove();
            userGiftListener = null;
        }
    }

    @Override
    public void onCoinsUpdateWithDeltaComplete(long lockStamp) {
        waitingToUpdateCoins = false;
        runOnUiThread(() -> Toast.makeText(activity, "Progress offline has been updated in the cloud.", Toast.LENGTH_LONG).show());
        settingsWriteLock.unlockWrite(lockStamp);
    }

    private void showGifts() {

        final DocumentReference usernamesDocRef = db.collection(users).document("usernames");
        usernamesDocRef
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // First we get the list of usernames
                        DocumentSnapshot usernames = task.getResult();
                        if (usernames.exists()) {
                            System.out.println("GOT USERNAMES DOC");
                            final DocumentReference userGiftsDocRef = db.collection(users_gifts).document(uid);
                            // We show all the gifts already stored
                            System.out.println("GETTING NEW GIFTS");
                            db.runTransaction(transaction -> {
                                // The first step to showing them is to get them from the database
                                DocumentSnapshot userGiftsSnapshot = transaction.get(userGiftsDocRef);
                                // As we have a username document we use this to get usernames rather than from each
                                // users document, this is because the username document is less likely to be updated
                                // mid-transaction

                                Object giftsObj = userGiftsSnapshot.get("gifts");
                                if (giftsObj != null) {
                                    try {
                                        // We suppress warning here as impossible to fail, but IDE gives a warning falsely about unchecked cast
                                        @SuppressWarnings("unchecked")
                                        ArrayList<String> giftIDs = (ArrayList<String>) userGiftsSnapshot.get("gifts");

                                        ArrayList<String[]> giftDetails = new ArrayList<>();

                                        ArrayList<DocumentReference> giftDocRefs = new ArrayList<>();

                                        // We assert giftIDs is not null, as the checks above ensure it isn't
                                        assert giftIDs != null;
                                        for (String giftID : giftIDs) {
                                            DocumentReference giftDocRef = db.collection(gifts).document(giftID);
                                            DocumentSnapshot giftSnapshot = transaction.get(giftDocRef);
                                            String senderUid = giftSnapshot.getString("senderUid");
                                            Double giftAmount = giftSnapshot.getDouble("amount");
                                            if (senderUid != null & giftAmount != null) {
                                                giftDocRefs.add(giftDocRef);
                                                String senderName = usernames.getString(senderUid);
                                                String currency = giftSnapshot.getString("currency");
                                                String amount = Double.toString(giftAmount);
                                                giftDetails.add(new String[]{senderName, currency, amount});
                                            } else {
                                                System.out.println("EXPECTED SENDER UID AND GIFT AMOUNT TO BE NON-NULL, BUT AT LEAST ONE IS NULL");
                                            }
                                        }

                                        for (DocumentReference giftDocRef : giftDocRefs) {
                                            transaction.delete(giftDocRef);
                                        }

                                        Map<String, Object> usersGiftsDetails = new HashMap<>();
                                        usersGiftsDetails.put("gifts", new ArrayList<String>());
                                        transaction.set(userGiftsDocRef, usersGiftsDetails);
                                        return giftDetails;
                                    } catch (ClassCastException e) {
                                        throw new FirebaseFirestoreException("GIFTS IS NOT AN ARRAY LIST",
                                                FirebaseFirestoreException.Code.ABORTED);
                                    }
                                } else {
                                    throw new FirebaseFirestoreException("COULD NOT GET GIFTS FROM USERS-GIFTS DOC FOR JIM",
                                            FirebaseFirestoreException.Code.ABORTED);
                                }
                            }).addOnSuccessListener(giftDetails -> {
                                // Once we've gotten the current batch of gifts we set up a listener to listen for new gifts
                                settingUpUserGiftListener = false;
                                justUpdated = true;
                                setUpListenerForGifts();
                                // After that we show the gifts we have just fetched
                                if (giftDetails.size() > 0) {
                                    HashMap<String, Double> currencyChanges = new HashMap<>();
                                    for (String[] giftDetail : giftDetails) {
                                        String senderName = giftDetail[0];
                                        String currency = giftDetail[1];
                                        double amount = Double.parseDouble(giftDetail[2]);
                                        runOnUiThread(() -> Toast.makeText(activity, String.format("Recieved %s %s from %s", amount, currency, senderName), Toast.LENGTH_LONG).show());
                                        if (!justLoggedIn) {
                                            currencyChanges.put(currency, currencyChanges.getOrDefault(currency, 0.0) + amount);
                                        }
                                    }
                                    if (!justLoggedIn) {
                                        coinsUpdateExecutor.submit(new CoinsUpdateTask(activity, settingsWriteLock, settings, currencyChanges));
                                    }
                                }
                                if (justLoggedIn) {
                                    justLoggedIn = false;
                                }
                            }).addOnFailureListener(e -> {
                                System.out.printf("TRANSACTION FOR RECEIVING GIFTS FAILED WITH EXCEPTION:\n%s\n", e.getMessage());
                                settingUpUserGiftListener = false;
                            });
                        } else {
                            System.out.println("COULD NOT FIND USERNAMES DOC");
                            settingUpUserGiftListener = false;
                        }
                    } else {
                        System.out.printf("GETTING USERNAMES DOC FOR SHOWING GIFTS FAILED WITH EXCEPTION:\n%s\n", task.getException());
                        settingUpUserGiftListener = false;
                    }
                });
    }

    private void setUpListenerForGifts() {

        if (!uid.equals("") && !settingUpUserGiftListener && userGiftListener == null && !waitingToListenForGifts) {

            settingUpUserGiftListener = true;

            if (isNetworkAvailable()) {
                final DocumentReference userGiftsDocRef = db.collection(users_gifts).document(uid);

                userGiftListener = userGiftsDocRef.addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        System.out.printf("LISTENING FOR GIFTS FAILED WITH EXCEPTION:\n%s\n", e.getMessage());
                        userGiftListener = null;
                        return;
                    }

                    // We use justUpdated to prevent checking for changes when a gift has just been
                    // shown, as I found this could lead to infinite loops

                    if (!justUpdated) {

                        if (snapshot != null && snapshot.exists()) {
                            System.out.println("FOUND NEW GIFTS");
                            Object giftsObj = snapshot.get("gifts");
                            if (giftsObj != null) {
                                try {
                                    // We suppress warning here as impossible to fail, but IDE gives a warning falsely about unchecked cast
                                    @SuppressWarnings("unchecked")
                                    ArrayList<String> gifts = (ArrayList<String>) snapshot.get("gifts");
                                    // We can require non-null here as above ensures gifts will never be null
                                    // for this code to be executed
                                    if (Objects.requireNonNull(gifts).size() > 0) {
                                        settingUpUserGiftListener = true;
                                        userGiftListener.remove();
                                        userGiftListener = null;
                                        showGifts();
                                    }
                                } catch (ClassCastException err) {
                                    System.out.println("GIFTS IS NOT AN ARRAY LIST");
                                }
                            } else {
                                System.out.println("COULD NOT GET GIFTS FROM USERS-GIFTS DOC FOR JIM");
                            }
                        } else {
                            System.out.println("USERS GIFT DOCUMENT IS NULL");
                        }

                    } else {
                        justUpdated = false;
                    }
                });

                settingUpUserGiftListener = false;
            } else {
                settingUpUserGiftListener = false;
                waitingToListenForGifts = true;
            }
        }
    }

    @Override
    public void coinsUpdateTaskComplete() {
        if (navigationView.getMenu().findItem(R.id.nav_balance).isChecked()) {
            ((BalanceFragment) currentFragment).coinsUpdateTaskComplete();
        }
    }

    public LocalDate localDateNow() {
        LocalDate day;
        if (tester) {
            day = fixedTestDay;
        } else {
            day = LocalDate.now();
        }
        return day;
    }

    private void setupIfTester() {
        if (Arrays.stream(testerUids).anyMatch(uid::equals)) {
            users = "users-test";
            gifts = "gifts-test";
            users_gifts = "users_gifts-test";
            tester = true;
            nextDayBtn.setEnabled(true);
            nextDayBtn.setVisibility(View.VISIBLE);
            flipInternetStateBtn.setEnabled(true);
            flipInternetStateBtn.setVisibility(View.VISIBLE);
        } else {
            users = "users";
            gifts = "gifts";
            users_gifts = "users_gifts";
            tester = false;
            nextDayBtn.setEnabled(false);
            nextDayBtn.setVisibility(View.GONE);
            flipInternetStateBtn.setEnabled(false);
            flipInternetStateBtn.setVisibility(View.GONE);
        }
    }
}