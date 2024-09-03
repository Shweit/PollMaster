package com.shweit.poll.commands.pollDetailsCommand;

import java.util.List;

public class PollDetails {

    private final int pollId;
    private final String question;
    private final List<String> answers;
    private final String creator;
    private final String createdAt;
    private final boolean multi;

    public PollDetails(int pollId, String question, List<String> answers, String creator, String createdAt, boolean multi) {
        this.pollId = pollId;
        this.question = question;
        this.answers = answers;
        this.creator = creator;
        this.createdAt = createdAt;
        this.multi = multi;
    }

    public int getPollId() {
        return pollId;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public String getCreator() {
        return creator;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isMulti() {
        return multi;
    }
}
