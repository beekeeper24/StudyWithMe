package com.studywithme.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successResponseContainsDataAndDefaultMessage() {
        ApiResponse<TestPayload> response = ApiResponse.success(new TestPayload("study"));

        assertThat(response.success()).isTrue();
        assertThat(response.data().name()).isEqualTo("study");
        assertThat(response.message()).isEqualTo("요청이 성공했습니다.");
    }

    private record TestPayload(String name) {
    }
}
