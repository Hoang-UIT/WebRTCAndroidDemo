package com.example.webrtcadroiddemo;

import java.io.Serializable;
import java.util.ArrayList;

public class Caller implements Serializable {
    private String id;
    private String name;
    private Boolean isOnline;
    private String sdp;
    private ArrayList<Candidate> candidates = new ArrayList<>();

    public Caller() {
    }

    public Caller(String id, String name, String sdp, ArrayList<Candidate> candidates) {
        this.id = id;
        this.name = name;
        this.sdp = sdp;
        this.candidates = candidates;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getOnline() {
        return isOnline;
    }

    public void setOnline(Boolean online) {
        isOnline = online;
    }

    public void addCadidate(Candidate candidate) {
        this.candidates.add(candidate);
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public ArrayList<Candidate> getCandidates() {
        return candidates;
    }
}

