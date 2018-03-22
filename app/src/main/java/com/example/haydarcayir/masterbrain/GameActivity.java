package com.example.haydarcayir.masterbrain;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class GameActivity extends AppCompatActivity {

    private Dialog dialogHowToPlay;

    private static final float SHAKE_THRESHOLD = 3.00f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000; // 1 sec
    private long mLastShakeTime;
    private SensorListen sensorListen;

    private ImageView blueColor;
    private ImageView redColor;
    private ImageView greenColor;
    private ImageView purpleColor;
    private ImageView orangeColor;
    private ImageView whiteColor;

    private Dialog dialog_score;
    private TextView result,score_number,played_time;
    private Button end_game;
    private ImageButton close_dlg;

    private ArrayList<Integer> guessList;   // 3 2 1 5  which user enters in each guess
    private ArrayList<Integer> colorList;   // 0 1 2 3 4 5
    private ArrayList<Integer> answerList;  // 0 1 2 4
    private HashMap<String,Integer> colorMap; // 0 -> "blue" | 1 -> "red" | 2 -> "green" | 3 -> "yellow" | 4 -> "purple" | 5 -> "white"
    private ArrayList<TableRow> rowList; // holds all rows which represent guess steps
    private ArrayList<TableLayout> guessTableList; // holds the tables that contains correct and incorrect answers' number
    private int currentWindow = 0;
    private int guessNo = 0;
    private int correct = 0;
    private int colorCorrect = 0;
    private int playerScore = 0;
    private long startTime = 0;
    private long endTime = 0;

    private Handler customHandler = new Handler();
    private long timeInMilliseconds = 0L;
    private long timeSwapBuff = 0L;
    private long updatedTime = 0L;
    private int elapsedTime = 0;

    public String gameMode;

    private Button guessButton;
    private Button newGame;
    private Button endGame;
    private ImageButton pauseButton;
    private TextView timerText;

    private DatabaseReference mDatabase;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    private String userName;
    private String score;
    private String id;

    private boolean offline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        sensorListen = new SensorListen();
        sensorListen.open();

        /*get message from previous activity*/
        Bundle extras = getIntent().getExtras();
        gameMode = extras.getString("gameMode");
        offline = extras.getBoolean("offline");
        if(gameMode == null)
            gameMode = "easy";

        if(!offline){
            firebaseAuth=FirebaseAuth.getInstance();
            user = firebaseAuth.getCurrentUser();

            mDatabase= FirebaseDatabase.getInstance().getReference();
        }

        timerText = (TextView) findViewById(R.id.timerText);

        //Guess button definition
        guessButton = (Button) findViewById(R.id.guessButton);
        guessButton.setEnabled(false);
        guessButton.setTextColor(getResources().getColor(R.color.disabled_color));
        guessButton.setOnClickListener(GuessClickListener);

        pauseButton = (ImageButton) findViewById(R.id.pauseButton);
        pauseButton.setTag("pause");
        pauseButton.setOnClickListener(PauseClickListener);


        blueColor = (ImageView) findViewById(R.id.blueColor);
        blueColor.setTag("blue");

        redColor = (ImageView) findViewById(R.id.redColor);
        redColor.setTag("red");

        greenColor = (ImageView) findViewById(R.id.greenColor);
        greenColor.setTag("green");

        purpleColor = (ImageView) findViewById(R.id.purpleColor);
        purpleColor.setTag("purple");

        orangeColor = (ImageView) findViewById(R.id.orangeColor);
        orangeColor.setTag("yellow");

        whiteColor = (ImageView) findViewById(R.id.whiteColor);
        whiteColor.setTag("white");

        // get rows from resources and add them to row list
        rowList = new ArrayList<TableRow>();
        rowList.add((TableRow) findViewById(R.id.firstRow));
        rowList.add((TableRow) findViewById(R.id.secondRow));
        rowList.add((TableRow) findViewById(R.id.thirdRow));
        rowList.add((TableRow) findViewById(R.id.fourthRow));
        rowList.add((TableRow) findViewById(R.id.fifthRow));
        rowList.add((TableRow) findViewById(R.id.sixthRow));
        rowList.add((TableRow) findViewById(R.id.seventhRow));
        rowList.add((TableRow) findViewById(R.id.eighthRow));
        rowList.add((TableRow) findViewById(R.id.resultRow));

        guessTableList = new ArrayList<TableLayout>();
        guessTableList.add((TableLayout) findViewById(R.id.firstGuess));
        guessTableList.add((TableLayout) findViewById(R.id.secondGuess));
        guessTableList.add((TableLayout) findViewById(R.id.thirdGuess));
        guessTableList.add((TableLayout) findViewById(R.id.fourthGuess));
        guessTableList.add((TableLayout) findViewById(R.id.fifthGuess));
        guessTableList.add((TableLayout) findViewById(R.id.sixthGuess));
        guessTableList.add((TableLayout) findViewById(R.id.seventhGuess));
        guessTableList.add((TableLayout) findViewById(R.id.eighthGuess));

        colorMap = new HashMap<String,Integer>();
        setColorMap();

        colorList = new ArrayList<Integer>();
        answerList = new ArrayList<Integer>();
        guessList = new ArrayList<Integer>();


        startNewGame(gameMode);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorListen.close();
        timeSwapBuff += timeInMilliseconds;
        customHandler.removeCallbacks(updateTimerThread);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorListen.open();
        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 0);
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
        return;
    }

    // If we want to reach int representation from string representation or vice versa
    private void setColorMap() {
        colorMap.put("blue",0);
        colorMap.put("red",1);
        colorMap.put("green",2);
        colorMap.put("yellow",3);
        colorMap.put("purple",4);
        colorMap.put("white",5);
    }

    private void startNewGame(String gameMode){
        //startTime = System.currentTimeMillis();

        timeInMilliseconds = 0L;
        timeSwapBuff = 0L;
        updatedTime = 0L;
        elapsedTime = 0;

        guessNo = 0;
        currentWindow = 0;
        playerScore = 0;

        clearRows();
        colorList.clear();
        answerList.clear();
        guessList.clear();

        for(int i = 0; i < 6; i++)
            colorList.add(i);

        ImageView cell;
        for(int i = 0; i < 4; i++){
            cell = (ImageView) rowList.get(guessNo).getChildAt(i + 1);
            cell.setOnClickListener(CellClickListener);
            cell.setTag("null|"+i);
        }
        Collections.shuffle(colorList); // shuffle list for choosing answerList

        if(gameMode.equals("hard") && (float)(Math.random() * 1) <= 0.9){
            int index = (int)(Math.random() * 4);
            int number = colorList.get(index);
            colorList.remove(Math.abs(index - 1));
            colorList.add(0,number);
            Collections.shuffle(colorList);
        }

        // create answer list from colorList which is generated randomly
        for(int i = 0; i < 4; i++)
            answerList.add(colorList.get(i)); // create a random answer list

        for(int i = 0; i < 4; i++)
            Log.d("error",answerList.get(i).toString()); // create a random answer list

        setSelectableClickListeners();  // set listener for above colors

        startTime = SystemClock.uptimeMillis();
        customHandler.postDelayed(updateTimerThread, 0);

        sensorListen.open();
    }

    private void clearRows() {
        ImageView currentImage;
        TableRow row;
        //clears color cells
        for(int i=0; i < 9; i++){
           for(int j=0; j < 4; j++) {
               currentImage = (ImageView) rowList.get(i).getChildAt(j + 1);
               currentImage.setImageResource(0);
           }
        }
        //clears guess(correct or incorrect) cells
        for(int i=0; i < 8; i++){
            for(int j=0; j < 4; j++) {
                if( j > 1){
                    row = (TableRow) guessTableList.get(i).getChildAt(1);
                    currentImage = (ImageView) row.getChildAt(j - 2);
                    currentImage.setImageResource(0);
                }
                else{
                    row = (TableRow) guessTableList.get(i).getChildAt(0);
                    currentImage = (ImageView) row.getChildAt(j);
                    currentImage.setImageResource(0);
                }
            }
        }
    }

    // when click on a cell
    private View.OnClickListener CellClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String tag = (String)view.getTag(); // tag is like yellow|0 (we need two parameter)
            StringTokenizer tokens = new StringTokenizer(tag, "|");
            String first = tokens.nextToken();// this will contain "yellow"
            String second = tokens.nextToken();// this will contain "0"
            ImageView cell = (ImageView) view;
            cell.setImageResource(0); // make empty clicked cell
            currentWindow = Integer.parseInt(second); // current window is clicked cell
            guessButton.setEnabled(false);
            guessButton.setTextColor(getResources().getColor(R.color.disabled_color));
        }
    };

    // when click on colors (bottom side)
    private View.OnClickListener ColorClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String tag = (String)view.getTag();
            //function for setting image according to clicked color image
            setColorBall(tag,guessNo,currentWindow);

            if(isFull()) {
                guessButton.setEnabled(true);
                guessButton.setTextColor(getResources().getColor(R.color.white));
            }
            // if current window is fourth then skip to first window
            if(currentWindow < 3)
                currentWindow++;
            else
                currentWindow = 0;
        }
    };

    // when click on pause button ( how to play )
    private View.OnClickListener PauseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showHowToPlay();
        }
    };

    // when click on guess button
    private View.OnClickListener GuessClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ImageView cell;
            // add guess list current guessed order
            for(int i = 0; i < 4; i++){
                cell = (ImageView) rowList.get(guessNo).getChildAt(i+1);
                String tag = (String)cell.getTag();
                StringTokenizer tokens = new StringTokenizer(tag, "|");
                String first = tokens.nextToken();// this will contain "yellow"
                guessList.add(colorMap.get(first));
            }

            deleteClickListener();// clear listeners for previous cells
            view.setEnabled(false);
            guessButton.setTextColor(getResources().getColor(R.color.disabled_color));

            showGuesses();
            playerScore += updateScore();// update score according to correct answers

            if(checkGameFinish()) // checks whether player guessed correctly or not
            {
                timeSwapBuff += timeInMilliseconds;
                customHandler.removeCallbacks(updateTimerThread);

                deleteClickListener();
                deleteSelectablesClickListener();

                //endTime = System.currentTimeMillis();
                int totalTime = elapsedTime / 30;
                if(gameMode.equals("easy"))
                    playerScore += 300/(3 + (2*totalTime/3)); //create timer
                else
                    playerScore += 600/(3 + (2*totalTime/3)); //create timer

                playerScore *= (8 - guessNo); // score = score * guessNo (if you guess in first step, you get x8 point )

                if(score == null)
                    score = "0";
                int tempScore = Integer.parseInt(score);

                show_dialog("win");

                // set result row with answer list
                for(int i = 0; i < 4;i++){
                    setColorBall(getColorFromId(answerList.get(i)),8,i);
                }

                //startNewGame("easy");
            }
            else {
                if(guessNo == 7){// Player guessed incorrectly and it was last chance. Game finished
                    timeSwapBuff += timeInMilliseconds;
                    customHandler.removeCallbacks(updateTimerThread);

                    deleteClickListener();
                    deleteSelectablesClickListener();

                    int totalTime = elapsedTime / 30;

                    if(gameMode.equals("easy"))
                        playerScore += 300/(3 + (2*totalTime/3)); //create timer
                    else
                        playerScore += 600/(3 + (2*totalTime/3)); //create timer

                    if(score == null)
                        score = "0";
                    int tempScore = Integer.parseInt(score);

                    // set result row with answer list
                    for(int i = 0; i < 4;i++){
                       setColorBall(getColorFromId(answerList.get(i)),8,i);
                    }
                    show_dialog("lose");
                    //startNewGame("easy");

                }
                else{ // // Player guessed incorrectly but game continues
                    guessNo++;
                    currentWindow = 0;
                    for (int i = 0; i < 4; i++) {
                        cell = (ImageView) rowList.get(guessNo).getChildAt(i + 1);
                        cell.setOnClickListener(CellClickListener);
                        cell.setTag("null|"+i);
                    }
                }
            }
        }
    };

    private String getColorFromId(int id) {
        if(id == 0){
            return "blue";
        }
        else if(id == 1){
            return "red";
        }
        else if(id == 2){
            return "green";
        }
        else if(id == 3){
            return "yellow";
        }
        else if(id == 4){
            return "purple";
        }
        else {
            return "white";
        }
    }

    private void showGuesses() {
        TableRow row; // represents current row in table layout
        ImageView cell; // represents current image
        correctColorAndLocation();
        //show how many correct answers are entered
        for(int i = 0; i < correct; i++){
            if( i > 1){ // fills lower row
                row = (TableRow) guessTableList.get(guessNo).getChildAt(1);
                cell = (ImageView) row.getChildAt(i - 2);
            }
            else{ //filss upper row
                row = (TableRow) guessTableList.get(guessNo).getChildAt(0);
                cell = (ImageView) row.getChildAt(i);
            }

            cell.setImageResource(R.drawable.correct); // set correct(black) image to current image view
        }
        // show how many correct color are entered
        for(int i = 0; i < colorCorrect; i++){
            if( correct == 2){ // lower row can be filled
                row = (TableRow) guessTableList.get(guessNo).getChildAt(1);
                cell = (ImageView) row.getChildAt(i);
            }
            else if( correct == 3){ // only lower row's second column can be filled
                row = (TableRow) guessTableList.get(guessNo).getChildAt(1);
                cell = (ImageView) row.getChildAt(1);
            }
            else{
                if(i > 1) {// fills lower row
                    if(correct == 0){ // each column can be filled in this case
                        row = (TableRow) guessTableList.get(guessNo).getChildAt(1);
                        cell = (ImageView) row.getChildAt(i - 2);
                    }
                    else{ // beacuse of correct = 1, only second column can be filled in this case
                        row = (TableRow) guessTableList.get(guessNo).getChildAt(1);
                        cell = (ImageView) row.getChildAt(i - 1);
                    }

                }
                else{ // fills upper row except correct = 1 and i = 1
                    if(correct == 0){
                        row = (TableRow) guessTableList.get(guessNo).getChildAt(0);
                        cell = (ImageView) row.getChildAt(i);
                    }
                    else{//correct = 1
                        if(i == 0){// upper row second column can be filled
                            row = (TableRow) guessTableList.get(guessNo).getChildAt(0);
                            cell = (ImageView) row.getChildAt(i+1);
                        }
                        else{ // lower row first column can be filled
                            row = (TableRow) guessTableList.get(guessNo).getChildAt(1);
                            cell = (ImageView) row.getChildAt(i-1);
                        }
                    }
                }
            }

            cell.setImageResource(R.drawable.incorrect);
        }
    }

    private void setSelectableClickListeners(){
        blueColor.setOnClickListener(ColorClickListener);
        redColor.setOnClickListener(ColorClickListener);
        orangeColor.setOnClickListener(ColorClickListener);
        purpleColor.setOnClickListener(ColorClickListener);
        greenColor.setOnClickListener(ColorClickListener);
        whiteColor.setOnClickListener(ColorClickListener);
    }

    //deletes click listeners for current cells
    private void deleteClickListener() {
        ImageView cell;
        for(int i = 0; i < 4; i++){
            cell = (ImageView) rowList.get(guessNo).getChildAt(i + 1);
            cell.setOnClickListener(null);
        }
    }

    //deletes click listeners for selectable cells
    private void deleteSelectablesClickListener() {
        blueColor.setOnClickListener(null);
        redColor.setOnClickListener(null);
        orangeColor.setOnClickListener(null);
        purpleColor.setOnClickListener(null);
        greenColor.setOnClickListener(null);
        whiteColor.setOnClickListener(null);
    }
    //
    private boolean checkGameFinish() {
        correctColorAndLocation();// updates correct and colorCorrect
        if(correct == 4)
        {
            Toast.makeText(GameActivity.this, "YOU WIN.",
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        guessList.clear();
        return false;
    }

    private void correctColorAndLocation() {
        correct = 0;
        colorCorrect = 0;
        ArrayList<Integer> tempAnswerList = new ArrayList<>(answerList);
        ArrayList<Integer> tempGuessList = new ArrayList<>(guessList);

        //calculate number of correct answer
        for(int i = 0; i < answerList.size(); i++){
            for(int j = 0; j < answerList.size(); j++){
                if(guessList.get(i) == answerList.get(j) && i == j){
                    tempAnswerList.remove(i-correct);
                    tempGuessList.remove(i-correct);
                    correct++;
                }
            }
        }

        // Delete repeated elements from guess list
        Set<Integer> hs = new HashSet<>();
        hs.addAll(tempGuessList);
        tempGuessList.clear();
        tempGuessList.addAll(hs);

        // calculate number of correct colors
        for(int i = 0; i < tempGuessList.size(); i++){
            for(int j = 0; j < tempAnswerList.size(); j++){
                if(tempGuessList.get(i) == tempAnswerList.get(j)){
                    colorCorrect++;
                    if(gameMode.equals("easy"))
                        break;
                }
            }
        }
    }

    private int updateScore() {
        int score = 0;
        correctColorAndLocation();
        score = correct * 5 + colorCorrect * 1; // In each stage score += correct*10 + colorCorrect*5
        return score;
    }

    private boolean isFull(){
        ImageView currentImage;
        for(int i = 0; i < 4; i++){
            currentImage = (ImageView) rowList.get(guessNo).getChildAt(i+1);
            if(currentImage.getDrawable() == null){
                return false;
            }
        }
        return true;
    }

    // Set needed cell clicked color
    private void setColorBall(String img,int guessNumber, int cWindow) {
        ImageView currentImage = (ImageView) rowList.get(guessNumber).getChildAt(cWindow + 1);

        if(img.equals("white")){
            currentImage.setImageResource(R.drawable.white);
            currentImage.setTag(img+"|"+cWindow);
        }
        else if(img.equals("purple")){
            currentImage.setImageResource(R.drawable.purple);
            currentImage.setTag(img+"|"+cWindow);
        }
        else if(img.equals("red")){
            currentImage.setImageResource(R.drawable.red);
            currentImage.setTag(img+"|"+cWindow);
        }
        else if(img.equals("yellow")){
            currentImage.setImageResource(R.drawable.yellow);
            currentImage.setTag(img+"|"+cWindow);
        }
        else if(img.equals("green")){
            currentImage.setImageResource(R.drawable.green);
            currentImage.setTag(img+"|"+cWindow);
        }
        else{
            currentImage.setImageResource(R.drawable.blue);
            currentImage.setTag(img+"|"+cWindow);
        }
    }

    private void show_dialog(String status){

        sensorListen.close();

        dialog_score = new Dialog(this);
        dialog_score.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog_score.setContentView(R.layout.show_score);
        dialog_score.setCancelable(false);
        dialog_score.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        newGame = (Button) dialog_score.findViewById(R.id.newGame);
        endGame = (Button) dialog_score.findViewById(R.id.endGame);

        result =(TextView) dialog_score.findViewById(R.id.resultText);
        score_number = (TextView) dialog_score.findViewById(R.id.scoreTitle);
        played_time = (TextView) dialog_score.findViewById(R.id.elapsedTimeText);
        close_dlg = (ImageButton) dialog_score.findViewById(R.id.close_dialog);

        if(status.equals("win"))
        {
            result.setText("YOU WIN");
        }
        else
        {
            result.setText("YOU LOSE");
        }

        score_number.setText("SCORE: " + playerScore);
        int mins = elapsedTime / 60;
        int secs = elapsedTime % 60;
        played_time.setText("TIME: " + String.format("%02d",mins) + ":" + String.format("%02d",secs));

        close_dlg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_score.cancel();
            }
        });

        endGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_score.cancel();
                Intent i = new Intent(GameActivity.this, MenuActivity.class);
                i.putExtra("offline",offline);
                finish();
                startActivity(i);
            }
        });

        newGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_score.cancel();
                startNewGame(gameMode);

            }
        });


        dialog_score.show();
    }

    private void showHowToPlay() {

        sensorListen.close();
        timeSwapBuff += timeInMilliseconds;
        customHandler.removeCallbacks(updateTimerThread);

        dialogHowToPlay = new Dialog(this);
        dialogHowToPlay.setContentView(R.layout.how_play);
        dialogHowToPlay.setTitle("How To Play");
        dialogHowToPlay.setCancelable(true);

        dialogHowToPlay.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                sensorListen.open();
                startTime = SystemClock.uptimeMillis();
                customHandler.postDelayed(updateTimerThread, 0);
            }
        });

        dialogHowToPlay.show();
    }

    private void showExitDialog(){
        timeSwapBuff += timeInMilliseconds;
        customHandler.removeCallbacks(updateTimerThread);

        sensorListen.close();

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(GameActivity.this);
        alertDialog.setTitle("Leave Game?");
        alertDialog.setMessage("Are you sure you want to leave the game?");
        alertDialog.setCancelable(false);
        alertDialog.setPositiveButton("YES",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(GameActivity.this, MenuActivity.class);
                i.putExtra("offline",offline);
                finish();
                startActivity(i);
            }
        });
        alertDialog.setNegativeButton("NO",
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startTime = SystemClock.uptimeMillis();
                customHandler.postDelayed(updateTimerThread, 0);
                sensorListen.open();
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    public class SensorListen implements SensorEventListener {
        private final SensorManager mSensorManager; // sensor manager
        private final Sensor mAccelerometer;    // accelerometer

        public SensorListen() {
            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // defines accelerometer
        }

        protected void open() {
            // starts sensor listener
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        protected void close() {
            // stops sensor listener
            mSensorManager.unregisterListener(this);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long curTime = System.currentTimeMillis();
                if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    double acceleration = Math.sqrt(Math.pow(x, 2) +
                            Math.pow(y, 2) +
                            Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;

                    // if acceleration exceeds threshold value
                    if (acceleration > SHAKE_THRESHOLD) {
                        mLastShakeTime = curTime;
                        startNewGame(gameMode);
                    }
                }
            }
        }
    }

    private Runnable updateTimerThread = new Runnable() {
        @Override
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            elapsedTime = secs;
            int mins = secs / 60;
            secs = secs % 60;
            timerText.setText("" + String.format("%02d",mins) + ":" + String.format("%02d",secs));
            customHandler.postDelayed(this, 0);
        }
    };
}