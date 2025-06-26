/*
* Copyright © 2023 - 2024. Cloud Software Group, Inc.
* This file is subject to the license terms contained
* in the license file that is distributed with this file.
*/
package com.tibco.sonar.plugins.bw6.check.project;

import com.tibco.sonar.plugins.bw6.check.AbstractProjectCheck;
import com.tibco.sonar.plugins.bw6.profile.BWProcessQualityProfile;
import com.tibco.sonar.plugins.bw6.source.ProjectSource;
import com.tibco.utils.bw6.model.Project;
import java.io.File;

import com.tibco.utils.common.logger.Logger;
import com.tibco.utils.common.logger.LoggerFactory;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

@Rule(
        key = ProjectNameValidationCheck.RULE_KEY,
        name = "Project Name Validation Check",
        description = "This rule ensures the naming convention for the project name",
        priority = Priority.MINOR)

@BelongsToProfile(title = BWProcessQualityProfile.PROFILE_NAME, priority = Priority.MINOR)
public class ProjectNameValidationCheck extends AbstractProjectCheck {

    public static final String RULE_KEY = "ProjectNameValidation";

    private static final Logger LOG = LoggerFactory.getLogger(ProjectNameValidationCheck.class);

    @RuleProperty(
            key = "projectNamePattern",
            description = "Regular Expression the project name should match",
            defaultValue = "^(om|il)-([A-Z][a-z0-9]+)+$",
            type = "TEXT"
    )
    protected String projectNamePattern;

    @RuleProperty(
            key = "maxProjectNameLength",
            description = "Maximum allowed length for the project name",
            defaultValue = "50",
            type = "INTEGER"
    )
    protected int maxProjectNameLength;

    @Override
    public void validate(ProjectSource resourceXml) {
        LOG.debug("Started rule: " + this.getClass());
        Project project = resourceXml.getProject();
        LOG.debug("Validating project name");

        if (project != null && project.getFile() != null) {
            String projectName = project.getFile().getName();

            boolean isValidPattern = projectName.matches(projectNamePattern);
            boolean isValidLength = projectName.length() <= maxProjectNameLength;

            if (!isValidPattern) {
                reportIssueOnFile("Project name '" + projectName + "' doesn't match the project naming convention with kebab-case pattern ["+projectNamePattern+"]");
            }

            if (!isValidLength) {
                reportIssueOnFile("Project name '" + projectName + "' exceeds the maximum allowed length of " + maxProjectNameLength + " characters.");
            }
        }
    }

    @Override
    public String getRuleKeyName() {
        return RULE_KEY;
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }
}