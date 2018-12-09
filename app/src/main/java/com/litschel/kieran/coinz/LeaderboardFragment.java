package com.litschel.kieran.coinz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

public class LeaderboardFragment extends Fragment {

    private FirebaseFirestore db;
    private String uid;
    private String username;
    private TableRow[] rows;
    private TextView userRank;
    private TextView[] names;
    private TextView[] golds;
    private TextView top10Txt;
    private FloatingActionButton refreshFAB;
    private String users;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        db = ((MainActivity) Objects.requireNonNull(getActivity())).db;
        uid = ((MainActivity) getActivity()).uid;
        username = ((MainActivity) getActivity()).settings.getString("username", "");
        users = ((MainActivity) getActivity()).users;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // Put the view components in arrays so we can set them using loops

        rows = new TableRow[]{
                view.findViewById(R.id.YouRow),
                view.findViewById(R.id.UserHeaderRow),
                view.findViewById(R.id.UserRow),
                view.findViewById(R.id.Top10Row),
                view.findViewById(R.id.HeaderRow),
                view.findViewById(R.id.FirstRow),
                view.findViewById(R.id.SecondRow),
                view.findViewById(R.id.ThirdRow),
                view.findViewById(R.id.FourthRow),
                view.findViewById(R.id.FifthRow),
                view.findViewById(R.id.SixthRow),
                view.findViewById(R.id.SeventhRow),
                view.findViewById(R.id.EighthRow),
                view.findViewById(R.id.NinthRow),
                view.findViewById(R.id.TenthRow)};

        userRank = view.findViewById(R.id.UserRank);

        names = new TextView[]{
                view.findViewById(R.id.UserName),
                view.findViewById(R.id.FirstName),
                view.findViewById(R.id.SecondName),
                view.findViewById(R.id.ThirdName),
                view.findViewById(R.id.FourthName),
                view.findViewById(R.id.FifthName),
                view.findViewById(R.id.SixthName),
                view.findViewById(R.id.SeventhName),
                view.findViewById(R.id.EighthName),
                view.findViewById(R.id.NinthName),
                view.findViewById(R.id.TenthName)};

        golds = new TextView[]{
                view.findViewById(R.id.UserGold),
                view.findViewById(R.id.FirstGold),
                view.findViewById(R.id.SecondGold),
                view.findViewById(R.id.ThirdGold),
                view.findViewById(R.id.FourthGold),
                view.findViewById(R.id.FifthGold),
                view.findViewById(R.id.SixthGold),
                view.findViewById(R.id.SeventhGold),
                view.findViewById(R.id.EighthGold),
                view.findViewById(R.id.NinthGold),
                view.findViewById(R.id.TenthGold)};

        top10Txt = view.findViewById(R.id.Top10Text);

        refreshFAB = view.findViewById(R.id.refreshButton);
        refreshFAB.setOnClickListener(view1 -> refreshLeaderboard());

        refreshLeaderboard();
    }

    // This suppresses the warning from not using Locale, which we discuss why we don't use in the code for the exchange dialog fragment
    @SuppressLint("SetTextI18n")
    private void refreshLeaderboard() {
        refreshFAB.setEnabled(false);
        if (getActivity() != null) {
            if (getActivity().getClass() == MainActivity.class) {
                // We require a network connection to get the database
                if (((MainActivity) getActivity()).isNetworkAvailable()) {
                    // We require the user to have create a username for themself so we can show their
                    // position on the leaderboard
                    if (!((MainActivity) getActivity()).settings.getString("username", "").equals("")) {
                        // Hide all rows, as we won't show some if there are less than 10 users being displayed
                        // on the leaderboard
                        for (TableRow row : rows) {
                            row.setVisibility(View.GONE);
                        }
                        // Get all users documents
                        db.collection(users)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Put all users into users usernames and gold counts into an array
                                        ArrayList<UserNdGold> userNdGolds = new ArrayList<>();
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            String otherUsername = document.getString("username");
                                            if (otherUsername == null) {
                                                // This is necessary as it is not guarenteed every document in users has a username, e.g. the usernames document
                                                continue;
                                            }
                                            Double gold = document.getDouble("GOLD");
                                            if (gold == null) {
                                                System.out.println("NO GOLD VALUE DECLARED FOR USER " + otherUsername);
                                                continue;
                                            }
                                            if (!otherUsername.equals("")) {
                                                userNdGolds.add(new UserNdGold(otherUsername, gold));
                                            }
                                        }
                                        // Sort the list into descending order by gold values
                                        userNdGolds.sort(Comparator.comparing(UserNdGold::getGold).reversed());
                                        // Find the users position in the list and their stats
                                        int currUserRank = -1;
                                        UserNdGold currUser = new UserNdGold("", 0);
                                        for (int i = 0; i < userNdGolds.size(); i++) {
                                            UserNdGold user = userNdGolds.get(i);
                                            if (user.getUsername().equals(username)) {
                                                currUserRank = i + 1;
                                                currUser = user;
                                                break;
                                            }
                                        }
                                        // At maxmimum we will show the top 10, but if there's less
                                        // than 10 people in the database we'll show the top n
                                        if (userNdGolds.size() < 10) {
                                            top10Txt.setText(String.format("Top %s", userNdGolds.size()));
                                        } else {
                                            top10Txt.setText(R.string.top_10);
                                        }
                                        // Set the title and field headers for the main table to visible
                                        rows[3].setVisibility(View.VISIBLE);
                                        rows[4].setVisibility(View.VISIBLE);
                                        // Set the rows for the top 10, or top n if there are less than
                                        // 10 users in the database
                                        for (int i = 0; i < Math.min(userNdGolds.size(), 10); i++) {
                                            UserNdGold user = userNdGolds.get(i);
                                            names[i + 1].setText(user.getUsername());
                                            golds[i + 1].setText(user.getGoldStr());
                                            rows[i + 5].setVisibility(View.VISIBLE);
                                        }
                                        // If there's more than 10 users in the database we show the
                                        // user their place on the database
                                        if (currUserRank > 10) {
                                            userRank.setText(Integer.toString(currUserRank));
                                            names[0].setText(currUser.getUsername());
                                            golds[0].setText(currUser.getGoldStr());
                                            for (int i = 0; i < 3; i++) {
                                                rows[i].setVisibility(View.VISIBLE);
                                            }
                                        }
                                    } else {
                                        System.out.printf("GETTING LEADERBOARD FAILED WITH EXCEPTION:\n%s\n",
                                                task.getException());
                                        Toast.makeText(getActivity(), "Something went wrong when downloading the leaderboard, please check your internet connection and try again.", Toast.LENGTH_LONG).show();
                                    }
                                    refreshFAB.setEnabled(true);
                                });
                    } else {
                        // If the users username isn't set we show them the create username dialog
                        DialogFragment newFragment = new ChangeUsernameDialogFragment();
                        Bundle args = new Bundle();
                        args.putBoolean("isNewUser", true);
                        args.putBoolean("isLeaderboard", true);
                        args.putString("username", username);
                        args.putString("uid", uid);
                        newFragment.setArguments(args);
                        // We suppress the warning about getActivity being null, as we ensure it's non-null before this is run
                        newFragment.show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "create_username_dialog");
                        refreshFAB.setEnabled(true);
                    }
                } else {
                    Toast.makeText(getActivity(), "You require an internet connection to update the leaderboard. Please connect to the internet and try again.", Toast.LENGTH_LONG).show();
                    refreshFAB.setEnabled(true);
                }
            } else {
                System.out.println("ACTIVITY CLASS WAS EXPECTED TO BE MAIN ACTIVITY BUT ISN'T");
                refreshFAB.setEnabled(true);
            }
        } else {
            System.out.println("ACTIVITY WAS NULL WHEN EXPECTED NON-NULL");
            refreshFAB.setEnabled(true);
        }
    }

    // We use this object to store details about the user in the array list, we use objects and an
    // array list as opposed to a hashmap so that we can sort the list
    class UserNdGold {
        private String username;
        private double gold;

        UserNdGold(String username, double gold) {
            this.username = username;
            this.gold = gold;
        }

        public String getUsername() {
            return username;
        }

        public double getGold() {
            return gold;
        }

        String getGoldStr() {
            String goldStr;
            if (gold >= Math.pow(10, 9)) {
                goldStr = String.format(Locale.getDefault(), "%.3f B", gold / Math.pow(10, 9));
            } else if (gold >= Math.pow(10, 6)) {
                goldStr = String.format(Locale.getDefault(), "%.3f M", gold / Math.pow(10, 6));
            } else if (gold >= Math.pow(10, 3)) {
                goldStr = String.format(Locale.getDefault(), "%.3f K", gold / Math.pow(10, 3));
            } else {
                goldStr = String.format(Locale.getDefault(), "%.3f", gold);
            }
            return goldStr;
        }
    }
}
