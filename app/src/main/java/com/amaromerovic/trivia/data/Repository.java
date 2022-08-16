package com.amaromerovic.trivia.data;

import android.util.Log;

import com.amaromerovic.trivia.controller.AppController;
import com.amaromerovic.trivia.model.Question;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Repository {
    private final ArrayList<Question> questions = new ArrayList<>();
    private final String url = "https://raw.githubusercontent.com/curiousily/simple-quiz/master/script/statements-data.json";
    private Random random;
    private int indexShuffle;

    public List<Question> getQuestions(final AnswerListAsyncResponse callback) {
        random = new Random();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                        for (int i = 0; i < response.length(); i++) {
                            indexShuffle = random.nextInt(response.length());
                            try {
                                questions.add(new Question(response.getJSONArray(indexShuffle).get(0).toString(), response.getJSONArray(indexShuffle).getBoolean(1)));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        if (callback != null){
                            callback.processFinished(questions);
                        }
                }, error -> Log.d("RepoClassCall", "onCreate: Failed!"));

        AppController.getInstance().addToRequestQueue(jsonArrayRequest);
        AppController.getInstance().getRequestQueue().start();

        return questions;
    }

}
