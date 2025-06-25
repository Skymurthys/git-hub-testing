package com.tibco.sonar.plugins.bw6.check.process;

import java.util.List;

import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

import com.tibco.sonar.plugins.bw6.check.AbstractProcessCheck;
import com.tibco.sonar.plugins.bw6.profile.BWProcessQualityProfile;
import com.tibco.sonar.plugins.bw6.source.ProcessSource;
import com.tibco.utils.bw6.model.Activity;
import com.tibco.utils.bw6.model.Process;
import com.tibco.utils.common.logger.Logger;
import com.tibco.utils.common.logger.LoggerFactory;

@Rule(
    key = ActivityNamingConventionCheck.RULE_KEY,
    name = "Activity Naming Convention Check",
    priority = Priority.MINOR,
    description = "This rule ensures the naming convention for activity names"
)
@BelongsToProfile(title = BWProcessQualityProfile.PROFILE_NAME, priority = Priority.MINOR)
public class ActivityNamingConventionCheck extends AbstractProcessCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityNamingConventionCheck.class);
    public static final String RULE_KEY = "ActivityNamingConvention";

    @RuleProperty(
        key = "Pattern",
        description = "Regular expression that activity names should follow",
        defaultValue = "^[A-Z][a-zA-Z0-9]*$",
        type = "TEXT"
    )
    protected String activityNamePattern;

    @Override
    protected void validate(ProcessSource processSource) {
        LOG.debug("Start validation for rule: " + RULE_KEY);

        Process process = processSource.getProcessModel();
        List<Activity> activities = process.getActivities();

        for (Activity activity : activities) {
            String activityName = activity.getName();

            LOG.debug("Checking activity name: " + activityName);

            if (!activityName.matches(activityNamePattern)) {
                reportIssueOnFile(
                    "Activity name '" + activityName +
                    "' does not follow the naming convention: " + activityNamePattern
                );
            }
        }

        LOG.debug("Validation ended for rule: " + RULE_KEY);
    }

    @Override
    public String getRuleKeyName() {
        return RULE_KEY;
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }

    public String getActivityNamePattern() {
        return activityNamePattern;
    }

    public void setActivityNamePattern(String activityNamePattern) {
        this.activityNamePattern = activityNamePattern;
    }
}
