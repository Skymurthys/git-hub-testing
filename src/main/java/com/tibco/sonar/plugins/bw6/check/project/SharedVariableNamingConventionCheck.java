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
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Rule(
    key = SharedVariableNamingConventionCheck.RULE_KEY,
    name = "Shared variable Naming Validation in msv, jsv",
    description = "Ensures variable names in .msv and .jsv follow naming convention",
    priority = Priority.MINOR
)
@BelongsToProfile(title = BWProcessQualityProfile.PROFILE_NAME, priority = Priority.MINOR)
public class SharedVariableNamingConventionCheck extends AbstractProjectCheck {

    public static final String RULE_KEY = "SharedVariableNameValidation";

    private static final Logger LOG = LoggerFactory.getLogger(SharedVariableNamingConventionCheck.class);

    @RuleProperty(
        key = "sharedVariableNamePattern",
        description = "Regular expression for shared variable naming convention",
        defaultValue = "^[a-z]+([A-Z][a-z0-9]+)*$",
        type = "TEXT"
    )
    protected String sharedVariableNamePattern;

    @Override
    public void validate(ProjectSource source) {
        LOG.debug("Started rule: " + this.getClass());

        File moduleDir = source.getProject().getFile();
        File metaInfDir = new File(moduleDir, "META-INF");

        if (!metaInfDir.exists() || !metaInfDir.isDirectory()) return;

        File[] files = metaInfDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();

            // ⬇⬇ DEFAULT.SUBSTVAR REMOVED COMPLETELY ⬇⬇

            if (name.endsWith(".msv")) {
                validateXml(file, "module.msv", "moduleSharedVariable");
            } else if (name.endsWith(".jsv")) {
                validateXml(file, "module.jsv", "jobSharedVariable");
            }
        }
    }

    private void validateXml(File file, String contextLabel, String tagOrAttr) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(false);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new FileInputStream(file));
            doc.getDocumentElement().normalize();

            List<String> invalidVars = new ArrayList<>();

            // Only JSV and MSV logic remains
            if (tagOrAttr != null) {
                NodeList nodes = doc.getElementsByTagName(tagOrAttr);
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element elem = (Element) nodes.item(i);
                    if (elem.hasAttribute("name")) {
                        String name = elem.getAttribute("name").trim();
                        if (!name.matches(sharedVariableNamePattern)) {
                            invalidVars.add(name);
                        }
                    }
                }
            }

            if (!invalidVars.isEmpty()) {
                for (String var : invalidVars) {
                    reportIssueOnFile(
                        "Invalid shared variable name '" + var + "' in " + file.getName()
                            + ". Must follow the camelCase pattern [" + sharedVariableNamePattern + "]"
                    );
                }
            }
        } catch (Exception e) {
            reportIssueOnFile("Error parsing " + file.getName() + ": " + e.getMessage());
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
