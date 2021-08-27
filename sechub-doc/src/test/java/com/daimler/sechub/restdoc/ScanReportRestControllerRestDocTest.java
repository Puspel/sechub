// SPDX-License-Identifier: MIT
package com.daimler.sechub.restdoc;
import static com.daimler.sechub.test.TestURLBuilder.https;
import static com.daimler.sechub.test.TestURLBuilder.RestDocPathParameter.JOB_UUID;
import static com.daimler.sechub.test.TestURLBuilder.RestDocPathParameter.PROJECT_ID;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.daimler.sechub.commons.model.TrafficLight;
import com.daimler.sechub.docgen.util.RestDocFactory;
import com.daimler.sechub.domain.scan.HTMLScanResultReportModelBuilder;
import com.daimler.sechub.domain.scan.report.DownloadScanReportService;
import com.daimler.sechub.domain.scan.report.ScanReport;
import com.daimler.sechub.domain.scan.report.ScanReportRestController;
import com.daimler.sechub.domain.scan.report.ScanReportResult;
import com.daimler.sechub.sharedkernel.usecases.UseCaseRestDoc;
import com.daimler.sechub.sharedkernel.usecases.user.execute.UseCaseUserDownloadsJobReport;
import com.daimler.sechub.test.ExampleConstants;
import com.daimler.sechub.test.TestPortProvider;
import com.epages.restdocs.apispec.ResourceSnippetParameters;

@RunWith(SpringRunner.class)
@WebMvcTest(ScanReportRestController.class)
@ContextConfiguration(classes= {ScanReportRestController.class, ScanReportRestControllerRestDocTest.SimpleTestConfiguration.class})
@AutoConfigureRestDocs(uriScheme="https",uriHost=ExampleConstants.URI_SECHUB_SERVER,uriPort=443)
public class ScanReportRestControllerRestDocTest {

	private static final String PROJECT1_ID = "project1";

	private static final int PORT_USED = TestPortProvider.DEFAULT_INSTANCE.getRestDocTestPort();

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private DownloadScanReportService downloadReportService;

	@MockBean
	HTMLScanResultReportModelBuilder modelBuilder;

	private UUID jobUUID;

	@UseCaseRestDoc(useCase=UseCaseUserDownloadsJobReport.class,variant="JSON")
	@Test
	@WithMockUser
	public void get_report_from_existing_job_returns_information_as_json_when_type_is_APPLICATION_JSON_UTF8() throws Exception {
		/* prepare */
        String apiEndpoint = https(PORT_USED).buildGetJobReportUrl(PROJECT_ID.pathElement(), JOB_UUID.pathElement());
        Class<? extends Annotation> useCase = UseCaseUserDownloadsJobReport.class;
        
		ScanReport report = new ScanReport(jobUUID,PROJECT1_ID);
		report.setResult("{'count':'1'}");
		report.setTrafficLight(TrafficLight.YELLOW);

		ScanReportResult result1 = new ScanReportResult(report);
		when(downloadReportService.getScanReportResult(PROJECT1_ID, jobUUID)).thenReturn(result1);

		/* execute + test @formatter:off */
	    this.mockMvc.perform(
	    		get(apiEndpoint,PROJECT1_ID,jobUUID).
	    		    accept(MediaType.APPLICATION_JSON_VALUE).
	    			contentType(MediaType.APPLICATION_JSON_VALUE)
	    		).
	    			andExpect(status().isOk()).
	    			andExpect(content().json("{\"jobUUID\":\""+jobUUID.toString()+"\",\"result\":{\"count\":1,\"findings\":[]},\"trafficLight\":\"YELLOW\"}")).

	    			andDo(document(RestDocFactory.createPath(useCase, "JSON"),
                            resource(
                                    ResourceSnippetParameters.builder().
                                        summary(RestDocFactory.createSummary(useCase)).
                                        description(RestDocFactory.createDescription(useCase)).
                                        tag(RestDocFactory.extractTag(apiEndpoint)).
                                        responseSchema(OpenApiSchema.SECHUB_REPORT.getSchema()).
                                        pathParameters(
                                                parameterWithName(PROJECT_ID.paramName()).description("The project Id"),
                                                parameterWithName(JOB_UUID.paramName()).description("The job UUID")
                                        ).
                                        build()
                                    )
	    			     ));

	    /* @formatter:on */
	}

	@UseCaseRestDoc(useCase=UseCaseUserDownloadsJobReport.class,variant="HTML")
	@Test
	@WithMockUser
	public void get_report_from_existing_job_returns_information_as_html_when_type_is_APPLICATION_XHTML_XML() throws Exception {
		/* prepare */
        String apiEndpoint = https(PORT_USED).buildGetJobReportUrl(PROJECT_ID.pathElement(), JOB_UUID.pathElement());
        Class<? extends Annotation> useCase = UseCaseUserDownloadsJobReport.class;
        
		ScanReport report = new ScanReport(jobUUID,PROJECT1_ID);
		report.setResult("{'count':'1'}");
		report.setTrafficLight(TrafficLight.YELLOW);

		ScanReportResult result1 = new ScanReportResult(report);
		when(downloadReportService.getScanReportResult(PROJECT1_ID, jobUUID)).thenReturn(result1);

		/* execute + test @formatter:off */
        this.mockMvc.perform(
        		get(apiEndpoint,PROJECT1_ID,jobUUID).
        		    accept(MediaType.APPLICATION_XHTML_XML).
        			contentType(MediaType.APPLICATION_JSON_VALUE)
        		).  
        			andExpect(status().isOk()).
        			andExpect(content().contentType("text/html;charset=UTF-8")).
        			andExpect(content().encoding("UTF-8")).
        			andExpect(content().string(containsString(jobUUID.toString()))).
        			andExpect(content().string(containsString("theRedStyle"))).

        			andDo(document(RestDocFactory.createPath(useCase, "HTML"),
                            resource(
                                    ResourceSnippetParameters.builder().
                                        summary(RestDocFactory.createSummary(useCase)).
                                        description(RestDocFactory.createDescription(useCase)).
                                        tag(RestDocFactory.extractTag(apiEndpoint)).
                                        responseSchema(OpenApiSchema.SECHUB_REPORT.getSchema()).
                                        pathParameters(
                                                parameterWithName(PROJECT_ID.paramName()).description("The project Id"),
                                                parameterWithName(JOB_UUID.paramName()).description("The job UUID")
                                        ).
                                        build()
                                    )
        			      ));

        /* @formatter:on */
	}

	@TestConfiguration
	@EnableAutoConfiguration
	public static class SimpleTestConfiguration{

	}

	@Before
	public void before() throws Exception {
		jobUUID=UUID.randomUUID();
		Map<String,Object> map = new HashMap<>();
		map.put("jobuuid", jobUUID);
		map.put("styleRed", "theRedStyle");
		map.put("styleGreen", "display:none");
		map.put("styleYellow", "display:none");
		map.put("redList", new ArrayList<>());
		map.put("yellowList", new ArrayList<>());
		map.put("greenList", new ArrayList<>());
		map.put("isWebDesignMode", false);
		when(modelBuilder.build(any())).thenReturn(map);
	}

}
