package com.shweit.poll.commands.pollDetailsCommand;

import java.util.List;

public final class PollDetails {

    private final int pollId;
    private final String question;
    private final List<String> answers;
    private final String creator;
    private final String createdAt;
    private final boolean multi;

    public PollDetails(
            final int pollIdParameter,
            final String questionParameter,
            final List<String> answersParameter,
            final String creatorParameter,
            final String createdAtParameter,
            final boolean multiParameter
    ) {
        this.pollId = pollIdParameter;
        this.question = questionParameter;
        this.answers = answersParameter;
        this.creator = creatorParameter;
        this.createdAt = createdAtParameter;
        this.multi = multiParameter;
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
