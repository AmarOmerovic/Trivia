package com.amaromerovic.trivia;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.amaromerovic.trivia.data.Repository;
import com.amaromerovic.trivia.databinding.ActivityMainBinding;
import com.amaromerovic.trivia.model.Question;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String SHARED_PREF_QUESTION_INDEX_KEY = "GetQuestionIndex";
    private ActivityMainBinding mainBinding;
    private int questionIndex;
    private List<Question> questions;
    private Snackbar snackbar;
    private static final String SHARED_PREF_KEY = "TriviaHighScorePoints";
    private static final String SHARED_PREF_HIGHSCORE_KEY = "GetHighScore";
    private int highScore;
    private int currentScore;
    private static final int POINTS_PER_QUESTION = 5;
    private static final int MAX_POINTS = (913 * 5);
    private SoundPool soundPool;
    private int correctSound, wrongSound, swipeSound, popUpSound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isOnline()) {
            alert("There was a problem with your internet connection. Please make sure that your connection is stable before you keep using the app.", "No internet connection!");
        }

        mainBinding = DataBindingUtil.setContentView(MainActivity.this, R.layout.activity_main);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build();

        correctSound = soundPool.load(this, R.raw.correct, 1);
        wrongSound = soundPool.load(this, R.raw.wrong, 1);
        swipeSound = soundPool.load(this, R.raw.swipe, 1);
        popUpSound = soundPool.load(this, R.raw.message_pop_alert, 1);

        questionIndex = 0;
        questions = new Repository().getQuestions(this::setQuestionToView);
        loadGame();

        mainBinding.restartButton.setOnClickListener(view -> {
            questionIndex = 0;
            currentScore = 0;
            questions = new Repository().getQuestions(this::setQuestionToView);
        });

        mainBinding.nextButton.setOnClickListener(view -> {
            mainBinding.nextButton.setClickable(false);
            mainBinding.trueButton.setClickable(false);
            mainBinding.falseButton.setClickable(false);
            mainBinding.restartButton.setClickable(false);
            getNextQuestionAfterAnswer();
            setQuestionToView(questions);
        });

        mainBinding.trueButton.setOnClickListener(view -> {
            checkAnswer(true);
            setQuestionToView(questions);
        });

        mainBinding.falseButton.setOnClickListener(view -> {
            checkAnswer(false);
            setQuestionToView(questions);
        });

        mainBinding.highScoreButton.setOnClickListener(view -> setSnackbar(750, getResources().getString(R.string.highScoreText) + highScore + " / " + MAX_POINTS, Gravity.CENTER, 1000));


        mainBinding.shareButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "I am playing Trivia");
            intent.putExtra(Intent.EXTRA_TEXT, "My current score: " + currentScore + "\n My highscore is: " + highScore);
            startActivity(intent);
        });


    }

    private void getNextQuestionAfterAnswer() {
        questionIndex++;
        slideOutAnimation();
        if (questionIndex == (questions.size() - 1)){
            mainBinding.nextButton.setText(R.string.retakeButton);
        } else if (questionIndex == questions.size()){
            saveGame();
            questionIndex = 0;
            currentScore = 0;
            questions = new Repository().getQuestions(this::setQuestionToView);
            mainBinding.nextButton.setText(R.string.nextButton);
        }
    }

    @SuppressLint("SetTextI18n")
    private void setQuestionToView(List<Question> questionArrayList) {
        mainBinding.questionTextView.setText(questionArrayList.get(questionIndex).getQuestion());
        mainBinding.questionNo.setText(String.format(getString(R.string.questionNoFormated), questionIndex + 1, questions.size()));
        mainBinding.pointsTextView.setText(getResources().getString(R.string.points) + " " + currentScore);
    }

    private void checkAnswer(boolean usersChoice){
        mainBinding.trueButton.setClickable(false);
        mainBinding.falseButton.setClickable(false);
        mainBinding.nextButton.setClickable(false);
        mainBinding.restartButton.setClickable(false);
        int snackMessageIt = R.string.incorrect;
        if (questions.get(questionIndex).isAnswer() == usersChoice){
            currentScore += POINTS_PER_QUESTION;
            snackMessageIt = R.string.correct;
            fadeAnimation();
        } else {
            if (currentScore >= POINTS_PER_QUESTION){
                currentScore -= POINTS_PER_QUESTION;
            }
            shakeAnimation();
        }
        setSnackbar(FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY, getResources().getString(snackMessageIt), Gravity.TOP, 800);

    }

    private void setSnackbar(int layoutParams, String snackMessageIt, int gravity, int duration) {
        snackbar = Snackbar.make(mainBinding.cardView, snackMessageIt, duration);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.black));
        View view = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
        params.gravity = gravity;
        params.width = layoutParams;
        TextView textView = view.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null){
            textView.setGravity(Gravity.CENTER);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
            textView.setTextSize(20);
        }
        view.setLayoutParams(params);
        soundPool.play(popUpSound, 1, 1, 0, 0, 1);
        snackbar.show();
    }

    private void shakeAnimation(){
        Animation shake = AnimationUtils.loadAnimation(MainActivity.this, R.anim.shake_animation);
        mainBinding.cardView.setAnimation(shake);

        shake.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                soundPool.play(wrongSound, 1, 1, 0, 0, 1);
                mainBinding.questionTextView.setBackgroundColor(Color.RED);
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(300);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mainBinding.questionTextView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.cardViewColor));
                getNextQuestionAfterAnswer();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void slideOutAnimation(){
        Animation slide = AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_out_left);
        mainBinding.cardView.setAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                soundPool.play(swipeSound, 1, 1, 0, 0, 1);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setQuestionToView(questions);
                saveGame();
                slideInAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void slideInAnimation(){
        Animation slide = AnimationUtils.loadAnimation(MainActivity.this, R.anim.slide_in_left);
        mainBinding.cardView.setAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mainBinding.nextButton.setClickable(true);
                mainBinding.falseButton.setClickable(true);
                mainBinding.trueButton.setClickable(true);
                mainBinding.restartButton.setClickable(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void fadeAnimation() {
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(200);
        animation.setRepeatCount(3);
        animation.setRepeatMode(Animation.REVERSE);

        mainBinding.cardView.setAnimation(animation);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                soundPool.play(correctSound, 1, 1, 0, 0, 1);
                mainBinding.questionTextView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.animationGreen));
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(150);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mainBinding.questionTextView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.cardViewColor));
                getNextQuestionAfterAnswer();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }

        });
    }

    private void loadGame() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
        highScore = sharedPreferences.getInt(SHARED_PREF_HIGHSCORE_KEY, 0);
        questionIndex = sharedPreferences.getInt(SHARED_PREF_QUESTION_INDEX_KEY, 0);
    }

    private void saveGame() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (currentScore > highScore){
            highScore = currentScore;
            editor.putInt(SHARED_PREF_HIGHSCORE_KEY, highScore);
        }
        editor.putInt(SHARED_PREF_QUESTION_INDEX_KEY, questionIndex);
        editor.apply();
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void alert(String message, String title){
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setCancelable(false);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPositiveButton("I understand", (dialog1, id) -> finish());
        final AlertDialog alert = dialog.create();
        alert.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveGame();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}