package com.tibco.sonar.plugins.bw6.check.project;

import com.tibco.sonar.plugins.bw6.check.AbstractProjectCheck;
import com.tibco.sonar.plugins.bw6.profile.BWProcessQualityProfile;
import com.tibco.sonar.plugins.bw6.source.ProjectSource;
import com.tibco.utils.bw6.model.Project;
import com.tibco.utils.common.logger.Logger;
import com.tibco.utils.common.logger.LoggerFactory;

import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Rule(
    key = SchemaFilenameValidationCheck.RULE_KEY,
    name = "Schema Filename Convention Validation",
    description = "Ensure .xsd files follow camelCase naming without special characters",
    priority = Priority.MINOR
)
@BelongsToProfile(title = BWProcessQualityProfile.PROFILE_NAME, priority = Priority.MINOR)
public class SchemaFilenameValidationCheck extends AbstractProjectCheck {

    public static final String RULE_KEY = "SchemaFilenameValidation";

    private static final Logger LOG = LoggerFactory.getLogger(SchemaFilenameValidationCheck.class);

    @RuleProperty(
        key = "schemaFilenamePattern",
        description = "Regex pattern for valid .xsd filenames (camelCase, no special characters)",
        defaultValue = "^[a-z]+([A-Z][a-z0-9]+)*\\.xsd$",
        type = "TEXT"
    )
    protected String schemaFilenamePattern;

    @Override
    public void validate(ProjectSource source) {
        LOG.debug("Started rule: " + this.getClass());

        File moduleDir = source.getProject().getFile();
        File schemasDir = new File(moduleDir, "Schemas");

        if (!schemasDir.exists() || !schemasDir.isDirectory()) {
            reportIssueOnFile("Schemas folder not found in module: " + moduleDir.getAbsolutePath());
            return;
        }

        List<File> xsdFiles = new ArrayList<>();
        collectXsdFiles(schemasDir, xsdFiles);


        for (File xsd : xsdFiles) {
            String fileName = xsd.getName();
            if (!fileName.matches(schemaFilenamePattern)) {
                reportIssueOnFile("Invalid XSD filename: '" + fileName + "' in path: '" +
                    xsd.getAbsolutePath() + "'. It must match the pattern: " + schemaFilenamePattern);
            }
        }
    }

    private void collectXsdFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectXsdFiles(file, result);
            } else if (file.getName().endsWith(".xsd")) {
                result.add(file);
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
