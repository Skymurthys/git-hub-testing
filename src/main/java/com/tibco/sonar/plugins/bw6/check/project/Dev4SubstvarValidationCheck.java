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
import java.util.*;

@Rule(
        key = DEV4SubstvarValidationCheck.RULE_KEY,
        name = "Validate DEV4.substvar Variable Values with predefined_DEV4.substvar",
        description = "Compares values of matching global variables between DEV4.substvar and predefined_DEV4.substvar",
        priority = Priority.CRITICAL
)
@BelongsToProfile(title = BWProcessQualityProfile.PROFILE_NAME, priority = Priority.CRITICAL)
public class DEV4SubstvarValidationCheck extends AbstractProjectCheck {

    public static final String RULE_KEY = "DEV4SubstvarValidation";
    private static final Logger LOG = LoggerFactory.getLogger(DEV4SubstvarValidationCheck.class);

    @RuleProperty(
            key = "predefinedSubstvarPath",
            description = "Path to predefined_DEV4.substvar file",
            defaultValue = "C:/Workspace_BW6/dev_branch/copernico_sonar/predefined_DEV4.substvar",
            type = "TEXT"
    )
    protected String predefinedSubstvarPath;

	@Override
	public void validate(ProjectSource source) {
		File currentDir = source.getProject().getFile(); 
		File parentDir = currentDir.getParentFile();    

		// Search all sibling directories of currentDir, excluding .module/.parent
		File[] siblings = parentDir.listFiles(File::isDirectory);
		if (siblings == null) return;

		for (File sibling : siblings) {
			String name = sibling.getName().toLowerCase();
			if (name.endsWith(".module") || name.endsWith(".parent")) continue;

			File metaInfDir = new File(sibling, "META-INF");
			File DEV4File = findFile(metaInfDir, "DEV4.substvar");

			if (DEV4File != null) {
				validateAgainstPredefined(DEV4File);
				return;
			}
		}
		
		reportIssueOnFile("Missing DEV4.substvar in application folder");
	}

	private void validateAgainstPredefined(File DEV4File) {
		File predefinedFile = new File(predefinedSubstvarPath);
		if (!predefinedFile.exists() || !predefinedFile.isFile() || !predefinedFile.canRead()) {
			reportIssueOnFile("Invalid predefined_DEV4.substvar file: " + predefinedSubstvarPath);
			return;
		}

		Map<String, String> DEV4Vars = parseGlobalVariables(DEV4File);
		Map<String, String> predefinedVars = parseGlobalVariables(predefinedFile);

		for (Map.Entry<String, String> entry : predefinedVars.entrySet()) {
			String varName = entry.getKey();
			String expectedValue = entry.getValue();

			if (DEV4Vars.containsKey(varName)) {
				String actualValue = DEV4Vars.get(varName);
				if (!Objects.equals(expectedValue, actualValue)) {
					reportIssueOnFile("Variable '" + varName + "' mismatch. Expected: '" + expectedValue + "', Found: '" + actualValue + "' in DEV4.substvar");
				}
			}
		}
	}


    private Map<String, String> parseGlobalVariables(File file) {
        Map<String, String> vars = new HashMap<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(false);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new FileInputStream(file));
            doc.getDocumentElement().normalize();

            NodeList variableNodes = doc.getElementsByTagName("globalVariable");

            for (int i = 0; i < variableNodes.getLength(); i++) {
                Node node = variableNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    String name = getTagValue("name", elem);
                    String value = getTagValue("value", elem);
                    if (name != null) {
                        vars.put(name.trim(), value != null ? value.trim() : "");
                    }
                }
            }
        } catch (Exception e) {
            reportIssueOnFile("Error parsing: " + file.getName() + " - " + e.getMessage());
        }
        return vars;
    }

    private String getTagValue(String tag, Element element) {
        NodeList list = element.getElementsByTagName(tag);
        if (list != null && list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }

    private File findFile(File dir, String fileName) {
        if (dir == null || !dir.exists()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.getName().equals(fileName)) {
                return file;
            }
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
