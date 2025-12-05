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
    key = GlobalVariableNamingConventionCheck.RULE_KEY,
    name = "Global Variable Naming Validation in substvar",
    description = "Ensures global variable names in default.substvar follow naming rules",
    priority = Priority.MINOR
)
@BelongsToProfile(title = BWProcessQualityProfile.PROFILE_NAME, priority = Priority.MINOR)
public class GlobalVariableNamingConventionCheck extends AbstractProjectCheck {

    public static final String RULE_KEY = "GlobalVariableNameValidation";

    private static final Logger LOG = LoggerFactory.getLogger(GlobalVariableNamingConventionCheck.class);

    @RuleProperty(
        key = "globalVariableNamePattern",
        description = "Regular expression for global variable naming convention",
        defaultValue = "^[a-z]+([A-Z][a-z0-9]+)*$",   
        type = "TEXT"
    )
    protected String globalVariableNamePattern;

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
            if (name.equalsIgnoreCase("default.substvar")) {
                validateXml(file, "default.substvar");
            }
        }
    }

    private void validateXml(File file, String contextLabel) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(false);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new FileInputStream(file));
            doc.getDocumentElement().normalize();

            List<String> invalidVars = new ArrayList<>();

            NodeList nodes = doc.getElementsByTagName("globalVariable");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element elem = (Element) nodes.item(i);
                String name = getTagValue("name", elem);

                if (name != null && !name.startsWith("BW.")) {

                    // Extract last part of path
                    String baseName = name.replaceAll("[/\\\\]+", "/");
                    String lastPart = baseName.substring(baseName.lastIndexOf("/") + 1);

                    if (!lastPart.matches(globalVariableNamePattern)) {
                        invalidVars.add(name);
                    }
                }
            }

            if (!invalidVars.isEmpty()) {
                for (String var : invalidVars) {
                    reportIssueOnFile(
                        "Invalid global variable name '" + var + "' in " + file.getName()
                        + ". Must follow camelCase pattern [" + globalVariableNamePattern + "]"
                    );
                }
            }

        } catch (Exception e) {
            reportIssueOnFile("Error parsing " + file.getName() + ": " + e.getMessage());
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList list = element.getElementsByTagName(tag);
        if (list != null && list.getLength() > 0) {
            Node node = list.item(0);
            return node.getTextContent().trim();
        }
        return null;
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
