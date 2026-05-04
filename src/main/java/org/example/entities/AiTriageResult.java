package org.example.entities;

import java.util.ArrayList;
import java.util.List;

public class AiTriageResult {
    private String recommendedType = "";
    private String recommendedSpeciality = "";
    private String priorityLevel = "";
    private String rationale = "";
    private boolean emergency;
    private String suggestedPsychologueName = "";
    private List<String> followUpQuestions = new ArrayList<>();
    private String synthesizedNotes = "";

    public String getRecommendedType() {
        return recommendedType;
    }

    public void setRecommendedType(String recommendedType) {
        this.recommendedType = recommendedType;
    }

    public String getRecommendedSpeciality() {
        return recommendedSpeciality;
    }

    public void setRecommendedSpeciality(String recommendedSpeciality) {
        this.recommendedSpeciality = recommendedSpeciality;
    }

    public String getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public boolean isEmergency() {
        return emergency;
    }

    public void setEmergency(boolean emergency) {
        this.emergency = emergency;
    }

    public String getSuggestedPsychologueName() {
        return suggestedPsychologueName;
    }

    public void setSuggestedPsychologueName(String suggestedPsychologueName) {
        this.suggestedPsychologueName = suggestedPsychologueName;
    }

    public List<String> getFollowUpQuestions() {
        return followUpQuestions;
    }

    public void setFollowUpQuestions(List<String> followUpQuestions) {
        this.followUpQuestions = followUpQuestions == null ? new ArrayList<>() : followUpQuestions;
    }

    public String getSynthesizedNotes() {
        return synthesizedNotes;
    }

    public void setSynthesizedNotes(String synthesizedNotes) {
        this.synthesizedNotes = synthesizedNotes;
    }
}
