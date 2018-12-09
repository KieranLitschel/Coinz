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
    // We use this to keep track of which fragment is being displayed (as selected in the navigation
    // view) so that we can interact with it
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
    private FloatingActionButton flipInternetStateBtn;
    private FloatingActionButton nextDayBtn;
    private LocalDate fixedTestDay = LocalDate.of(2018, 12, 1);
    private boolean justLoggedIn;
    private boolean justShowedGifts;

    // This method creates the main activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // We ensure we have the required permissions, and if not we request them

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

        // We create a FixedThreadPool to save time having to create new threads, we only use a single
        // executor as all threads will need to acquire the settingsWriteLock, so no threads can be
        // run in parallel

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

        // The following two FABs will only be visible and enabled for testers

        // This button mimicks the day changing, allowing us to unit test handling day changes
        nextDayBtn = findViewById(R.id.changeDayButton);
        nextDayBtn.setOnClickListener(view -> {
            fixedTestDay = fixedTestDay.plusDays(1);
            if (currentFragment.getClass() == MapFragment.class) {
                ((MapFragment) currentFragment).checkForMapUpdate();
            }
        });
        // This button mimicks the internet being enabled/disabled, allowing us to test how the app
        // behaves when offline
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

        // Below we setup how the navigation drawer behaves when items are selected

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    int itemId = menuItem.getItemId();

                    if (!navigationView.getMenu().findItem(menuItem.getItemId()).isChecked()) {
                        Class fragmentClass = null;

                        // We match the selected item to the corresponding fragment

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
                                // In the case the user wants to logout there is no associated
                                // fragment, so we just log them out
                                logout();
                                break;
                            default:
                                fragmentClass = MapFragment.class;
                        }

                        // If we use logout we don't want the currently displayed fragment to change
                        if (itemId != R.id.nav_logout) {
                            // We try and create a new instance of the fragment that corresponds
                            // to the selected item
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

        // Check if the user needs to be signed in, and if so bring up the sign in window
        signIn();
    }

    // This will be triggered whenever the network changes
    private BroadcastReceiver networkChangeReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onNetworkChange();
        }
    };

    // These are the actions we perform when the network changes
    private void onNetworkChange() {
        if (isNetworkAvailable()) {
            // If we were waiting to set up the gift listener then we set it up
            if (waitingToListenForGifts) {
                waitingToListenForGifts = false;
                setUpListenerForGifts();
            }
            if (currentFragment != null) {
                // In rare cases initial map setup may not complete if the user loses internet connection
                // between logging in and the map fragment being created, this ensures that when they
                // next connect to the internet the map is setup
                if (currentFragment.getClass() == MapFragment.class && waitingToInitialMapSetup) {
                    System.out.println("FOUND INTERNET CHECKING FOR INITIAL MAP SETUP");
                    waitingToInitialMapSetup = false;
                    ((MapFragment) currentFragment).initialMapSetup();
                }
                // If we were waiting to download todays map then we check if we still need to download it,
                // we only do this if the map is being displayed as otherwise it will be downloaded next
                // time the map opens
                if (currentFragment.getClass() == MapFragment.class && waitingToUpdateMap) {
                    System.out.println("FOUND INTERNET CHECKING FOR MAP UPDATE");
                    waitingToUpdateMap = false;
                    ((MapFragment) currentFragment).checkForMapUpdate();
                }
            }
            // If the coinDelta values in the shared preferences file are non-zero then this will
            // be triggered, causing the deltas to be updated in the database
            if (waitingToUpdateCoins) {
                waitingToUpdateCoins = false;
                coinsUpdateExecutor.submit(new CoinsUpdateWithDeltaTask(users, activity, db, settings, settingsWriteLock, uid));
            }
        } else {
            // If we suddenly lose interent then the gift listener will fail, so we need to set it
            // up to
            settingUpUserGiftListener = false;
            waitingToListenForGifts = true;
            if (userGiftListener != null) {
                userGiftListener.remove();
            }
            userGiftListener = null;
        }
    }

    // This method checks whether there is internet available
    public boolean isNetworkAvailable() {
        if (tester && !testerInternet) {
            // If the user is a tester and the testerInternet is disabled, then we say there is no internet
            // which will lead the app to behave as if offline
            return false;
        } else {
            // Otherwise we check for an internet connection and return the result
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

    // This is used for initial setup, setting the default fragment to the map
    private void setDefaultFragment() {
        // Set default fragment to the map, as we do if the map is selected in the navigation view

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

    // This method handles signing the user in
    private void signIn() {
        // If the users uid was read as empty, then the user is not signed in
        if (uid.equals("")) {
            if (isNetworkAvailable()) {
                // If there's an internet connection we sign them in using the default implemention
                // for Firebase Authentication
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
                // If there's no internet then we bring up the dialog to inform them
                DialogFragment noInternetFragment = new NoInternetDialogFragment();
                // We set cancelable to false so they can't dismiss the dialog, forcing them to click
                // the positive or negative button
                noInternetFragment.setCancelable(false);
                noInternetFragment.show(getSupportFragmentManager(), "no_internet_dialog");
            }
        } else {
            // If the users signed in, we check if they have any non-zero deltas, if so then these
            // need to be updated when we connect to the internet, so we set waitingToUpdateCoins to true
            for (String currency : currencies) {
                if (!settings.getString(currency + "Delta", "").equals("")) {
                    if (isNetworkAvailable()) {
                        coinsUpdateExecutor.submit(new CoinsUpdateWithDeltaTask(users, activity, db, settings, settingsWriteLock, uid));
                    } else {
                        setToUpdateCoinsOnInternet();
                    }
                    break;
                }
            }
            setDefaultFragment();
        }
    }

    // This method will be executed when the user finishes signing in through the Firebase Authenticator
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If this method was triggered from a signIn, then we handle the result of the sign in
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // If this is the case we signed in successfully
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    // When only using one sign in method (in this case email), the uid for each user is unique
                    // so we can use this to uniquely identify them in the database
                    uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    justLoggedIn = true;
                    setupIfTester();
                    // We save their uid locally for future reference
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("uid", uid);
                    editor.apply();
                    // We check whether there a first time user, in which case we'll need to create
                    // their document in the database
                    checkFirstTimeUser();
                } else {
                    System.out.println("FIREBASEAUTH RETURN NULL USER WHEN EXPECTED TO BE NON-NULL");
                }
                // ...
            } else {
                // If the response was null then something went wrong, so we warn the user they
                // failed to sign in
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
                    // Otherwise they dismissed the dialog, in which case we inform them they must
                    // be signed in to use the app and show them the dialog again
                    Toast.makeText(this, "You must sign in to use this app.", Toast.LENGTH_LONG)
                            .show();
                    signIn();
                }
            }
        }
    }

    // This checks whether this is the first time the user is using the app
    private void checkFirstTimeUser() {
        // This points to the users document
        DocumentReference usersDocRef = db.collection(users).document(uid);
        System.out.println("CHECKING IF USER EXISTS IN DATABASE");
        usersDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (!document.exists()) {
                    // If their document does not exist, it is their first time, so we need to set them
                    // up in the database
                    System.out.println("FIRST TIME USER, SETTING DEFAULTS");
                    setFirstTimeUser();
                } else {
                    // If their document does exist we don't need to do anymore, so we show them
                    // the map
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

    // This sets up a first time user in the database
    private void setFirstTimeUser() {
        // We are going to write several documents, so use a WriteBatch to make this more efficient
        WriteBatch batch = db.batch();

        // We create the default document for the users details
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

        // We create the default layout for the users gifts document
        // Note we store gifts in a separate document to make listening for changes more efficient, as otherwise
        // the listener will trigger every time a coin is collected
        DocumentReference userGiftDocRef = db.collection(users_gifts).document(uid);
        Map<String, Object> user_gift_defaults = new HashMap<>();
        user_gift_defaults.put("gifts", new ArrayList<String>());
        batch.set(userGiftDocRef, user_gift_defaults);

        // We write the documents to the database
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Once we've written the users documents to the database we can show them the map
                    System.out.println("SUCCESSFULLY ADDED USER TO DATABASE");
                    setDefaultFragment();
                    // We do this rather than setting up the gift listner for the same reason described
                    // in checkFirstTimeUser
                    settingUpUserGiftListener = true;
                    showGifts();
                })
                .addOnFailureListener(e -> System.out.println("FAILED TO ADD USER TO DATABASE"));
    }

    // Logs the user out
    private void logout() {
        // We destroy the current fragment as the data relates to the user being logged out
        fragmentManager.beginTransaction().remove(currentFragment).commit();
        // We delete all details stored locally about the user
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();
        uid = "";
        // We set waiting for all these things to false as they relate to the previous user
        waitingToUpdateCoins = false;
        waitingToInitialMapSetup = false;
        waitingToUpdateMap = false;
        waitingToListenForGifts = false;
        // Ask for user to sign in
        signIn();
    }

    // Sets that we need to update coins on internet and displays a toast to warn the user
    public void setToUpdateCoinsOnInternet() {
        runOnUiThread(() -> Toast.makeText(activity, "Could not connect to the internet, progress offline will be updated in the cloud when connection is established.", Toast.LENGTH_LONG).show());
        waitingToUpdateCoins = true;
    }

    // This just ensures that if we click on the drawer, it opens
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // This is a callback from the NoInternetDialogFragment, which asks the app to try signing in
    // the user again
    @Override
    public void tryInternetAgain() {
        signIn();
    }

    // This is another callback from the NoInternetDialogFragment, which closes the app if the
    // user decided not to try the internet again
    @Override
    public void closeApp() {
        finish();
        System.exit(0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // We setup the networkChangeReciever to be triggered if the connection changes
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReciever, intentFilter);
        // If the coinUpdateExecutor is shutdown, we restart it
        if (coinsUpdateExecutor.isShutdown()) {
            coinsUpdateExecutor = Executors.newFixedThreadPool(1);
        }
        // If the user has not just logged in then we need to check if we need to setup the gift listener
        if (!justLoggedIn) {
            setUpListenerForGifts();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister the gift listnener so it is not triggered when the app isn't in focus
        unregisterReceiver(networkChangeReciever);
        // Shut down the executor to save resources
        coinsUpdateExecutor.shutdown();
        // Shut down the gift listener to save resources
        if (userGiftListener != null) {
            userGiftListener.remove();
            userGiftListener = null;
        }
    }

    // This is the callback from the CoinsUpdateWithDeltaTask
    @Override
    public void onCoinsUpdateWithDeltaComplete(long lockStamp) {
        waitingToUpdateCoins = false;
        runOnUiThread(() -> Toast.makeText(activity, "Progress offline has been updated in the cloud.", Toast.LENGTH_LONG).show());
        settingsWriteLock.unlockWrite(lockStamp);
    }

    // This informs the user who they've recieved gifts from
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
                            // We use a transaction here in case any gifts come in while we're reading them
                            // to prevent us overwritting them when we clear the users gifts array
                            db.runTransaction(transaction -> {
                                // The first step to showing them is to get them from the database
                                DocumentSnapshot userGiftsSnapshot = transaction.get(userGiftsDocRef);

                                Object giftsObj = userGiftsSnapshot.get("gifts");
                                if (giftsObj != null) {
                                    try {
                                        // We suppress warning here as impossible to fail, but IDE gives a warning falsely about unchecked cast
                                        @SuppressWarnings("unchecked")

                                        // We get the array of giftIds from the document
                                                ArrayList<String> giftIDs = (ArrayList<String>) userGiftsSnapshot.get("gifts");

                                        ArrayList<String[]> giftDetails = new ArrayList<>();

                                        ArrayList<DocumentReference> giftDocRefs = new ArrayList<>();

                                        // We assert giftIDs is not null, as the checks above ensure it isn't
                                        assert giftIDs != null;

                                        for (String giftID : giftIDs) {
                                            // For each giftID we get the corresponding gift document in the gifts collection
                                            DocumentReference giftDocRef = db.collection(gifts).document(giftID);
                                            DocumentSnapshot giftSnapshot = transaction.get(giftDocRef);
                                            String senderUid = giftSnapshot.getString("senderUid");
                                            Double giftAmount = giftSnapshot.getDouble("amount");
                                            if (senderUid != null & giftAmount != null) {
                                                // We don't delete the read gifts here as all reads must
                                                // come before writes in a transaction, so we just keep
                                                // track of the gifts we have read
                                                giftDocRefs.add(giftDocRef);
                                                // We extract the details from the gift and store them
                                                // in an array we can pass to the on success listener
                                                String senderName = usernames.getString(senderUid);
                                                String currency = giftSnapshot.getString("currency");
                                                String amount = Double.toString(giftAmount);
                                                giftDetails.add(new String[]{senderName, currency, amount});
                                            } else {
                                                throw new FirebaseFirestoreException("EXPECTED SENDER UID AND GIFT AMOUNT TO BE NON-NULL, BUT AT LEAST ONE IS NULL",
                                                        FirebaseFirestoreException.Code.ABORTED);
                                            }
                                        }

                                        // Now that we have read all the documents we need to, we can
                                        // delete the gifts we have read
                                        for (DocumentReference giftDocRef : giftDocRefs) {
                                            transaction.delete(giftDocRef);
                                        }

                                        // Finally we put an empty array back for gifts, as we have
                                        // read all the gifts from the array
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
                                justShowedGifts = true;
                                setUpListenerForGifts();
                                // After that we show the gifts we have just fetched
                                if (giftDetails.size() > 0) {
                                    // We prevent local values being updated if the user has justLoggedIn,
                                    // as in this case the values will be up to date as they've just been downloaded
                                    // from the database
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

    // This method sets up the listener for gifts
    private void setUpListenerForGifts() {

        // We add the logic below to prevent multiple listeners being created and potential sources of errors

        // If the UID is not set then this indicates the user has not logged in yet, so we need to
        // wait to seet up the gift listener, settingUpGiftListener is true if we're showing gifts,
        // so we don't set up a gift listener in this case as it will be setup after we've shown the gifts,
        // if userGiftListener is not null then a gift listener already exists, so we don't need to create
        // a new one, if we're waiting to listnen for gifts then there's no internet, so the gift listener
        // will be setup when there is internet
        if (!uid.equals("") && !settingUpUserGiftListener && userGiftListener == null && !waitingToListenForGifts) {

            settingUpUserGiftListener = true;

            if (isNetworkAvailable()) {
                final DocumentReference userGiftsDocRef = db.collection(users_gifts).document(uid);

                // We set up a document listener on the users gifts document so that we can react
                // to gifts being recieved in realtime
                userGiftListener = userGiftsDocRef.addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        System.out.printf("LISTENING FOR GIFTS FAILED WITH EXCEPTION:\n%s\n", e.getMessage());
                        userGiftListener = null;
                        return;
                    }

                    // We use justShowedGifts to prevent checking for changes when a gift has just been
                    // shown, as I found this could lead to infinite loops

                    if (!justShowedGifts) {

                        if (snapshot != null && snapshot.exists()) {
                            System.out.println("FOUND NEW GIFTS");
                            Object giftsObj = snapshot.get("gifts");
                            if (giftsObj != null) {
                                try {
                                    // We suppress warning here as impossible to fail, but IDE gives a warning falsely about unchecked cast
                                    @SuppressWarnings("unchecked")
                                    ArrayList<String> gifts = (ArrayList<String>) snapshot.get("gifts");

                                    // We make sure that there are gifts in the array before showing the
                                    // gifts, as I found the listener is sometimes triggered when gifts
                                    // have not been added to the array, so this logic prevents unnecessary
                                    // database queries

                                    // We can require non-null here as above ensures gifts will never be null
                                    // for this code to be executed

                                    if (Objects.requireNonNull(gifts).size() > 0) {
                                        // If there
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
                        justShowedGifts = false;
                    }
                });

                settingUpUserGiftListener = false;
            } else {
                // If there's no internet avaialable we mark that the listener should be setup when
                // it is available
                settingUpUserGiftListener = false;
                waitingToListenForGifts = true;
            }
        }
    }

    // This is called back from the CoinsUpdateTask, it ensures that if we're viewing the balance
    // the values are updated to show the changes just just made
    @Override
    public void coinsUpdateTaskComplete() {
        if (navigationView.getMenu().findItem(R.id.nav_balance).isChecked()) {
            ((BalanceFragment) currentFragment).coinsUpdateTaskComplete();
        }
    }

    // We use this method throughout the app to get the date, this is so that we can spoof the date
    // for testers, and allow them to change it via the app
    public LocalDate localDateNow() {
        LocalDate day;
        if (tester) {
            day = fixedTestDay;
        } else {
            day = LocalDate.now();
        }
        return day;
    }

    // This checks if a user is a tester, in which case we set them up with the testing version of
    // the app
    private void setupIfTester() {
        if (Arrays.stream(testerUids).anyMatch(uid::equals)) {
            // If they're a tester

            // We point to the collections for testing, so we can keep testers seperate from the user
            // database so that tests are repeatable
            users = "users-test";
            gifts = "gifts-test";
            users_gifts = "users_gifts-test";
            tester = true;
            // We show and enable the two FABs which are only available to testers
            nextDayBtn.setEnabled(true);
            nextDayBtn.setVisibility(View.VISIBLE);
            flipInternetStateBtn.setEnabled(true);
            flipInternetStateBtn.setVisibility(View.VISIBLE);
        } else {
            // If they're not a tester

            // We point them to the regular database
            users = "users";
            gifts = "gifts";
            users_gifts = "users_gifts";
            tester = false;
            // We make sure the tester buttons are completely gone from the UI and disable them to be safe
            nextDayBtn.setEnabled(false);
            nextDayBtn.setVisibility(View.GONE);
            flipInternetStateBtn.setEnabled(false);
            flipInternetStateBtn.setVisibility(View.GONE);
        }
    }
}