package com.smartclearance.reviewbomb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ReviewBombServiceTest {

    @Test
    void internal_state_exists() throws Exception {
        ReviewBombRepository repository = mock(ReviewBombRepository.class);
        ReviewBombService service = new ReviewBombService(repository);

        Field field = ReviewBombService.class.getDeclaredField("recentPasswords");
        field.setAccessible(true);

        assertThat(field.get(service).toString()).isEqualTo("[]");
    }
}
