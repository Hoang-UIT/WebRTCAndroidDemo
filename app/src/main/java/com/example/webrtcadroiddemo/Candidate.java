package com.example.webrtcadroiddemo;

import java.io.Serializable;

public class Candidate implements Serializable {
    private String id;
    private int label;
    private String candidate;

    public Candidate(String id, int label, String candidate) {
        this.id = id;
        this.label = label;
        this.candidate = candidate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLabel() {
        return label;
    }

    public void setLabel(int label) {
        this.label = label;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }
}
