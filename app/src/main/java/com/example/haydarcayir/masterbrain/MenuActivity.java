package com.example.haydarcayir.masterbrain;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

public class MenuActivity extends AppCompatActivity {

    private ImageButton exitButton;
    private ImageButton settingsButton;
    private Button easyButton;
    private Button hardButton;
    public Color backgroundColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        exitButton = (ImageButton) findViewById(R.id.exitButton);
        exitButton.setTag("exit");
        exitButton.setOnClickListener(ImageClickListener);

        settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        settingsButton.setTag("settings");
        settingsButton.setOnClickListener(ImageClickListener);

        easyButton = (Button) findViewById(R.id.easyButton); // easy button
        easyButton.setOnClickListener(ButtonListener); // add click listener
        hardButton = (Button) findViewById(R.id.hardButton);// hard button
        hardButton.setOnClickListener(ButtonListener); // add click listener

    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    // when click on imageViews ( exit and settings )
    private View.OnClickListener ImageClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String img = String.valueOf(view.getTag());
            if(img.contains("exit")){
                android.os.Process.killProcess(android.os.Process.myPid());
                Log.e("exit","exit");
            }
        }
    };

    // when click on main three buttons (easy, hard and leaderboard)
    private View.OnClickListener ButtonListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            String buttonName = ((Button) view).getText().toString();
            if(buttonName.contains("EASY")){
                startEasyGame((Button) view);
            }
            else{
                startHardGame((Button) view);
            }
        } // end method onClick
    };

    private void startEasyGame(Button v){
        //start game in easy mode
        String message = v.getText().toString().toLowerCase();
        Intent i = new Intent(getApplicationContext(),GameActivity.class);//Creates new intent to start a new activity
        i.putExtra("gameMode",message);
        startActivity(i);
        finish();
    }

    private void startHardGame(Button v){
        //start game in hard mode
        String message = v.getText().toString().toLowerCase();
        Intent i = new Intent(getApplicationContext(),GameActivity.class);//Creates new intent to start a new activity
        i.putExtra("gameMode",message);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    private void showExitDialog(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MenuActivity.this);
        alertDialog.setTitle("Leave application?");
        alertDialog.setMessage("Are you sure you want to leave the application?");
        alertDialog.setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

        alertDialog.setNegativeButton("NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }
}
