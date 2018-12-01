package com.litschel.kieran.coinz;

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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.lang.Math;

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        db = ((MainActivity) Objects.requireNonNull(getActivity())).db;
        uid = ((MainActivity) getActivity()).uid;
        username = ((MainActivity) getActivity()).settings.getString("username", "");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
        refreshFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshLeaderboard();
            }
        });

        refreshLeaderboard();
    }

    private void refreshLeaderboard() {
        refreshFAB.setEnabled(false);
        if (((MainActivity) getActivity()).isNetworkAvailable()) {
            if (!((MainActivity) getActivity()).settings.getString("username", "").equals("")) {
                for (TableRow row : rows) {
                    row.setVisibility(View.GONE);
                }
                db.collection("users")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    ArrayList<UserNdGold> userNdGolds = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String otherUsername = document.getString("username");
                                        if (otherUsername == null) {
                                            // This is necessary as it is not guarenteed every document in users has a username, e.g. the usernames document
                                            continue;
                                        }
                                        double gold = document.getDouble("GOLD");
                                        if (!otherUsername.equals("")) {
                                            userNdGolds.add(new UserNdGold(otherUsername, gold));
                                        }
                                    }
                                    // Sort the list into descending order
                                    userNdGolds.sort(new Comparator<UserNdGold>() {
                                        @Override
                                        public int compare(UserNdGold t1, UserNdGold t2) {
                                            if (t1.getGold() < t2.getGold()) {
                                                return 1;
                                            }
                                            if (t1.getGold() > t2.getGold()) {
                                                return -1;
                                            }
                                            return 0;
                                        }
                                    });
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
                                    if (userNdGolds.size() < 10) {
                                        top10Txt.setText(String.format("Top %s", userNdGolds.size()));
                                    } else {
                                        top10Txt.setText(R.string.top_10);
                                    }
                                    rows[3].setVisibility(View.VISIBLE);
                                    rows[4].setVisibility(View.VISIBLE);
                                    for (int i = 0; i < Math.min(userNdGolds.size(), 10); i++) {
                                        UserNdGold user = userNdGolds.get(i);
                                        names[i + 1].setText(user.getUsername());
                                        golds[i + 1].setText(user.getGoldStr());
                                        rows[i + 5].setVisibility(View.VISIBLE);
                                    }
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
                                            task.getException().getMessage());
                                    Toast.makeText(getActivity(), "Something went wrong when downloading the leaderboard, please check your internet connection and try again.", Toast.LENGTH_LONG).show();
                                }
                                refreshFAB.setEnabled(true);
                            }
                        });
            } else {
                DialogFragment newFragment = new ChangeUsernameDialogFragment();
                Bundle args = new Bundle();
                args.putBoolean("isNewUser", true);
                args.putBoolean("isLeaderboard", true);
                args.putString("username", username);
                args.putString("uid", uid);
                newFragment.setArguments(args);
                newFragment.show(getActivity().getSupportFragmentManager(), "create_username_dialog");
                refreshFAB.setEnabled(true);
            }
        } else {
            Toast.makeText(getActivity(), "You require an internet connection to update the leaderboard. Please connect to the internet and try again.", Toast.LENGTH_LONG);
            refreshFAB.setEnabled(true);
        }
    }

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

        public String getGoldStr() {
            String goldStr;
            if (gold >= Math.pow(10, 9)) {
                goldStr = String.format("%.3f B", gold / Math.pow(10, 9));
            } else if (gold >= Math.pow(10, 6)) {
                goldStr = String.format("%.3f M", gold / Math.pow(10, 6));
            } else if (gold >= Math.pow(10, 3)) {
                goldStr = String.format("%.3f K", gold / Math.pow(10, 3));
            } else {
                goldStr = String.format("%.3f", gold);
            }
            return goldStr;
        }
    }
}
