package jenkins.zafira.parsers;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.qaprosoft.zafira.client.ZafiraClient;
import com.qaprosoft.zafira.client.model.JobType;
import com.qaprosoft.zafira.client.model.TestCaseType;
import com.qaprosoft.zafira.client.model.TestRunType;
import com.qaprosoft.zafira.client.model.TestRunType.Initiator;
import com.qaprosoft.zafira.client.model.TestSuiteType;
import com.qaprosoft.zafira.client.model.TestType;
import com.qaprosoft.zafira.client.model.UserType;


public class TestSuiteParser extends DefaultHandler
{	
	private static final String TEST_RESULTS = "testResults";
	private static final String HTTP_SAMPLE = "httpSample";
	private static final String ASSERTION_RESULT = "assertionResult";
	private static final String NAME = "name";
	private static final String FAILURE = "failure";
	private static final String ERROR = "error";
	private static final String FAILURE_MESSAGE = "failureMessage";
	private String CURRENT_TAG = new String(); 
	
	private TestSuiteType zafiraTestSuite;
	private TestCaseType zafiraTestCase;
	private TestType zafiraTest;	
	private UserType zafiraUser;
	private JobType zafiraJob;
	private TestRunType zafiraTestRun;
	private ZafiraClient zafira;
	private String pathToReport;
	private hudson.model.AbstractBuild<?,?> build;
	
	@SuppressWarnings("deprecation")
	public TestSuiteParser(String zafiraUrl, UserType user, String pathToReport, hudson.model.AbstractBuild<?,?> arg0) 
	{
		this.zafira = new ZafiraClient(zafiraUrl);
		this.pathToReport = pathToReport;
		this.build = arg0;
		if(!this.zafira.isAvailable())
		{
			throw new RuntimeException("Zafira is unavailable!!!");
		}
		this.zafiraUser = this.zafira.createUser(user).getObject();
		URI uri;
		try {
			uri = new URI(arg0.getAbsoluteUrl());
			String url = StringUtils.substringBeforeLast(arg0.getAbsoluteUrl(), "/");
			String[] items = url.split("/");
			this.zafiraJob = this.zafira.createJob(new JobType(items[items.length - 2], StringUtils.substringBeforeLast(url, "/"), 
					uri.getHost(), zafiraUser.getId())).getObject();
		} catch (URISyntaxException e) 
		{
			throw new RuntimeException("Wrong Jenkins host!!!" + e);
		}
		
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException 
	{
		 switch(qName)
         {
         	case TEST_RESULTS: createAndSaveTestSuite(attributes); break;
         	case HTTP_SAMPLE: createAndSaveTestCase(attributes, qName); break;
         	case ASSERTION_RESULT: createTest(attributes); break;
         	case NAME: CURRENT_TAG = NAME; break;
         	case FAILURE: CURRENT_TAG = FAILURE; break;
         	case ERROR: CURRENT_TAG = ERROR; break;
         	case FAILURE_MESSAGE: CURRENT_TAG = FAILURE_MESSAGE; break;
         	default: break;         		
         } 
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException 
	{
		 switch(qName)
         {
	     	case TEST_RESULTS:
	     		this.zafira.finishTestRun(this.zafiraTestRun.getId());
	     		zafiraTestSuite = null;
	     		zafiraUser = null;
	     		zafiraTestRun = null;
	     		break;
	     	case HTTP_SAMPLE: 
	     		zafiraTestCase = null;
	     		zafiraTest = null;
	     		break;
	     	case ASSERTION_RESULT: zafiraTest = zafira.createTest(zafiraTest).getObject(); break;
	     	case NAME: CURRENT_TAG = new String(); break;
         	case FAILURE: CURRENT_TAG = new String(); break;
         	case ERROR: CURRENT_TAG = new String(); break;
         	case FAILURE_MESSAGE: CURRENT_TAG = new String(); break;
			default: break;
         }
	}
	
	@Override
	public void characters(char ch[], int start, int length) throws SAXException 
	{
		String text = new String(ch, start, length);
		switch(CURRENT_TAG)
		{
			case NAME: zafiraTest.setName(text); CURRENT_TAG = new String(); break;
	     	case FAILURE: 
	     		if("true".equals(text))
	     		{
	     			zafiraTest.setStatus(TestType.Status.FAILED);
	     		} else {
	     			 zafiraTest.setMessage("OK");
	     		}
	     		CURRENT_TAG = new String(); break;
	     	case ERROR: if("true".equals(text)) CURRENT_TAG = new String(); new UnsupportedOperationException("Please contact support team to handle this case!"); break;
	     	case FAILURE_MESSAGE: zafiraTest.setMessage(text); CURRENT_TAG = new String();break;
	     	default: new RuntimeException("Fail to support tag: " + CURRENT_TAG);
		}
	}
	
	private void createAndSaveTestSuite(Attributes attributes)
	{
		zafiraTestSuite = new TestSuiteType(StringUtils.substringBefore(pathToReport, "."), pathToReport, "Jmeter tests.", zafiraUser.getId());
		zafiraTestSuite = zafira.createTestSuite(zafiraTestSuite).getObject();
		
		zafiraTestRun = new TestRunType(zafiraTestSuite.getId(), zafiraUser.getId(), build.getBuildVariables().get("GIT_URL"), 
				build.getBuildVariables().get("GIT_BRANCH"), build.getBuildVariables().get("GIT_COMMIT"), "", zafiraJob.getId(), build.getNumber(), Initiator.HUMAN , "");
		zafiraTestRun = zafira.createTestRun(zafiraTestRun).getObject();
	}
	
	private void createAndSaveTestCase(Attributes attributes, String methodName)
	{
		zafiraTestCase = new TestCaseType(methodName, attributes.getValue("lb"), attributes.getValue("tn"),
				zafiraTestSuite.getId(), zafiraUser.getId());
		zafiraTestCase = zafira.createTestCase(zafiraTestCase).getObject();
		
		zafiraTest = new TestType();
		zafiraTest.setName(attributes.getValue("lb"));
		if("200".equals(attributes.getValue("rc")))
			zafiraTest.setStatus(TestType.Status.PASSED);
		else
			zafiraTest.setStatus(TestType.Status.FAILED);
		zafiraTest.setTestCaseId(zafiraTestCase.getId());
		zafiraTest.setTestRunId(zafiraTestRun.getId());
		zafiraTest.setLogURL(this.build.getAbsoluteUrl() + "console");
		zafiraTest.setMessage(attributes.getValue("rm"));
		zafiraTest = zafira.createTest(zafiraTest).getObject();
	}
	
	private void createTest(Attributes attributes)
	{
		zafiraTest = new TestType();
		zafiraTest.setName(attributes.getValue("name"));
		zafiraTest.setStatus(TestType.Status.PASSED);
		zafiraTest.setTestCaseId(zafiraTestCase.getId());
		zafiraTest.setTestRunId(zafiraTestRun.getId());
		zafiraTest.setLogURL(this.build.getAbsoluteUrl() + "console");
	}
}
