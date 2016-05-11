package jenkins.zafira.jmeter;


import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.xml.sax.SAXException;

import com.qaprosoft.zafira.client.model.UserType;

import hudson.Extension;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.zafira.parsers.TestSuiteParser;
import net.sf.json.JSONObject;

public class JMeterZafiraBuilder extends Builder
{
	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
	private SAXParserFactory saxFactory = SAXParserFactory.newInstance();
    private String pathToReport;
    private String userName;
    private String email;
    private String firstName;
    private String lastName;
    
	@DataBoundConstructor
    public JMeterZafiraBuilder(String pathToReport, String email, String userName, String firstName, String lastName) 
    {
        this.pathToReport = pathToReport;
        this.email = email;
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
    }

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
//	@Override
//	public BuildStepDescriptor<Publisher> getDescriptor() 
//	{
//		return (DescriptorImpl)super.getDescriptor();
//	}
	
    public String getPathToReport() 
    {
        return pathToReport;
    }
    
    public void setPathToReport(String pathToReport) 
    {
        this.pathToReport = pathToReport;
    }

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
    public boolean perform(hudson.model.AbstractBuild<?,?> arg0, hudson.Launcher arg1, hudson.model.BuildListener arg2) throws InterruptedException, IOException 
    {
		try 
		{
			if(StringUtils.isBlank(userName))
			{
				userName = "anonymous";
				email = arg0.getBuildVariables().get("BUILD_USER_EMAIL");
				firstName = arg0.getBuildVariables().get("BUILD_USER_FIRST_NAME");
				lastName = arg0.getBuildVariables().get("BUILD_USER_LAST_NAME");
			}	
			UserType zafiraUser = new UserType(userName, email, firstName, lastName);
			SAXParser saxParser = saxFactory.newSAXParser();
			TestSuiteParser testSuiteParser = new TestSuiteParser(DESCRIPTOR.getZafiraUrl(), zafiraUser, arg0.getWorkspace() + getPathToReport(), arg0);
	    	saxParser.parse(new File(arg0.getWorkspace() + getPathToReport()), testSuiteParser);
		} 
		catch (ParserConfigurationException | SAXException e) 
		{
			arg1.getListener().getLogger().println("Zafira integration failed!!!" + e);
		}
		return true;
    }
  
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> 
    {
        private String zafiraUrl;
    	
        public DescriptorImpl()
        {
    		load();
    	}
        
        public FormValidation doCheckName(@QueryParameter String globalPathToReport)
                throws IOException, ServletException {
            if (globalPathToReport.length() == 0)
                return FormValidation.error("Please set a path to report");
            if (globalPathToReport.length() < 4)
                return FormValidation.warning("Isn't the path to report too short?");
            return FormValidation.ok();
        }
        
        public String getDisplayName()
        {
            return "Publish Jmeter test result report to Zafira";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException 
        {
        	setZafiraUrl(json.getString("zafiraUrl"));
            save();
            return super.configure(req, json);
        }
       
		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class arg0) 
		{
			return true;
		}

		public String getZafiraUrl()
		{
			return zafiraUrl;
		}

		public void setZafiraUrl(String zafiraUrl)
		{
			this.zafiraUrl = zafiraUrl;
		}
    }
}

