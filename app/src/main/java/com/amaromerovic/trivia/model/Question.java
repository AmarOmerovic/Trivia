package com.amaromerovic.trivia.model;

public class Question {
    private final String question;
    private final boolean answer;

    public Question(String question, boolean answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public boolean isAnswer() {
        return answer;
    }
}
