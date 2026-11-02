package com.slopeguard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(roles = "CITIZEN")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/reports with valid payload creates report and returns 201 Created")
    void createReport_whenValid_shouldReturnCreated() throws Exception {
        String validJson = """
                {
                    "reporterName": "Pema Tshering",
                    "description": "Active debris flow observed across hillside trail after sustained cloudburst.",
                    "latitude": 27.3412,
                    "longitude": 88.6125,
                    "locationDescription": "North Sikkim Highway link road near upper monastery",
                    "incidentCategory": "DEBRIS_FLOW"
                }
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.incidentCategory", is("DEBRIS_FLOW")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                // Public response must NOT leak reporter identity
                .andExpect(jsonPath("$.reporterName").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/reports with invalid data returns 400 Bad Request with field errors")
    void createReport_whenInvalid_shouldReturn400() throws Exception {
        // Missing description, latitude out of bounds, missing category
        String invalidJson = """
                {
                    "reporterName": "Test",
                    "description": "",
                    "latitude": 150.0,
                    "longitude": 92.5,
                    "locationDescription": ""
                }
                """;

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors", notNullValue()))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("GET /api/reports returns public reports list without reporter names")
    void getAllReports_shouldReturnPublicView() throws Exception {
        mockMvc.perform(get("/api/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[0].description", notNullValue()))
                .andExpect(jsonPath("$[0].status", notNullValue()))
                // Verify reporter privacy
                .andExpect(jsonPath("$[0].reporterName").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/reports/{id} retrieves newly submitted report")
    void getReportById_afterSubmission_shouldRetrieveReport() throws Exception {
        String newReport = """
                {
                    "reporterName": "Citizen Observer",
                    "description": "Noticeable vertical ground fissure propagating along the residential hill terrace.",
                    "latitude": 23.7311,
                    "longitude": 92.7154,
                    "locationDescription": "Ramhlun North slope perimeter, Aizawl",
                    "incidentCategory": "SLOPE_CRACK"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newReport))
                .andExpect(status().isCreated())
                .andReturn();

        String locationHeader = result.getResponse().getHeader("Location");
        org.junit.jupiter.api.Assertions.assertNotNull(locationHeader);

        mockMvc.perform(get(locationHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentCategory", is("SLOPE_CRACK")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.locationDescription", containsString("Aizawl")));
    }

    @Test
    @DisplayName("GET /api/reports/{id} with unknown ID returns 404 Not Found")
    void getReportById_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/reports/99999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("99999 was not found")));
    }
}
