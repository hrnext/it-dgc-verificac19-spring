package it.dgc.verificac19.data.local;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import it.dgc.verificac19.data.remote.model.Rule;
import it.dgc.verificac19.model.ValidationRulesEnum;

@Component
public class Preferences {

    private long resumeToken;

    private LocalDateTime dateLastFetch;

    private LocalDateTime drlDateLastFetch;

    private List<Rule> validationRulesJson;

    private boolean isDrlSyncActive = true;

    private long currentVersion = 0;

    private long totalNumberUCVI = 0;

    private long requestedVersion = 0;

    private long fromVersion = 0;

    private long totalChunk = 0;

    private long chunk = 0;

    private long currentChunk = 0;

    @PostConstruct
    public void init() {
        this.clear();
    }

    /**
     *
     * This method clears all values from the preferences.
     */
    public void clear() {
        this.resumeToken = -1L;
        this.dateLastFetch = null;
        this.validationRulesJson = new ArrayList<Rule>();
    }

    public void clearDrlPrefs() {
        this.drlDateLastFetch = null;
        this.fromVersion = 0;
        this.totalChunk = 0;
        this.chunk = 0;
        this.totalNumberUCVI = 0;
        this.currentVersion = 0;
        this.requestedVersion = 0;
        this.currentChunk = 0;
        this.isDrlSyncActive = true;
    }

    /**
     * @return the resumeToken
     */
    public long getResumeToken() {
        return resumeToken;
    }

    /**
     * @param resumeToken
     *            the resumeToken to set
     */
    public void setResumeToken(Long resumeToken) {
        this.resumeToken = resumeToken;
    }

    /**
     * @return the dateLastFetch
     */
    public synchronized LocalDateTime getDateLastFetch() {
        return dateLastFetch;
    }

    /**
     * @param dateLastFetch
     *            the dateLastFetch to set
     */
    public synchronized void setDateLastFetch(LocalDateTime dateLastFetch) {
        this.dateLastFetch = dateLastFetch;
    }

    /**
     * @return the validationRulesJson
     */
    public List<Rule> getValidationRulesJson() {
        return validationRulesJson;
    }

    /**
     * @param validationRulesJson
     *            the validationRulesJson to set
     */
    public void setValidationRulesJson(List<Rule> validationRulesJson) {
        this.validationRulesJson = validationRulesJson;
    }

    public String getValidationRuleValueByName(ValidationRulesEnum validationRulesEnum) {
        Optional<Rule> rule = this.getValidationRulesJson().stream().filter(i -> i.getName() == validationRulesEnum).findFirst();
        return rule.isPresent() ? rule.get().getValue() : "";
    }

    public String getValidationRuleValueByNameAndType(ValidationRulesEnum validationRulesEnum, String type) {
        Optional<Rule> rule = this.getValidationRulesJson().stream().filter(i -> i.getName() == validationRulesEnum && i.getType().equals(type)).findFirst();
        return rule.isPresent() ? rule.get().getValue() : "";
    }

    public boolean isDrlSyncActive() {
        return isDrlSyncActive;
    }

    public Long getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Long currentVersion) {
        this.currentVersion = currentVersion;
    }

    public synchronized LocalDateTime getDrlDateLastFetch() {
        return drlDateLastFetch;
    }

    public synchronized void setDrlDateLastFetch(LocalDateTime drlDateLastFetch) {
        this.drlDateLastFetch = drlDateLastFetch;
    }

    public synchronized long getTotalNumberUCVI() {
        return totalNumberUCVI;
    }

    public synchronized void setTotalNumberUCVI(long totalNumberUCVI) {
        this.totalNumberUCVI = totalNumberUCVI;
    }

    public synchronized long getRequestedVersion() {
        return requestedVersion;
    }

    public synchronized void setRequestedVersion(long requestedVersion) {
        this.requestedVersion = requestedVersion;
    }

    public long getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(long fromVersion) {
        this.fromVersion = fromVersion;
    }

    public long getTotalChunk() {
        return totalChunk;
    }

    public void setTotalChunk(long totalChunk) {
        this.totalChunk = totalChunk;
    }

    public long getChunk() {
        return chunk;
    }

    public void setChunk(long chunk) {
        this.chunk = chunk;
    }

    public long getCurrentChunk() {
        return currentChunk;
    }

    public void setCurrentChunk(long currentChunk) {
        this.currentChunk = currentChunk;
    }

    public void setResumeToken(long resumeToken) {
        this.resumeToken = resumeToken;
    }

    public void setDrlSyncActive(boolean isDrlSyncActive) {
        this.isDrlSyncActive = isDrlSyncActive;
    }

}
