/**
 * Copyright 2016 IBM Corp. All Rights Reserved.
 *
 * Licensed under the IBM License, a copy of which may be obtained at:
 *
 * http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?li_formnum=L-DDIN-AEGGZJ&popup=y&title=IBM%20IoT%20for%20Automotive%20Sample%20Starter%20Apps%20%28Android-Mobile%20and%20Server-all%29
 *
 * You may not use this file except in compliance with the license.
 */
package carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.Reservations;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;

import carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.AnalyzeMyDriving;
import carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.FirstPage;
import carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.tabNavigation;
import carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.ConnectedDriverAPI.API;
import carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.ConnectedDriverAPI.ReservationsData;
import carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.Home.CreateReservation;
import carsharing.starter.automotive.iot.ibm.com.mobilestarterapp.R;

public class CompleteReservation extends AppCompatActivity {
    public static Boolean isUnlocked = false;
    public static Boolean userUnlocked = false;

    private ReservationsData reservationData;
    private ReservationsData reservation;

    static final GregorianCalendar[] pickupCal = { new GregorianCalendar() };

    private Button unlockButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_reservation);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            reservationData = (ReservationsData) getIntent().getSerializableExtra("reservationData");
            reservation = reservationData;

            pickupCal[0].setTimeInMillis(reservationData.pickupTime.longValue());
            getSupportActionBar().setTitle("Pickup at " + dateToString(pickupCal[0]));

            ImageView carThumbnailImageView = (ImageView) findViewById(R.id.carThumbnail);
            TextView carTitleTextView = (TextView) findViewById(R.id.behaviorTitle);
            TextView carStarsTextView = (TextView) findViewById(R.id.carStars);

            Picasso.with(getApplicationContext()).load(reservationData.carDetails.thumbnailURL).placeholder(R.drawable.models).into(carThumbnailImageView);

            carTitleTextView.setText(reservationData.carDetails.title);
            String stars = new String(new char[reservationData.carDetails.stars]).replace("\0", "\u2605");
            String emptyStars = new String(new char[5 - reservationData.carDetails.stars]).replace("\0", "\u2606");
            carStarsTextView.setText(stars + emptyStars);

            ListView listView = (ListView) findViewById(R.id.listView);

            CompleteReservationAdapter adapter = new CompleteReservationAdapter(this, reservationData);
            listView.setAdapter(adapter);
        }

        unlockButton = (Button) findViewById(R.id.unlockButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);

//        cancelButton.setEnabled(false);
        unlockButton.setEnabled(false);

        setLabelsAccordingToStatus();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();

        return true;
    }

    public static String dateToString(GregorianCalendar cal) {
        SimpleDateFormat formattedCal = new SimpleDateFormat("MM/dd/yy, hh:mm a");
        formattedCal.setCalendar(cal);

        String dateFormatted = formattedCal.format(cal.getTime());

        return dateFormatted;
    }

    public void unlockCar(View view) {
        String url = API.carControl;

        String reservationId = reservationData._id;
        final String[] command = { new String() };
        command[0] = "lock";

        if (reservation == null || (reservation.carDetails.status.equals("Locked") && !isUnlocked)) {
            command[0] = "unlock";
        }

        try {
            JSONObject bodyObject = new JSONObject();
            bodyObject.put("reservationId", reservationId);
            bodyObject.put("command", command[0]);

            API.doRequest task = new API.doRequest(new API.doRequest.TaskListener() {
                @Override
                public void postExecute(JSONArray result) throws JSONException {
                    JSONObject serverResponse = result.getJSONObject(result.length() - 1);
                    int statusCode = serverResponse.getInt("statusCode");

                    result.remove(result.length() - 1);

                    ArrayList<ReservationsData> reservations = new ArrayList<ReservationsData>();

                    for (int i=0; i < result.length(); i++) {
                        ReservationsData tempReservationData = new ReservationsData(result.getJSONObject(i));
                        reservations.add(tempReservationData);
                    }

                    if (reservations.size() == 1) {
                        reservation = reservations.get(0);
                    }

                    switch (statusCode) {
                    case 200:
                        if (command[0].equals("lock")) {
                            unlockButton.setText("Unlock the car");

                            break;
                        } else {
                            unlockButton.setText("Lock the car");

                            cancelButton.setEnabled(true);

                            Toast toast = Toast.makeText(getApplicationContext(), "Enjoy your ride and drive safe", Toast.LENGTH_SHORT);
                            toast.show();

                            setLabelsAccordingToStatus();

                            break;
                        }
                    default:
                        Log.e("Unlock Car", "unknown status code on unlock car action");

                        break;
                    }

                    Log.i("Unlock Car", result.toString());
                }
            });

            task.execute(url, "POST", null, bodyObject.toString()).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setLabelsAccordingToStatus() {
            if (reservation != null) {
                if (reservation.status.equals("active")) {
                    cancelButton.setText("Cancel Reservation");
                } else {
                    cancelButton.setText("Complete Reservation");
                }

                if (reservation.carDetails.status.equals("Locked")) {
                    unlockButton.setText("Unlock the car");
                } else {
                    unlockButton.setText("Lock the car");
                }
            } else {
                if (reservationData.status.equals("active")) {
                    cancelButton.setText("Cancel Reservation");
                } else {
                    cancelButton.setText("Complete Reservation");
                }

                if (reservationData.carDetails.status.equals("Locked")) {
                    unlockButton.setText("Unlock the car");
                } else {
                    unlockButton.setText("Lock the car");
                }
            }

            unlockButton.setEnabled(true);
    }

    public void cancelReservation(View view) {
        cancelButton.setEnabled(false);

        String url = API.reservation + "/" + reservationData._id;

        final View v = view;

        try {
            if (reservation.status.equals(null) || reservation.status.equals("active")) {
                API.doRequest task = new API.doRequest(new API.doRequest.TaskListener() {
                    @Override
                    public void postExecute(JSONArray result) {
                        Log.i("Cancel Reservation", result.toString());

                        Toast toast = Toast.makeText(getApplicationContext(), "Reservation Successfully Cancelled!", Toast.LENGTH_SHORT);
                        toast.show();

                        Reservations.userReserved = true;

                        Intent tabActivity = new Intent(v.getContext(), tabNavigation.class);
                        startActivity(tabActivity);
                    }
                });

                task.execute(url, "DELETE").get();
            } else {
                API.doRequest task = new API.doRequest(new API.doRequest.TaskListener() {
                    @Override
                    public void postExecute(JSONArray result) {
                        Log.i("Complete Reservation", result.toString());

                        Toast toast = Toast.makeText(getApplicationContext(), "Reservation Successfully Completed!", Toast.LENGTH_SHORT);
                        toast.show();

                        Reservations.userReserved = true;

                        Intent tabActivity = new Intent(v.getContext(), tabNavigation.class);
                        startActivity(tabActivity);
                    }
                });

                String trip_id = AnalyzeMyDriving.getTripId(reservation.carDetails.deviceID);

                //TODO Compare with the Swift Version, needs additions
                JSONObject bodyObject = new JSONObject();
                bodyObject.put("status", "close");
                bodyObject.put("trip_id", trip_id);

                task.execute(url, "PUT", null, bodyObject.toString()).get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}