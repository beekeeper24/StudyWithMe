package com.studywithme.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studywithme.global.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerTest.TestController.class
})
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc;

    @Autowired
    GlobalExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void businessExceptionReturnsDomainErrorCode() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STUDY-001"))
                .andExpect(jsonPath("$.error.message").value("스터디를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.path").value("/test/business-error"));
    }

    @Test
    void validationExceptionReturnsInvalidInputErrorCodeWithFieldDetails() throws Exception {
        mockMvc.perform(post("/test/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL-400"))
                .andExpect(jsonPath("$.error.message").value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.error.detail[0].field").value("name"))
                .andExpect(jsonPath("$.path").value("/test/validation-error"));
    }

    @RestController
    public static class TestController {

        @GetMapping("/test/success")
        ApiResponse<TestResponse> success() {
            return ApiResponse.success(new TestResponse("ok"));
        }

        @GetMapping("/test/business-error")
        void businessError() {
            throw new BusinessException(TestErrorCode.STUDY_NOT_FOUND);
        }

        @PostMapping("/test/validation-error")
        void validationError(@Valid @RequestBody TestRequest request) {
        }
    }

    private record TestResponse(String value) {
    }

    private record TestRequest(@NotBlank String name) {
    }

    private enum TestErrorCode implements ErrorCode {
        STUDY_NOT_FOUND("STUDY-001", "스터디를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

        private final String code;
        private final String message;
        private final HttpStatus status;

        TestErrorCode(String code, String message, HttpStatus status) {
            this.code = code;
            this.message = message;
            this.status = status;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }
    }
}
