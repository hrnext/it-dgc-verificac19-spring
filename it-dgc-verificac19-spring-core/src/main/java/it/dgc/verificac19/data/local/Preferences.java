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

  private Long resumeToken;

  private LocalDateTime dateLastFetch;

  private List<Rule> validationRulesJson;

  @PostConstruct
  public void init() {
    this.clear();
  }

  /**
   *
   * This method clears all values from the preferences.
   */
  public void clear() {
    this.resumeToken = null;
    this.dateLastFetch = null;
    this.validationRulesJson = new ArrayList<Rule>();
  }

  /**
   * @return the resumeToken
   */
  public Long getResumeToken() {
    return resumeToken;
  }

  /**
   * @param resumeToken the resumeToken to set
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
   * @param dateLastFetch the dateLastFetch to set
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
   * @param validationRulesJson the validationRulesJson to set
   */
  public void setValidationRulesJson(List<Rule> validationRulesJson) {
    this.validationRulesJson = validationRulesJson;
  }

  public String getValidationRuleValueByName(ValidationRulesEnum validationRulesEnum) {
    Optional<Rule> rule = this.getValidationRulesJson().stream()
        .filter(i -> i.getName() == validationRulesEnum).findFirst();
    return rule.isPresent() ? rule.get().getValue() : "";
  }

  public String getValidationRuleValueByNameAndType(ValidationRulesEnum validationRulesEnum,
      String type) {
    Optional<Rule> rule = this.getValidationRulesJson().stream()
        .filter(i -> i.getName() == validationRulesEnum && i.getType().equals(type)).findFirst();
    return rule.isPresent() ? rule.get().getValue() : "";
  }
}
