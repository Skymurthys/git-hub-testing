package com.tibco.sonar.plugins.bw6.check.project;

import com.tibco.sonar.plugins.bw6.check.AbstractProjectCheck;
import com.tibco.sonar.plugins.bw6.profile.BWProcessQualityProfile;
import com.tibco.sonar.plugins.bw6.source.ProjectSource;
import com.tibco.utils.common.logger.Logger;
import com.tibco.utils.common.logger.LoggerFactory;

import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

import java.io.File;
import java.util.*;

@Rule(
    key = ProcessesFolderStructureCheck.RULE_KEY,
    name = "Validate Processes Folder Structure",
    description = "Ensures Processes folder contains required subfolders and no .bwp files directly inside",
    priority = Priority.MAJOR
)
@BelongsToProfile(title = BWProcessQualityProfile.PROFILE_NAME, priority = Priority.MAJOR)
public class ProcessesFolderStructureCheck extends AbstractProjectCheck {

    public static final String RULE_KEY = "ProcessesFolderStructureCheck";
    private static final Logger LOG = LoggerFactory.getLogger(ProcessesFolderStructureCheck.class);

    @RuleProperty(
        key = "expectedSubfolders",
        description = "List of mandatory subfolders under 'Processes'",
        defaultValue = "FrontEnd,BackEnd,Orchestration",
        type = "TEXT"
    )
    protected String expectedSubfolders;

    @Override
    public void validate(ProjectSource source) {
        File moduleDir = source.getProject().getFile();
        File processesDir = new File(moduleDir, "Processes");

        Set<String> requiredFolders = new HashSet<>();
        for (String folder : expectedSubfolders.split(",")) {
            requiredFolders.add(folder.trim());
        }

        Set<String> foundFolders = new HashSet<>();
        File[] entries = processesDir.listFiles();
        if (entries == null) return;

        for (File entry : entries) {
            if (entry.isDirectory()) {
                foundFolders.add(entry.getName());
            } else if (entry.isFile() && entry.getName().endsWith(".bwp")) {
                reportIssueOnFile(".bwp file '" + entry.getName() + "' should not be directly under 'Processes' folder: " + processesDir.getAbsolutePath());
            }
        }

        for (String required : requiredFolders) {
            if (!foundFolders.contains(required)) {
                reportIssueOnFile("Missing required folder '" + required + "' under: " + processesDir.getAbsolutePath());
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
